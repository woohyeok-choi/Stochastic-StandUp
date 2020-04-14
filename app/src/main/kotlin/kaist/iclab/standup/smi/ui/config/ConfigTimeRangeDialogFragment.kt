package kaist.iclab.standup.smi.ui.config

import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.common.hourMinuteToMillis
import kaist.iclab.standup.smi.common.hourMinuteToString
import kaist.iclab.standup.smi.common.millisToHourMinute
import kaist.iclab.standup.smi.common.toHourMinutes
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogTimeRangeBinding
import kaist.iclab.standup.smi.ui.dialog.TimePickerDialogFragment


class ConfigTimeRangeDialogFragment :
    BaseBottomSheetDialogFragment<FragmentConfigDialogTimeRangeBinding>(), TimePickerDialogFragment.OnTimeSetListener {
    override val layoutId: Int = R.layout.fragment_config_dialog_time_range
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun beforeExecutePendingBindings() {
        val item = arguments?.getParcelable(ARG_ITEM) as? LocalTimeRangeConfigItem
        dataBinding.item = item
        val (from, to) = item?.value?.invoke() ?: (0L to 0L)
        val (fromHour, fromMinute) = from.millisToHourMinute()
        val (toHour, toMinute) = to.millisToHourMinute()

        dataBinding.txtTimeConfigFrom.doOnTextChanged { text, _, _, _ ->
            val fromMillis = text.toHourMinutes().hourMinuteToMillis()
            val toMillis = dataBinding.txtTimeConfigTo.text.toHourMinutes().hourMinuteToMillis()
            isSavable(
                item?.isSavable?.invoke(fromMillis to toMillis) ?: true
            )
        }

        dataBinding.txtTimeConfigTo.doOnTextChanged { text, _, _, _ ->
            val fromMillis = dataBinding.txtTimeConfigFrom.text.toHourMinutes().hourMinuteToMillis()
            val toMillis = text.toHourMinutes().hourMinuteToMillis()
            isSavable(
                item?.isSavable?.invoke(fromMillis to toMillis) ?: true
            )
        }

        dataBinding.txtTimeConfigFrom.text = (fromHour to fromMinute).hourMinuteToString()
        dataBinding.txtTimeConfigTo.text = (toHour to toMinute).hourMinuteToString()

        dataBinding.txtTimeConfigFrom.setOnClickListener {
            val (hour, minute) = dataBinding.txtTimeConfigFrom.text.toHourMinutes()
            val dialog = TimePickerDialogFragment.newInstance(hour.toInt(), minute.toInt())
            dialog.setTargetFragment(this, REQUEST_CODE_FROM)
            dialog.show(parentFragmentManager, javaClass.name)
        }

        dataBinding.txtTimeConfigTo.setOnClickListener {
            val (hour, minute) = dataBinding.txtTimeConfigTo.text.toHourMinutes()
            val dialog = TimePickerDialogFragment.newInstance(hour.toInt(), minute.toInt())
            dialog.setTargetFragment(this, REQUEST_CODE_TO)
            dialog.show(parentFragmentManager, javaClass.name)
        }
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val fromMillis = dataBinding.txtTimeConfigFrom.text.toHourMinutes().hourMinuteToMillis()
            val toMillis = dataBinding.txtTimeConfigTo.text.toHourMinutes().hourMinuteToMillis()
            dataBinding.item?.onSave?.invoke(fromMillis to toMillis)
        }
    }

    override fun onTimeSet(hour: Int, minute: Int, requestCode: Int) {
        if (requestCode == REQUEST_CODE_FROM) {
            dataBinding.txtTimeConfigFrom.text = (hour.toLong() to minute.toLong()).hourMinuteToString()
        }

        if (requestCode == REQUEST_CODE_TO) {
            dataBinding.txtTimeConfigTo.text = (hour.toLong() to minute.toLong()).hourMinuteToString()
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigTimeRangeDialogFragment::javaClass.name}.ARG_ITEM"
        private const val REQUEST_CODE_FROM = 0x01
        private const val REQUEST_CODE_TO = 0x02

        fun newInstance(item: LocalTimeRangeConfigItem) = ConfigTimeRangeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item
            )
        }
    }
}