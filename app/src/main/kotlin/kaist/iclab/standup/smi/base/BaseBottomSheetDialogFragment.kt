package kaist.iclab.standup.smi.base

import android.content.Context
import android.os.Bundle
import android.view.*
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.MenuRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import kaist.iclab.standup.smi.common.AppLog

abstract class BaseDialogFragment<B : ViewDataBinding, VM: ViewModel> : DialogFragment() {
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        AppLog.d(javaClass, "onCreate()")
    }
    
    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.d(javaClass, "onCreate()")
    }

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        AppLog.d(javaClass, "onCreateView()")
        dataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        return dataBinding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLog.d(javaClass, "onViewCreated()")

        dataBinding.setVariable(viewModelVariable, viewModel)
        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()

        dataBinding.executePendingBindings()
    }

    @CallSuper
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        AppLog.d(javaClass, "onActivityCreated()")
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
    override fun onDestroyView() {
        super.onDestroyView()
        AppLog.d(javaClass, "onDestroyView()")
    }

    @CallSuper
    override fun onDestroy() {
        super.onDestroy()
        AppLog.d(javaClass, "onDestroy()")
    }

    @CallSuper
    override fun onDetach() {
        super.onDetach()
        AppLog.d(javaClass, "onDetach()")
    }

}