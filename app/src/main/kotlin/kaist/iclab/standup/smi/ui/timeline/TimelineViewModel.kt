package kaist.iclab.standup.smi.ui.timeline

import android.util.Log
import androidx.lifecycle.*
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.DataField
import kaist.iclab.standup.smi.common.Status
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
import kotlin.math.abs

class TimelineViewModel(
    private val eventRepository: EventRepository,
    private val missionRepository: MissionRepository,
    private val statRepository: StatRepository,
    private val placeReference: () -> CollectionReference?
) : BaseViewModel<TimelineNavigator>() {
    private val ioContext = viewModelScope.coroutineContext + Dispatchers.IO

    val isDailyMode: MutableLiveData<Boolean> = MutableLiveData(true)

    /**
     * Data relevant to Daily-mode
     */
    val currentDateTime: MutableLiveData<DateTime> = MutableLiveData()

    val dailyStats: MutableLiveData<List<SedentaryMissionEvent>> = MutableLiveData()

    val dailyMissionsTriggered = dailyStats.map { data ->
        data?.sumBy { datum -> datum.missions.size } ?: 0
    }

    val dailyMissionsSuccess = dailyStats.map { data ->
        data?.sumBy { datum -> datum.missions.filter { it.isSucceeded() }.size } ?: 0
    }

    val dailyAvgSedentaryMillis = dailyStats.map { data ->
        data?.map { datum -> datum.event.duration }?.average()?.toLong() ?: 0
    }

    val dailyIncentiveObtained = dailyStats.map { data ->
        if (!LocalPrefs.isMissionOn) return@map null

        val isGainMode = BuildConfig.IS_GAIN_INCENTIVE
        val maxBudget = RemotePrefs.maxDailyBudget

        val incentive = data?.sumBy { datum -> datum.missions.sumIncentives() } ?: 0

        if (isGainMode) {
            incentive.coerceIn(0, maxBudget)
        } else {
            (maxBudget - abs(incentive)).coerceIn(0, RemotePrefs.maxDailyBudget)
        }
    }

    val dailyLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    fun loadDailyStat(dateTime: DateTime) = viewModelScope.launch(ioContext) {
        loadDailyStatInternal(dateTime)
    }

    fun refreshDailyStat() = viewModelScope.launch(ioContext) {
        currentDateTime.value?.let { loadDailyStatInternal(it) }
    }

    private suspend fun loadDailyStatInternal(dateTime: DateTime) {
        ui { navigator?.navigateBeforeDataLoad() }

        currentDateTime.postValue(dateTime)
        dailyLoadStatus.postValue(Status.loading())

        try {
            val (from, to) = dateTime.let { curTime ->
                val dayStart = curTime.withTimeAtStartOfDay().millis
                val start = dayStart + LocalPrefs.activeStartTimeMs
                val end = (dayStart + LocalPrefs.activeEndTimeMs)
                start to end
            }

            val events = eventRepository.getEvents(from, to)
            val missions = missionRepository.getTriggeredMissions(from, to)
            val places = events.distinctBy { it.geoHash }.mapNotNull {
                statRepository.getPlaceStat(it.latitude, it.longitude)
            }

            val missionEvents = events.toSedentaryMissionEvent(
                fromTime = from,
                toTime = to,
                missions = missions,
                places = places
            ).sortedByDescending {
                it.event.startTime
            }

            dailyStats.postValue(missionEvents)
            dailyLoadStatus.postValue(Status.success())
        } catch (e: Exception) {
            ui { navigator?.navigateDailyStatError(e) }
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
        onError = {
            ui { navigator?.navigatePlaceStatError(it) }
        },
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

    val placeStats = LivePagedListBuilder(
        placeDataSourceFactory, PagedList.Config.Builder()
            .setPageSize(30)
            .setEnablePlaceholders(true)
            .setMaxSize(100)
            .build()
    ).build()

    val totalVisitedPlaces = overallStats.map { it?.numPlaces ?: 0 }

    val totalIncentives = overallStats.map { stat ->
        if (!LocalPrefs.isMissionOn) {
            null
        } else {
            val nDaysMission = stat?.numDaysMissions ?: 0
            val maxIncentive = nDaysMission * RemotePrefs.maxDailyBudget
            val curIncentive = stat?.incentive ?: 0
            val isGainMode = BuildConfig.IS_GAIN_INCENTIVE
            if (isGainMode) {
                curIncentive.coerceIn(0, maxIncentive)
            } else {
                (maxIncentive - abs(curIncentive)).coerceIn(0, maxIncentive)
            }
        }
    }

    val placeLoadStatus = placeDataSourceFactory.sourceLiveData.switchMap { it.initStatus }

    fun loadPlaceStats(
        isDescending: Boolean = true,
        field: Int = TimelineFragment.EXTRA_FIELD_VISIT_TIME
    ) = viewModelScope.launch {
        ui { navigator?.navigateBeforeDataLoad() }

        currentOrderField = field
        currentOrderDescendingDirection = isDescending

        val query = placeReference.invoke()?.let {
            PlaceStat.buildQuery(
                ref = it,
                orderBy = extraToField(field),
                isAscending = !isDescending
            )
        }
        placeDataSourceFactory.updateQuery(query)
    }

    fun refreshPlaceStats() = viewModelScope.launch {
        ui { navigator?.navigateBeforeDataLoad() }

        placeDataSourceFactory.refresh()
    }

    fun retryToLoadPlaceStats() {
        placeDataSourceFactory.retry()
    }

    /**
     * Data relevant rename place
     */
    fun renamePlace(
        newName: String,
        latitude: Double,
        longitude: Double
    ) = viewModelScope.launch(ioContext) {
        try {
            if (newName.isBlank()) throwError(R.string.error_empty_input)
            statRepository.updatePlaceName(latitude, longitude, newName)

            if (isDailyMode.value != false) {
                refreshDailyStat()
            } else {
                refreshPlaceStats()
            }

        } catch (e: Exception) {
            ui { navigator?.navigatePlaceRenameError(e) }
        }
    }

    private fun extraToField(field: Int): DataField<*> = when (field) {
        TimelineFragment.EXTRA_FIELD_VISITS -> PlaceStats.numVisit
        TimelineFragment.EXTRA_FIELD_MISSIONS -> PlaceStats.numMission
        TimelineFragment.EXTRA_FIELD_INCENTIVE -> PlaceStats.incentive
        else -> PlaceStats.lastVisitTime
    }
}