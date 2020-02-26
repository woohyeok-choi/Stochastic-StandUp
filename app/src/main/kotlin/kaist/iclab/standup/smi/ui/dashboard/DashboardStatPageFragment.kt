package kaist.iclab.standup.smi.ui.dashboard

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentDashboardStatPageBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DashboardStatPageFragment : BaseFragment<FragmentDashboardStatPageBinding, DashboardViewModel>() {
    override val viewModel: DashboardViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard_stat_page
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {

    }


}