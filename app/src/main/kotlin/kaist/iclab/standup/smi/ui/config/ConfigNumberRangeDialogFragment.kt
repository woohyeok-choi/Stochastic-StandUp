package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogNumberRangeBinding

class ConfigNumberRangeDialogFragment :
    BaseBottomSheetDialogFragment<FragmentConfigDialogNumberRangeBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_number_range
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun beforeExecutePendingBindings() {
        val item = arguments?.getParcelable(ARG_ITEM) as? NumberRangeConfigItem
        dataBinding.item = item

        val min = item?.min?.toInt() ?: 0
        val max = item?.max?.toInt() ?: 0
        val (from, to) = item?.value?.invoke() ?: (0L to 0L)

        dataBinding.numberPickerConfigFrom.apply {
            minValue = min
            maxValue = max
            setOnValueChangedListener { _, _, newVal ->
                val newValue = newVal.toLong() to dataBinding.numberPickerConfigTo.value.toLong()
                isSavable(item?.isSavable?.invoke(newValue) ?: true)
            }
        }

        dataBinding.numberPickerConfigTo.apply {
            minValue = min
            maxValue = max
            setOnValueChangedListener { _, _, newVal ->
                val newValue = dataBinding.numberPickerConfigFrom.value.toLong() to newVal.toLong()
                isSavable(item?.isSavable?.invoke(newValue) ?: true)
            }
        }

        dataBinding.numberPickerConfigFrom.value = from.toInt()
        dataBinding.numberPickerConfigTo.value = to.toInt()
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.numberPickerConfigFrom.value.toLong() to dataBinding.numberPickerConfigTo.value.toLong()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigNumberRangeDialogFragment::javaClass.name}.ARG_ITEM"

        fun newInstance(item: NumberRangeConfigItem) = ConfigNumberRangeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item
            )
        }
    }

}