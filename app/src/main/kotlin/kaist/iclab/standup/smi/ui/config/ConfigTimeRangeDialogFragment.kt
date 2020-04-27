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
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogTimeRangeBinding
import kaist.iclab.standup.smi.ui.dialog.TimePickerDialogFragment

class ConfigTimeRangeDialogFragment :
    BaseBottomSheetDialogFragment<FragmentConfigDialogTimeRangeBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_time_range
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<LocalTimeRangeConfigItem>(ARG_ITEM) ?: return
        dataBinding.item = item

        val (from, to) = item.value?.invoke() ?: (0L to 0L)
        val (fromHour, fromMinute) = from.millisToHourMinute()
        val (toHour, toMinute) = to.millisToHourMinute()

        dataBinding.txtTimeConfigFrom.doOnTextChanged { text, _, _, _ ->
            val fromMillis = text.toHourMinutes().hourMinuteToMillis()
            val toMillis = dataBinding.txtTimeConfigTo.text.toHourMinutes().hourMinuteToMillis()
            onValueChanged(item, fromMillis, toMillis)
        }

        dataBinding.txtTimeConfigTo.doOnTextChanged { text, _, _, _ ->
            val fromMillis = dataBinding.txtTimeConfigFrom.text.toHourMinutes().hourMinuteToMillis()
            val toMillis = text.toHourMinutes().hourMinuteToMillis()
            onValueChanged(item, fromMillis, toMillis)
        }

        dataBinding.txtTimeConfigFrom.text = (fromHour to fromMinute).hourMinuteToString()
        dataBinding.txtTimeConfigTo.text = (toHour to toMinute).hourMinuteToString()

        dataBinding.txtTimeConfigFrom.setOnClickListener {
            val (hour, minute) = dataBinding.txtTimeConfigFrom.text.toHourMinutes()
            val dialog = TimePickerDialogFragment.newInstance(hour.toInt(), minute.toInt()) { h, m ->
                dataBinding.txtTimeConfigFrom.text = (h.toLong() to m.toLong()).hourMinuteToString()
            }
            dialog.show(parentFragmentManager, null)
        }

        dataBinding.txtTimeConfigTo.setOnClickListener {
            val (hour, minute) = dataBinding.txtTimeConfigTo.text.toHourMinutes()

            val dialog = TimePickerDialogFragment.newInstance(hour.toInt(), minute.toInt()) { h, m ->
                dataBinding.txtTimeConfigTo.text = (h.toLong() to m.toLong()).hourMinuteToString()
            }
            dialog.show(parentFragmentManager, null)
        }
        onValueChanged(item, from, to)
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val fromMillis = dataBinding.txtTimeConfigFrom.text.toHourMinutes().hourMinuteToMillis()
            val toMillis = dataBinding.txtTimeConfigTo.text.toHourMinutes().hourMinuteToMillis()
            dataBinding.item?.onSave?.invoke(fromMillis to toMillis)
        }
    }

    private fun onValueChanged(item: LocalTimeRangeConfigItem, fromValue: Long, toValue: Long) {
        isSavable(item.isSavable?.invoke(fromValue.toLong() to toValue.toLong()) ?: true)
    }

    companion object {
        private val ARG_ITEM = "${ConfigTimeRangeDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigTimeRangeDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: LocalTimeRangeConfigItem, onDismiss: (() -> Unit)? = null) = ConfigTimeRangeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}