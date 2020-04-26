package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogNumberRangeBinding

class ConfigNumberRangeDialogFragment :
    BaseBottomSheetDialogFragment<FragmentConfigDialogNumberRangeBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_number_range
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<NumberRangeConfigItem>(ARG_ITEM) ?: return

        val min = item.min.toInt()
        val max = item.max.toInt()
        val (from, to) = item.value?.invoke() ?: (0L to 0L)
        val values = (min..max).map {
            item.valueFormatter?.invoke(it.toLong()) ?: it.toString()
        }.toTypedArray()

        dataBinding.item = item
        dataBinding.numberPickerConfigFrom.apply {
            minValue = min
            maxValue = max
            displayedValues = values
            setOnValueChangedListener { _, _, newVal ->
                val newValue = newVal.toLong() to dataBinding.numberPickerConfigTo.value.toLong()
                isSavable(item.isSavable?.invoke(newValue) ?: true)
            }
        }

        dataBinding.numberPickerConfigTo.apply {
            minValue = min
            maxValue = max
            displayedValues = values
            setOnValueChangedListener { _, _, newVal ->
                val newValue = dataBinding.numberPickerConfigFrom.value.toLong() to newVal.toLong()
                isSavable(item.isSavable?.invoke(newValue) ?: true)
            }
        }

        dataBinding.numberPickerConfigFrom.value = from.toInt().coerceIn(min, max)
        dataBinding.numberPickerConfigTo.value = to.toInt().coerceIn(min, max)

    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.numberPickerConfigFrom.value.toLong() to dataBinding.numberPickerConfigTo.value.toLong()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigNumberRangeDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigNumberRangeDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: NumberRangeConfigItem, onDismiss: (() -> Unit)? = null) = ConfigNumberRangeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}