package kaist.iclab.standup.smi.ui.timeline

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineBinding
import kaist.iclab.standup.smi.databinding.FragmentTimelineListBinding
import kaist.iclab.standup.smi.ui.timeline.TimelineViewModel
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TimelineListFragment : BaseFragment<FragmentTimelineListBinding, TimelineViewModel>() {
    override val viewModel: TimelineViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline_list
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {

    }

}