package kaist.iclab.standup.smi.ui.timeline

import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.observe
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDailyListBinding
import kaist.iclab.standup.smi.repository.SedentaryMissionEvent

class TimelineChildDailyListFragment : BaseFragment<FragmentTimelineDailyListBinding, TimelineViewModel>() {
    override val viewModel: TimelineViewModel by sharedViewModelFromFragment()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline_daily_list

    override fun beforeExecutePendingBindings() {
        val adapter = TimelineDailyListAdapter()

        adapter.listener = parentFragment as? OnTimelineItemListener

        viewModel.dailyStats.observe(this) { items ->
            items?.let {
                adapter.items = it
            }
        }

        dataBinding.listTimeline.adapter = adapter
    }

}