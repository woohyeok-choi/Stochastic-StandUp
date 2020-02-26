package kaist.iclab.standup.smi.ui.daily

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentDailyChartBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class DailyChartFragment : BaseFragment<FragmentDailyChartBinding, DailyChartViewModel>() {
    override val viewModel: DailyChartViewModel by viewModel { parametersOf(this) }
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_daily_chart
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {

    }
}