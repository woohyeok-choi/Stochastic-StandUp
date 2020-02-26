package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

class YesNoDialogFragment : DialogFragment() {
    private lateinit var title: String
    private lateinit var message: String

    private var onPositiveButtonSelected: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                onPositiveButtonSelected?.invoke()
            }.setNegativeButton(android.R.string.no) { _, _ ->
            }.create()
    }

}