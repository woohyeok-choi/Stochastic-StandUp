package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment

class TimePickerDialogFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {
    interface OnTimeSetListener {
        fun onTimeSet(hour: Int, minute: Int, requestCode: Int)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val ctx = context ?: return super.onCreateDialog(savedInstanceState)
        val hour = arguments?.getInt(ARG_HOUR, 0) ?: 0
        val minute = arguments?.getInt(ARG_MINUTE, 0) ?: 0

        return TimePickerDialog(ctx, this, hour, minute, true)
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        if (targetFragment != null) {
            (targetFragment as? OnTimeSetListener)?.onTimeSet(hourOfDay, minute, targetRequestCode)
        } else {
            (activity as? OnTimeSetListener)?.onTimeSet(hourOfDay, minute, targetRequestCode)
        }
    }

    companion object {
        private val PREFIX = TimePickerDialogFragment::javaClass.name
        private val ARG_HOUR = "$PREFIX.ARG_HOUR"
        private val ARG_MINUTE = "$PREFIX.ARG_MINUTE"

        fun newInstance(hour: Int, minute: Int) = TimePickerDialogFragment().apply {
            arguments = bundleOf(
                ARG_HOUR to hour,
                ARG_MINUTE to minute
            )
        }
    }


}