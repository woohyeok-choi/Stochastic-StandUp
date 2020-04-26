package kaist.iclab.standup.smi.base

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.DialogFragment
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.common.AppLog
import kaist.iclab.standup.smi.databinding.FragmentBaseDialogBinding
import java.io.Serializable

abstract class BaseDialogFragment<B : ViewDataBinding> : DialogFragment() {
    protected lateinit var dataBinding: B
    @get:LayoutRes
    protected abstract val layoutId: Int
    protected abstract val showPositiveButton: Boolean
    protected abstract val showNegativeButton: Boolean

    private lateinit var rootBinding: FragmentBaseDialogBinding

    @StringRes
    protected open val textPositiveButton: Int = android.R.string.ok
    @StringRes
    protected open val textNegativeButton: Int = android.R.string.cancel

    interface OnDismissListener : Serializable {
        fun onDismiss()
    }

    abstract fun beforeExecutePendingBindings()

    abstract fun onClick(isPositive: Boolean)

    protected fun isSavable(isEnabled: Boolean) {
        rootBinding.btnPositive.isEnabled = isEnabled
    }

    var onDismiss: OnDismissListener? = null

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
        rootBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_base_dialog, container, false
        )
        dataBinding = DataBindingUtil.inflate(inflater, layoutId, container, false)
        rootBinding.container.addView(dataBinding.root)

        return rootBinding.root
    }

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AppLog.d(javaClass, "onViewCreated()")

        rootBinding.showPositiveButton = showPositiveButton
        rootBinding.showNegativeButton = showNegativeButton
        rootBinding.textPositiveButton = getString(textPositiveButton)
        rootBinding.textNegativeButton = getString(textNegativeButton)
        rootBinding.lifecycleOwner = this

        rootBinding.btnNegative.setOnClickListener {
            onClick(false)
            dismiss()
        }
        rootBinding.btnPositive.setOnClickListener {
            onClick(true)
            dismiss()
        }

        dataBinding.lifecycleOwner = this

        beforeExecutePendingBindings()

        rootBinding.executePendingBindings()
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

    override fun onDismiss(dialog: DialogInterface) {
        onDismiss?.onDismiss()
    }
}