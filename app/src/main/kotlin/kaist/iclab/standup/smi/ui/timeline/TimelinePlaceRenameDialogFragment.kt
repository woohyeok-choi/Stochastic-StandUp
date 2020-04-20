package kaist.iclab.standup.smi.ui.timeline

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.base.BaseDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDialogPlaceRenameBinding

class TimelinePlaceRenameDialogFragment : BaseBottomSheetDialogFragment<FragmentTimelineDialogPlaceRenameBinding>() {
    override val layoutId: Int = R.layout.fragment_timeline_dialog_place_rename
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    interface OnTextChangedListener {
        fun onTextChanged(prevText: String, curText: String, extra: Bundle?)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_BottomSheetEditText)
    }

    override fun beforeExecutePendingBindings() {
        val prevName = arguments?.getString(ARG_CONTENT, "") ?: ""

        dataBinding.name = prevName
        dataBinding.edtRename.doOnTextChanged { text, _, _ ,_ ->
            isSavable(!text.isNullOrBlank())
        }
    }

    override fun onClick(isPositive: Boolean) {
        if (!isPositive) return

        if (targetFragment != null) {
            (targetFragment as? OnTextChangedListener)?.onTextChanged(
                prevText = arguments?.getString(ARG_CONTENT, "") ?: "",
                curText = dataBinding.edtRename.text?.toString() ?: "",
                extra = arguments?.getBundle(ARG_EXTRA)
            )
        }
    }

    companion object {
        private val PREFIX = TimelinePlaceRenameDialogFragment::javaClass.name
        private val ARG_CONTENT = "$PREFIX.ARG_CONTENT"
        private val ARG_EXTRA = "$PREFIX.ARG_EXTRA"

        fun newInstance(
            name: String,
            extra: Bundle? = null
        ): TimelinePlaceRenameDialogFragment = TimelinePlaceRenameDialogFragment().apply {
            arguments = bundleOf(
                ARG_CONTENT to name,
                ARG_EXTRA to extra
            )
        }
    }
}