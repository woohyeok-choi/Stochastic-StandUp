package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import java.io.Serializable

class SingleChoiceDialogFragment : DialogFragment() {
    @Suppress("UNCHECKED_CAST")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arguments?.getStringArray(ARG_ITEMS) ?: arrayOf()
        val onItemSelected = arguments?.getSerializable(ARG_ON_ITEM_SELECTED) as? (String) -> Unit

        return AlertDialog.Builder(requireContext())
            .setItems(items) { _, pos ->
                onItemSelected?.invoke(items[pos])
            }.create()
    }

    companion object {
        private val ARG_ITEMS = "${SingleChoiceDialogFragment::class.java.name}.ARG_ITEMS"
        private val ARG_ON_ITEM_SELECTED = "${SingleChoiceDialogFragment::class.java.name}.ARG_ON_ITEM_SELECTED"

        fun newInstance(items: Array<String>, onItemSelected: (item: String) -> Unit) = SingleChoiceDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEMS to items,
                ARG_ON_ITEM_SELECTED to onItemSelected
            )
        }
    }
}