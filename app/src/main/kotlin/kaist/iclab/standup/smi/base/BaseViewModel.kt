package kaist.iclab.standup.smi.base

import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kaist.iclab.standup.smi.common.StandUpError
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.wrapError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

abstract class BaseViewModel<N : BaseNavigator>(navigator: N? = null) : ViewModel() {
    private val refNav: WeakReference<N?> = WeakReference(navigator)

    protected val nav: N? = refNav.get()

    val status: MutableLiveData<Status> = MutableLiveData(Status.init())

    fun launch(call: suspend () -> Unit) = viewModelScope.launch { call() }

    fun load(extras: Bundle? = null) = launch {
        status.postValue(Status.loading())
        try {
            io { onLoad(extras) }
            status.postValue(Status.loading())
        } catch (e: Exception) {
            val error = wrapError(e)
            ui { nav?.navigateError(error) }
            status.postValue(Status.failure(error))
        }
    }

    fun tryLaunch(call: suspend () -> Unit) = launch {
        try {
            call()
        } catch (e: Exception) {
            ui { nav?.navigateError(e) }
        }
    }

    suspend fun io(call: suspend () -> Unit) = withContext(Dispatchers.IO) { call() }

    suspend fun ui(call: suspend () -> Unit) = withContext(Dispatchers.Main) { call() }

    abstract suspend fun onLoad(extras: Bundle? = null)

    @CallSuper
    override fun onCleared() {
        refNav.clear()
        super.onCleared()
    }
}