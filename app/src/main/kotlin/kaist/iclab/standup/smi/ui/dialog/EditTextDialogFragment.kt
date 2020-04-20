package kaist.iclab.standup.smi.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.DialogFragment
import kaist.iclab.standup.smi.R

class EditTextDialogFragment : DialogFragment() {
    interface OnTextChangedListener {
        fun onTextChanged(prevText: String, curText: String, extra: Bundle?)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (context == null) return super.onCreateDialog(savedInstanceState)
        val title = arguments?.getString(ARG_TITLE, "") ?: ""
        val content = arguments?.getString(ARG_CONTENT, "") ?: ""
        val hint = arguments?.getString(ARG_HINT, "") ?: ""
        val extra = arguments?.getBundle(ARG_EXTRA)

        val editText = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setTextColor(ContextCompat.getColor(requireContext(), R.color.grey))
            setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.txt_size_default)
            )
            setText(content)
            setHint(hint)
            setHintTextColor(ContextCompat.getColor(requireContext(), R.color.light_grey))

        }
        val layout = FrameLayout(requireContext()).apply {
            setPadding(
                resources.getDimensionPixelSize(R.dimen.space_horizontal_large),
                resources.getDimensionPixelSize(R.dimen.space_vertical_small),
                resources.getDimensionPixelSize(R.dimen.space_horizontal_large),
                resources.getDimensionPixelSize(R.dimen.space_vertical_small)
            )
            addView(
                editText, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.yes) { _, _ ->
                if (targetFragment != null) {
                    (targetFragment as? OnTextChangedListener)?.onTextChanged(
                        content,
                        editText.text?.toString() ?: "",
                        extra
                    )
                } else {
                    (activity as? OnTextChangedListener)?.onTextChanged(
                        content,
                        editText.text?.toString() ?: "",
                        extra
                    )
                }
            }.setNegativeButton(android.R.string.cancel) { _, _ -> } .create()
    }

    companion object {
        private val PREFIX = EditTextDialogFragment::javaClass.name
        private val ARG_TITLE = "$PREFIX.ARG_TITLE"
        private val ARG_CONTENT = "$PREFIX.ARG_CONTENT"
        private val ARG_HINT = "$PREFIX.ARG_HINT"
        private val ARG_EXTRA = "$PREFIX.ARG_EXTRA"

        fun newInstance(
            title: String,
            content: String,
            hint: String? = null,
            extra: Bundle? = null
        ): EditTextDialogFragment = EditTextDialogFragment().apply {
            arguments = bundleOf(
                ARG_TITLE to title,
                ARG_CONTENT to content,
                ARG_HINT to hint,
                ARG_EXTRA to extra
            )
        }
    }
}