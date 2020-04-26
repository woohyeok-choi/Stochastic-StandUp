package kaist.iclab.standup.smi.ui.config

import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigDialogSingleChoiceBinding

class ConfigSingleChoiceDialogFragment : BaseBottomSheetDialogFragment<FragmentConfigDialogSingleChoiceBinding>() {
    override val layoutId: Int = R.layout.fragment_config_dialog_single_choice
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    private lateinit var viewIdToOptions: Map<Int?, Int>

    @Suppress("UNCHECKED_CAST")
    override fun beforeExecutePendingBindings() {
        onDismiss = arguments?.getSerializable(ARG_ON_DISMISS) as? () -> Unit

        val item = arguments?.getParcelable<SingleChoiceConfigItem>(ARG_ITEM) ?: return
        dataBinding.item = item

        val options = item.options
        val optionsToView = options.associate { option ->
            val button = RadioButton(requireContext()).apply {
                id = View.generateViewId()
                background = resources.getDrawable(R.drawable.bg_text_radio_button, null)
                setButtonDrawable(android.R.color.transparent)
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.space_horizontal_large),
                    resources.getDimensionPixelSize(R.dimen.space_vertical_default),
                    resources.getDimensionPixelSize(R.dimen.space_horizontal_large),
                    resources.getDimensionPixelSize(R.dimen.space_vertical_default)
                )
                text = if (item.valueFormatter == null) {
                    item.formatter?.invoke(option)
                } else {
                    item.valueFormatter?.invoke(option)
                } ?: option.toString()
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.txt_size_default))
                setTextColor(resources.getColor(R.color.grey, null))
            }
            option to button
        }

        optionsToView.values.forEach {
            dataBinding.radioOptions.addView(
                it,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        viewIdToOptions = optionsToView.keys.associateBy {
            optionsToView[it]?.id
        }

        val value = item.value?.invoke() ?: 0
        val selectedId = optionsToView[value]?.id ?: -1
        val firstChild = dataBinding.radioOptions.children.firstOrNull()

        dataBinding.radioOptions.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId < 0) {
                isSavable(false)
            } else {
                val checkedOption = viewIdToOptions[checkedId] ?: return@setOnCheckedChangeListener
                isSavable(item.isSavable?.invoke(checkedOption) ?: true)
            }
        }

        if (selectedId > 0) dataBinding.radioOptions.check(selectedId)

        if (dataBinding.radioOptions.checkedRadioButtonId < 0 && firstChild != null) {
            dataBinding.radioOptions.check(firstChild.id)
        }
    }

    override fun onClick(isPositive: Boolean) {
        if (isPositive) {
            val checkedId = dataBinding.radioOptions.checkedRadioButtonId
            val option = viewIdToOptions[checkedId] ?: 0
            dataBinding.item?.onSave?.invoke(option)
        }
    }

    companion object {
        private val ARG_ITEM = "${ConfigSingleChoiceDialogFragment::class.java.name}.ARG_ITEM"
        private val ARG_ON_DISMISS = "${ConfigSingleChoiceDialogFragment::class.java.name}.ON_DISMISS"

        fun newInstance(item: SingleChoiceConfigItem, onDismiss: (() -> Unit)? = null) = ConfigSingleChoiceDialogFragment().apply {
            arguments = bundleOf(
                ARG_ITEM to item,
                ARG_ON_DISMISS to onDismiss
            )
        }
    }
}
