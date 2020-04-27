package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.common.hourMinuteToMillis
import kaist.iclab.standup.smi.common.hourMinuteToString
import kaist.iclab.standup.smi.common.millisToHourMinute
import kaist.iclab.standup.smi.common.toHourMinutes
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogTimeBinding
import kaist.iclab.standup.smi.ui.dialog.TimePickerDialogFragment
import kotlin.math.min

class ConfigTimeDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogTimeBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_time
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<LocalTimeConfigItem>(ARG_ITEM) ?: return

        dataBinding.item = item

        val value = item.value?.invoke() ?: 0
        val (hourValue, minuteValue) = value.millisToHourMinute()

        dataBinding.txtTimeConfig.doOnTextChanged { text, _, _, _ ->
            val newValue = text.toHourMinutes().hourMinuteToMillis()
            onValueChanged(item, newValue)
        }

        dataBinding.txtTimeConfig.setOnClickListener {
            val (hour, minute) = dataBinding.txtTimeConfig.text.toHourMinutes()
            val dialog = TimePickerDialogFragment.newInstance(hour.toInt(), minute.toInt()) { h, m ->
                dataBinding.txtTimeConfig.text = (h.toLong() to m.toLong()).hourMinuteToString()
            }
            dialog.show(parentFragmentManager, null)
        }

        dataBinding.txtTimeConfig.text = (hourValue to minuteValue).hourMinuteToString()
        onValueChanged(item, value)
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val newValue = dataBinding.txtTimeConfig.text.toHourMinutes().hourMinuteToMillis()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    private fun onValueChanged(item: LocalTimeConfigItem, value: Long) {
        isSavable(item.isSavable?.invoke(value) ?: true)
    }

    companion object {
        private val ARG_ITEM = "${ConfigTimeDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigTimeDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: LocalTimeConfigItem, onDismiss: (() -> Unit)? = null) = ConfigTimeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}