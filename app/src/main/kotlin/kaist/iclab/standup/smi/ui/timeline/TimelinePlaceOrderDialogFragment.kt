package kaist.iclab.standup.smi.ui.timeline


import androidx.core.os.bundleOf
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDialogPlaceOrderBinding
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_DURATION
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_INCENTIVE
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_MISSIONS
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_VISITS
import kaist.iclab.standup.smi.ui.timeline.TimelineFragment.Companion.EXTRA_FIELD_VISIT_TIME

class TimelinePlaceOrderDialogFragment : BaseBottomSheetDialogFragment<FragmentTimelineDialogPlaceOrderBinding>(){
    override val layoutId: Int = R.layout.fragment_timeline_dialog_place_order
    override val showPositiveButton: Boolean = true
    override val showNegativeButton: Boolean = true

    interface OnPlaceOrderChangedListener {
        fun onOrderChanged(isDescending: Boolean, field: Int)
    }

    override fun beforeExecutePendingBindings() {
        val direction = arguments?.getBoolean(ARG_IS_DESCENDING, true) ?: true
        val field = arguments?.getInt(ARG_FIELD, EXTRA_FIELD_VISIT_TIME) ?: EXTRA_FIELD_VISIT_TIME

        val directionId = if (direction) {
            R.id.btn_place_order_direction_descending
        } else {
            R.id.btn_place_order_direction_ascending
        }

        val fieldId = when (field) {
            EXTRA_FIELD_DURATION -> R.id.btn_place_order_field_duration
            EXTRA_FIELD_INCENTIVE -> R.id.btn_place_order_field_incentive
            EXTRA_FIELD_MISSIONS -> R.id.btn_place_order_field_num_mission
            EXTRA_FIELD_VISITS -> R.id.btn_place_order_field_num_visits
            else -> R.id.btn_place_order_field_visit_time
        }

        dataBinding.groupPlaceOrderDirection.check(directionId)
        dataBinding.groupPlaceOrderField.check(fieldId)
    }

    override fun onClick(isPositive: Boolean) {
        val isDescending = dataBinding.groupPlaceOrderDirection.checkedRadioButtonId == R.id.btn_place_order_direction_descending

        val field = when (dataBinding.groupPlaceOrderField.checkedRadioButtonId) {
            R.id.btn_place_order_field_duration -> EXTRA_FIELD_DURATION
            R.id.btn_place_order_field_incentive -> EXTRA_FIELD_INCENTIVE
            R.id.btn_place_order_field_num_mission -> EXTRA_FIELD_MISSIONS
            R.id.btn_place_order_field_num_visits -> EXTRA_FIELD_VISITS
            else -> EXTRA_FIELD_VISIT_TIME
        }

        if (targetFragment != null) {
            (targetFragment as? OnPlaceOrderChangedListener)?.onOrderChanged(isDescending, field)
        } else {
            (activity as? OnPlaceOrderChangedListener)?.onOrderChanged(isDescending, field)
        }
    }

    companion object {
        private val PREFIX = TimelinePlaceOrderDialogFragment::javaClass.name
        private val ARG_IS_DESCENDING = "$PREFIX.ARG_IS_DESCENDING"
        private val ARG_FIELD = "$PREFIX.ARG_FIELD"

        fun newInstance(
            isDescending: Boolean,
            field: Int
        ) = TimelinePlaceOrderDialogFragment().apply {
            arguments = bundleOf(
                ARG_IS_DESCENDING to isDescending,
                ARG_FIELD to field
            )
        }
    }
}