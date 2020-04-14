package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogBooleanBinding

class ConfigBooleanDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogBooleanBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_boolean
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun beforeExecutePendingBindings() {
        val item = arguments?.getParcelable(ARG_ITEM) as? BooleanConfigItem
        dataBinding.item = item
        dataBinding.switchConfig.setOnCheckedChangeListener { _, isChecked ->
            isSavable(item?.isSavable?.invoke(isChecked) ?: true)
        }
        dataBinding.switchConfig.isChecked = item?.value?.invoke() ?: false
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.switchConfig.isChecked
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigBooleanDialogFragment::javaClass.name}.ARG_ITEM"

        fun newInstance(item: BooleanConfigItem) = ConfigBooleanDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item
            )
        }
    }

}