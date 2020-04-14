package kaist.iclab.standup.smi.base

import androidx.annotation.CallSuper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

abstract class BaseViewModel<N : BaseNavigator> : ViewModel() {
    private var refNav: WeakReference<N>? = null

    var navigator: N?
        get() = refNav?.get()
        set(value) {
            if (value != null)
                refNav = WeakReference(value)
        }

    suspend fun <R> io(call: suspend () -> R) : R = withContext(Dispatchers.IO) { call() }

    suspend fun <R> ui(call: suspend () -> R) : R = withContext(Dispatchers.Main) { call() }


    @CallSuper
    override fun onCleared() {
        refNav?.clear()
        super.onCleared()
    }
}