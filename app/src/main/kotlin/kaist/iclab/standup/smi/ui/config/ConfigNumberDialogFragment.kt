package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogNumberBinding

class ConfigNumberDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogNumberBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_number
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun beforeExecutePendingBindings() {
        val item = arguments?.getParcelable(ARG_ITEM) as? NumberConfigItem
        dataBinding.item = item
        dataBinding.numberPickerConfig.minValue = item?.min?.toInt() ?: 0
        dataBinding.numberPickerConfig.maxValue = item?.max?.toInt() ?: 100
        dataBinding.numberPickerConfig.setOnValueChangedListener { _, _, newVal ->
            isSavable(item?.isSavable?.invoke(newVal.toLong()) ?: true)
        }
        dataBinding.numberPickerConfig.value = item?.value?.invoke()?.toInt() ?: 0
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.numberPickerConfig.value.toLong()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigNumberDialogFragment::javaClass.name}.ARG_ITEM"

        fun newInstance(item: NumberConfigItem) = ConfigNumberDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item
            )
        }
    }

}