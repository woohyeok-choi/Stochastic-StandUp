package kaist.iclab.standup.smi.ui.config

import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class ConfigFragment : BaseFragment<FragmentConfigBinding, ConfigViewModel>(), ConfigNavigator {
    override val viewModel: ConfigViewModel by viewModel { parametersOf(this) }
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_config
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {

    }

    override fun navigateError(throwable: Throwable?) {

    }
}