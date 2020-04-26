package kaist.iclab.standup.smi.ui.place

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.firebase.firestore.CollectionReference
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.throwError
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.data.Missions
import kaist.iclab.standup.smi.data.PlaceStat
import kaist.iclab.standup.smi.repository.StatRepository
import kaist.iclab.standup.smi.view.DocumentDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaceViewModel(
    private val statRepository: StatRepository,
    private val missionReference: () -> CollectionReference?
) : BaseViewModel<PlaceNavigator>() {
    private val ioContext = viewModelScope.coroutineContext + Dispatchers.IO

    fun loadData(placeId: String) = viewModelScope.launch(ioContext) {
        placeLoadStatus.postValue(Status.loading())

        try {
            val place = statRepository.getPlaceStat(placeId) ?: throwError(0)
            placeStat.postValue(place)

            val query = missionReference.invoke()?.let {
                Mission.buildQuery(
                    ref = it,
                    orderBy = Missions.triggerTime,
                    isAscending = false
                ) {
                    Missions.geoHash equalTo placeId
                }
            }
            missionDataSourceFactory.updateQuery(query)
        } catch (e: Exception) {
            placeLoadStatus.postValue(Status.failure(e))
            navigator?.navigateError(e)
        }
    }

    val placeStat: MutableLiveData<PlaceStat> = MutableLiveData()
    val placeLoadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    private val missionDataSourceFactory = DocumentDataSource.Factory(
        scope = viewModelScope,
        dispatcher = ioContext,
        query = null,
        onError = { navigator?.navigateError(it) },
        entityClass = Mission
    )
    val missions = LivePagedListBuilder(missionDataSourceFactory, PagedList.Config.Builder()
        .setPageSize(30)
        .setEnablePlaceholders(true)
        .setMaxSize(100)
        .build()
    ).build()
    val missionLoadStatus = missionDataSourceFactory.sourceLiveData.switchMap { it.initStatus }
}