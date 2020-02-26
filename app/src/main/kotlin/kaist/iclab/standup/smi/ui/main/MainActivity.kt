package kaist.iclab.standup.smi.ui.main

import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseActivity
import kaist.iclab.standup.smi.databinding.ActivityMainBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(), MainNavigator {
    override val viewModel: MainViewModel by viewModel { parametersOf(this) }
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.activity_main
    override val menuRes: Int? = null

    override fun beforeExecutePendingBindings() {
        val navController = findNavController(R.id.fragment_nav_host)
        dataBinding.navBottom.setupWithNavController(navController)
    }

    override fun navigateError(throwable: Throwable?) {

    }
}