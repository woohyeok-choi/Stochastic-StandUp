package kaist.iclab.standup.smi.ui.config

import android.util.Log
import androidx.core.os.bundleOf
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogNumberBinding

class ConfigNumberDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogNumberBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_number
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<NumberConfigItem>(ARG_ITEM) ?: return

        val min = item.min.toInt()
        val max = item.max.toInt()
        val value = item.value?.invoke()?.toInt() ?: 0
        val values = (min..max).map {
            item.valueFormatter?.invoke(it.toLong()) ?: it.toString()
        }.toTypedArray()

        dataBinding.item = item
        dataBinding.numberPickerConfig.minValue = min
        dataBinding.numberPickerConfig.maxValue = max
        dataBinding.numberPickerConfig.setOnValueChangedListener { _, _, newVal ->
            isSavable(item.isSavable?.invoke(newVal.toLong()) ?: true)
        }
        dataBinding.numberPickerConfig.displayedValues = values
        dataBinding.numberPickerConfig.value = value.coerceIn(min, max)
        isSavable(item.isSavable?.invoke(value.coerceIn(min, max).toLong()) ?: true)
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.numberPickerConfig.value.toLong()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigNumberDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigNumberDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: NumberConfigItem, onDismiss: (() -> Unit)? = null) = ConfigNumberDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}