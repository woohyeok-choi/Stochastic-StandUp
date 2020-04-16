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
        val min = item?.min?.toInt() ?: 0
        val max = item?.max?.toInt() ?: 100
        val value = item?.value?.invoke()?.toInt() ?: 0
        val values = (min..max).map {
            item?.valueFormatter?.invoke(it.toLong()) ?: it.toString()
        }.toTypedArray()

        dataBinding.item = item
        dataBinding.numberPickerConfig.minValue = min
        dataBinding.numberPickerConfig.maxValue = max
        dataBinding.numberPickerConfig.setOnValueChangedListener { _, _, newVal ->
            isSavable(item?.isSavable?.invoke(newVal.toLong()) ?: true)
        }
        dataBinding.numberPickerConfig.displayedValues = values

        dataBinding.numberPickerConfig.value = value.coerceIn(min, max)
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