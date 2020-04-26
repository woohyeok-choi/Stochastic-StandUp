package kaist.iclab.standup.smi.ui.timeline


import android.view.ContextMenu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelinePlaceListBinding

class TimelineChildPlaceListFragment : BaseFragment<FragmentTimelinePlaceListBinding, TimelineViewModel>() {
    override val viewModel: TimelineViewModel by sharedViewModelFromFragment()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline_place_list

    override fun beforeExecutePendingBindings() {
        val adapter = TimelinePlaceListAdapter()

        adapter.listener = parentFragment as? OnTimelineItemListener

        viewModel.placeStats.observe(this) { stats ->
            stats?.let { adapter.submitList(it) }
        }

        dataBinding.listTimeline.adapter = adapter
        dataBinding.listTimeline.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
    }
}