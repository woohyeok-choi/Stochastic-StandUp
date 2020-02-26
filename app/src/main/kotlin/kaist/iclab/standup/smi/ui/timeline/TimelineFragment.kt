package kaist.iclab.standup.smi.ui.timeline

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class TimelineFragment : BaseFragment<FragmentTimelineBinding, TimelineViewModel>(), TimelineNavigator {
    override val viewModel: TimelineViewModel by viewModel { parametersOf(this) }
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {

    }

    override fun navigateError(throwable: Throwable?) {

    }
}