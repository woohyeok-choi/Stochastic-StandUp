package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogBooleanBinding

class ConfigBooleanDialogFragment :
    BaseBottomSheetDialogFragment<FragmentConfigDialogBooleanBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_boolean
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<BooleanConfigItem>(ARG_ITEM) ?: return

        dataBinding.item = item
        dataBinding.switchConfig.setOnCheckedChangeListener { _, isChecked ->
            onValueChanged(item, isChecked)
        }

        val value = item.value?.invoke() ?: false
        dataBinding.switchConfig.isChecked = value
        onValueChanged(item, value)
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.switchConfig.isChecked
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    private fun onValueChanged(item: BooleanConfigItem, isChecked: Boolean) {
        dataBinding.switchConfig.text = if(item.valueFormatter == null) {
            item.formatter?.invoke(isChecked)
        } else {
            item.valueFormatter?.invoke(isChecked)
        } ?: getString(if (isChecked) R.string.general_switch_on else R.string.general_switch_off)

        isSavable(item.isSavable?.invoke(isChecked) ?: true)
    }

    companion object {
        private val ARG_ITEM = "${ConfigBooleanDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigBooleanDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: BooleanConfigItem, onDismiss: (() -> Unit)? = null) = ConfigBooleanDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}