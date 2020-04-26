package kaist.iclab.standup.smi.ui.timeline

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.base.BaseDialogFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDialogPlaceRenameBinding

class TimelinePlaceRenameDialogFragment : BaseBottomSheetDialogFragment<FragmentTimelineDialogPlaceRenameBinding>() {
    override val layoutId: Int = R.layout.fragment_timeline_dialog_place_rename
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_BottomSheetEditText)
    }

    override fun beforeExecutePendingBindings() {
        val curName = arguments?.getString(ARG_CURRENT_NAME, "") ?: ""

        dataBinding.name = curName
        dataBinding.edtRename.doOnTextChanged { text, _, _ ,_ ->
            isSavable(!text.isNullOrBlank())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onClick(isPositive: Boolean) {
        if (!isPositive) return
        val newName = dataBinding.edtRename.text?.toString() ?: ""
        val onChanged = arguments?.getSerializable(ARG_ON_CHANGED) as? (String) -> Unit

        onChanged?.invoke(newName)
    }

    companion object {
        private val ARG_CURRENT_NAME = "${TimelinePlaceRenameDialogFragment::class.java.name}.ARG_CURRENT_NAME"
        private val ARG_ON_CHANGED = "${TimelinePlaceRenameDialogFragment::class.java.name}.ARG_ON_CHANGED"

        fun newInstance(curName: String, onChanged: (newName: String) -> Unit) = TimelinePlaceRenameDialogFragment().apply {
            arguments = bundleOf(
                ARG_CURRENT_NAME to curName,
                ARG_ON_CHANGED to onChanged
            )
        }
    }
}