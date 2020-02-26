package kaist.iclab.standup.smi.base

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
    @get:MenuRes
    protected abstract val menuRes: Int?

    abstract fun beforeExecutePendingBindings()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(javaClass, "onCreate()")

        dataBinding = DataBindingUtil.setContentView(this, layoutId)
        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()

        (viewModel as? BaseViewModel<*>)?.load(intent?.extras)

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        AppLog.d(javaClass, "onCreateOptionsMenu(menu: $menu)")
        menuRes?.let { menuInflater.inflate(it, menu) }
        return menuRes != null
    }
}