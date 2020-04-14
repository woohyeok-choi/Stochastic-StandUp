package kaist.iclab.standup.smi.base

import android.app.Application
import android.os.Bundle
import android.view.Menu
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import kaist.iclab.standup.smi.common.AppLog

abstract class BaseActivity<B : ViewDataBinding, VM : ViewModel> : AppCompatActivity() {
    protected abstract val viewModel: VM
    protected abstract val viewModelVariable: Int
    protected lateinit var dataBinding: B
    @get:LayoutRes
    protected abstract val layoutId: Int


    protected fun d(msg: String) {
        AppLog.d(javaClass, msg)
    }

    protected fun e(msg: String, throwable: Throwable? = null) {
        AppLog.e(javaClass, msg, throwable)
    }

    abstract fun beforeExecutePendingBindings()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(javaClass, "onCreate()")

        dataBinding = DataBindingUtil.setContentView(this, layoutId)
        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()

        dataBinding.executePendingBindings()
    }

    @CallSuper
    override fun onStart() {
        super.onStart()
        AppLog.d(javaClass, "onStart()")
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        AppLog.d(javaClass, "onResume()")
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        AppLog.d(javaClass, "onPause()")
    }

    @CallSuper
    override fun onStop() {
        super.onStop()
        AppLog.d(javaClass, "onStop()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(javaClass, "onDestroy()")
    }
}