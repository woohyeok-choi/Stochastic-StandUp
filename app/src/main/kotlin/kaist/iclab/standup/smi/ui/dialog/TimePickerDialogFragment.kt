package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import java.io.Serializable
import kotlin.math.min

class TimePickerDialogFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context ?: return super.onCreateDialog(savedInstanceState)

        val hour = arguments?.getInt(ARG_HOUR, 0) ?: 0
        val minute = arguments?.getInt(ARG_MINUTE, 0) ?: 0

        return TimePickerDialog(ctx, this, hour, minute, true)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val onTimeSet = arguments?.getSerializable(ARG_ON_TIME_SET) as? (Int, Int) -> Unit
        onTimeSet?.invoke(hourOfDay, minute)
    }

    companion object {
        private val ARG_HOUR = "${TimePickerDialogFragment::class.java.name}.ARG_HOUR"
        private val ARG_MINUTE = "${TimePickerDialogFragment::class.java.name}.ARG_MINUTE"
        private val ARG_ON_TIME_SET = "${TimePickerDialogFragment::class.java.name}.ARG_ON_TIME_SET"

        fun newInstance(hour: Int, minute: Int, onTimeSet: (hour: Int, minute: Int) -> Unit) = TimePickerDialogFragment().apply {
            arguments = bundleOf(
                ARG_HOUR to hour,
                ARG_MINUTE to minute,
                ARG_ON_TIME_SET to onTimeSet
            )
        }
    }
}