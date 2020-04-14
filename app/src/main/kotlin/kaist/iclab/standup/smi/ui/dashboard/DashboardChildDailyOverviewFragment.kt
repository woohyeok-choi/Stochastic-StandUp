package kaist.iclab.standup.smi.ui.dashboard

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentDashboardDailyOverviewBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DashboardChildDailyOverviewFragment : BaseFragment<FragmentDashboardDailyOverviewBinding, DashboardViewModel>() {
    override val viewModel: DashboardViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard_daily_overview

    override fun beforeExecutePendingBindings() {
    }
}