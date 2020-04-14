package kaist.iclab.standup.smi.ui.config

import android.text.format.DateUtils
import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.common.hourMinuteToMillis
import kaist.iclab.standup.smi.common.millisToHourMinute
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogNumberBinding
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogTimeBinding
import java.util.concurrent.TimeUnit
import kotlin.math.min

class ConfigTimeDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogTimeBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_time
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true



    override fun beforeExecutePendingBindings() {
        val item = arguments?.getParcelable(ARG_ITEM) as? LocalTimeConfigItem
        dataBinding.item = item

        val value = item?.value?.invoke() ?: 0
        val (hourValue, minuteValue) = value.millisToHourMinute()

        dataBinding.timePickerConfig.setOnTimeChangedListener { _, hourOfDay, minute ->
            val newValue = (hourOfDay.toLong() to minute.toLong()).hourMinuteToMillis()
            isSavable(
                item?.isSavable?.invoke(newValue) ?: true
            )
        }

        dataBinding.timePickerConfig.hour = hourValue.toInt()
        dataBinding.timePickerConfig.minute = minuteValue.toInt()
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val hour = dataBinding.timePickerConfig.hour
            val minute = dataBinding.timePickerConfig.minute
            val newValue = (hour.toLong() to minute.toLong()).hourMinuteToMillis()
            dataBinding.item?.onSave?.invoke(newValue)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigTimeDialogFragment::javaClass.name}.ARG_ITEM"

        fun newInstance(item: LocalTimeConfigItem) = ConfigTimeDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item
            )
        }
    }

}