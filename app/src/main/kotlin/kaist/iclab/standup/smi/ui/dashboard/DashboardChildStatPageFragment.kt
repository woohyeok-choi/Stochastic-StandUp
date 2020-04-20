package kaist.iclab.standup.smi.ui.dashboard

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentDashboardStatPageBinding
import kaist.iclab.standup.smi.ui.dashboard.DashboardFragment.Companion.ARG_HAS_NEXT
import kaist.iclab.standup.smi.ui.dashboard.DashboardFragment.Companion.ARG_HAS_PREVIOUS

class DashboardChildStatPageFragment : BaseFragment<FragmentDashboardStatPageBinding, DashboardViewModel>() {
    override val viewModel: DashboardViewModel by sharedViewModelFromFragment()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard_stat_page

    override fun beforeExecutePendingBindings() {
        dataBinding.hasNext = arguments?.getBoolean(ARG_HAS_NEXT) ?: false
        dataBinding.hasPrevious = arguments?.getBoolean(ARG_HAS_PREVIOUS) ?: false

        dataBinding.imgLeftMore.setOnClickListener {
            (parentFragment as? DashboardNavigator)?.navigatePreviousDate()
        }

        dataBinding.imgRightMore.setOnClickListener {
            (parentFragment as? DashboardNavigator)?.navigateNextDate()
        }
    }
}