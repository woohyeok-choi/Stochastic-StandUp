package kaist.iclab.standup.smi.ui.timeline


import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDialogPlaceOrderBinding
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_INCENTIVE
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_MISSIONS
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_VISITS
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_VISIT_TIME
import org.koin.android.ext.android.inject

class TimelinePlaceOrderDialogFragment : BaseBottomSheetDialogFragment<FragmentTimelineDialogPlaceOrderBinding>(){
    override val layoutId: Int = R.layout.fragment_timeline_dialog_place_order
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    override fun beforeExecutePendingBindings() {
        val direction = arguments?.getBoolean(ARG_IS_DESCENDING, true) ?: true
        val field = arguments?.getInt(ARG_FIELD, EXTRA_FIELD_VISIT_TIME) ?: EXTRA_FIELD_VISIT_TIME

        val directionId = if (direction) {
            R.id.btn_place_order_direction_descending
        } else {
            R.id.btn_place_order_direction_ascending
        }

        val fieldId = when (field) {
            EXTRA_FIELD_INCENTIVE -> R.id.btn_place_order_field_incentive
            EXTRA_FIELD_MISSIONS -> R.id.btn_place_order_field_num_mission
            EXTRA_FIELD_VISITS -> R.id.btn_place_order_field_num_visits
            else -> R.id.btn_place_order_field_visit_time
        }

        dataBinding.groupPlaceOrderDirection.check(directionId)
        dataBinding.groupPlaceOrderField.check(fieldId)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onClick(isPositive: Boolean) {
        if (!isPositive) return
        val isDescending = dataBinding.groupPlaceOrderDirection.checkedRadioButtonId == R.id.btn_place_order_direction_descending

        val field = when (dataBinding.groupPlaceOrderField.checkedRadioButtonId) {
            R.id.btn_place_order_field_incentive -> EXTRA_FIELD_INCENTIVE
            R.id.btn_place_order_field_num_mission -> EXTRA_FIELD_MISSIONS
            R.id.btn_place_order_field_num_visits -> EXTRA_FIELD_VISITS
            else -> EXTRA_FIELD_VISIT_TIME
        }

        val onSelected = arguments?.getSerializable(ARG_ON_SELECTED) as? (Boolean, Int) -> Unit
        onSelected?.invoke(isDescending, field)
    }

    companion object {
        private val ARG_FIELD = "${TimelinePlaceOrderDialogFragment::class.java.name}.ARG_FIELD"
        private val ARG_IS_DESCENDING = "${TimelinePlaceOrderDialogFragment::class.java.name}.ARG_IS_DESCENDING"
        private val ARG_ON_SELECTED = "${TimelinePlaceOrderDialogFragment::class.java.name}.ARG_ON_SELECTED"

        fun newInstance(isDescending: Boolean, field: Int, onSelected: (isDescending: Boolean, field: Int) -> Unit) = TimelinePlaceOrderDialogFragment().apply {
            arguments = bundleOf(
                ARG_IS_DESCENDING to isDescending,
                ARG_FIELD to field,
                ARG_ON_SELECTED to onSelected
            )
        }
    }
}