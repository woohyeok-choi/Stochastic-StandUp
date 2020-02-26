package kaist.iclab.standup.smi.ui.dashboard

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentDashboardBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DashboardFragment : BaseFragment<FragmentDashboardBinding, DashboardViewModel>(), DashboardNavigator {
    override val viewModel: DashboardViewModel by viewModel { parametersOf(this) }
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {
    }

    override fun navigateError(throwable: Throwable?) {
    }
}
