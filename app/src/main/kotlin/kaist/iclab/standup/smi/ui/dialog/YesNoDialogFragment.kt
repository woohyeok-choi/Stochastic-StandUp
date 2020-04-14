package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class YesNoDialogFragment : DialogFragment() {
    interface OnClickListener {
        fun onClick(isPositive: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: ""
        val message = arguments?.getString(ARG_MESSAGE) ?: ""

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                if (targetFragment != null) {
                    (targetFragment as? OnClickListener)?.onClick(true)
                } else {
                    (activity as? OnClickListener)?.onClick(true)
                }
            }.setNegativeButton(android.R.string.no) { _, _ ->
                if (targetFragment != null) {
                    (targetFragment as? OnClickListener)?.onClick(false)
                } else {
                    (activity as? OnClickListener)?.onClick(false)
                }
            }.create()
    }

    companion object {
        private val ARG_TITLE = "${YesNoDialogFragment::javaClass.name}.ARG_TITLE"
        private val ARG_MESSAGE = "${YesNoDialogFragment::javaClass.name}.ARG_MESSAGE"
    }
}