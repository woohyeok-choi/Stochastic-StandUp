package kaist.iclab.standup.smi.ui.timeline

import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.Query
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.sumByLong
import kaist.iclab.standup.smi.common.throwError
import kaist.iclab.standup.smi.data.PlaceStat
import kaist.iclab.standup.smi.data.PlaceStats
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.repository.*
import kaist.iclab.standup.smi.view.DocumentDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class TimelineViewModel (
    private val eventRepository: EventRepository,
    private val missionRepository: MissionRepository,
    private val statRepository: StatRepository,
    private val placeReference: () -> CollectionReference?
): BaseViewModel<TimelineNavigator>() {
    private val ioContext = viewModelScope.coroutineContext + Dispatchers.IO

    /**
     * Data relevant to Daily-mode
     */
    val currentDateTime: MutableLiveData<DateTime> = MutableLiveData()

    val dailyStats: MutableLiveData<List<SedentaryMissionEvent>> = MutableLiveData()

    val dailyTotalSedentaryMinutes = dailyStats.map { data ->
        data?.sumByLong { it.duration }?.let { TimeUnit.MILLISECONDS.toMinutes(it) } ?: 0
    }

    val dailyAvgSedentaryMinutes = dailyStats.map { data ->
        data?.map { it.duration }?.average()?.toLong()?.let { TimeUnit.MILLISECONDS.toMinutes(it)} ?: 0
    }

    val dailyNumProlongedSedentariness = dailyStats.map { data ->
        data?.filter { it.duration >= RemotePrefs.maxProlongedSedentaryTime }?.size ?: 0
    }

    val dailyTotalIncentives = dailyStats.map { data ->
        val incentive = data?.sumBy { it.incentives } ?: 0
        if (incentive >= 0) {
            incentive.coerceAtMost(RemotePrefs.maxDailyBudget)
        } else {
            (RemotePrefs.maxDailyBudget - abs(incentive)).coerceAtLeast(0)
        }
    }

    val dailyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    fun loadDailyStat(dateTime: DateTime) = viewModelScope.launch(ioContext) {
        loadDailyStat(dateTime, false)
    }

    fun refreshDailyStat() = viewModelScope.launch(ioContext) {
        currentDateTime.value?.let { loadDailyStat(it, true) }
    }

    private suspend fun loadDailyStat(dateTime: DateTime, isForced: Boolean) {
        if (!isForced && currentDateTime.value == dateTime) return

        currentDateTime.postValue(dateTime)
        dailyLoadStatus.postValue(Status.loading())

        try {
            val currentTime = System.currentTimeMillis()
            val (from, to) = dateTime.let { curTime ->
                val dayStart = curTime.withTimeAtStartOfDay().millis
                val start = dayStart + LocalPrefs.activeStartTimeMs
                val end = (dayStart + LocalPrefs.activeEndTimeMs).coerceAtMost(currentTime)
                start to end
            }

            val events = eventRepository.getEvents(from, to)
            val missions = missionRepository.getCompletedMissions(from, to)
            val places = events.distinctBy { it.geoHash }.mapNotNull {
                statRepository.getPlaceStat(it.latitude, it.longitude)
            }

            val missionEvents = events.toSedentaryMissionEvent(
                fromTime = from,
                toTime = to,
                missions = missions,
                places = places,
                prolongedSedentaryTimeMillis = RemotePrefs.maxProlongedSedentaryTime
            ).sortedByDescending { it.startTime ?: 0 }

            dailyStats.postValue(missionEvents)
            dailyLoadStatus.postValue(Status.success())
        } catch (e: Exception) {
            navigator?.navigateDailyStatError(e)
            dailyLoadStatus.postValue(Status.failure(e))
        }
    }

    /**
     * Data relevant to Place-mode
     */
    private val placeDataSourceFactory = DocumentDataSource.Factory(
        scope = viewModelScope,
        dispatcher = ioContext,
        query = null,
        onError = { navigator?.navigatePlaceStatError(it) },
        entityClass = PlaceStat
    )

    private val overallStats = liveData(ioContext) {
        try {
            emit(statRepository.getOverallStat())
        } catch (e: Exception) {
            emit(null)
        }
    }

    var currentOrderField = TimelineFragment.EXTRA_FIELD_VISIT_TIME
        private set

    var currentOrderDescendingDirection = true
        private set

    val placeStats= LivePagedListBuilder(placeDataSourceFactory, PagedList.Config.Builder()
        .setPageSize(30)
        .setEnablePlaceholders(true)
        .setMaxSize(100)
        .build()
    ).build()

    val totalVisitedPlaces = overallStats.map { it?.numPlaces ?: 0 }

    val totalIncentives = overallStats.map { it?.incentive ?: 0 }

    val placeLoadStatus = placeDataSourceFactory.sourceLiveData.switchMap { it.initStatus }

    fun loadPlaceStats(isDescending: Boolean = true, field: Int = TimelineFragment.EXTRA_FIELD_VISIT_TIME) {
        currentOrderField = field
        currentOrderDescendingDirection = isDescending

        val query = placeReference.invoke()?.orderBy(fieldToString(field), if (isDescending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
        placeDataSourceFactory.updateQuery(query)
    }

    fun refreshPlaceStats() {
        placeDataSourceFactory.refresh()
    }

    fun retryToLoadPlaceStats() {
        placeDataSourceFactory.retry()
    }

    /**
     * Data relevant rename place
     */
    fun renamePlace(newName: String,
                    latitude: Double,
                    longitude: Double
    ) = viewModelScope.launch(ioContext) {
        try {
            if (newName.isBlank()) throwError(R.string.error_empty_input)
            statRepository.updatePlaceName(latitude, longitude, newName)
        } catch (e: Exception) {
            navigator?.navigatePlaceRenameError(e)
        }
    }

    private fun fieldToString(field: Int) : String = when(field) {
        TimelineFragment.EXTRA_FIELD_VISITS -> PlaceStats.numVisit.name
        TimelineFragment.EXTRA_FIELD_MISSIONS -> PlaceStats.numMission.name
        TimelineFragment.EXTRA_FIELD_INCENTIVE -> PlaceStats.incentive.name
        TimelineFragment.EXTRA_FIELD_DURATION -> PlaceStats.approximateDuration.name
        else -> PlaceStats.lastVisitTime.name
    }
}