package kaist.iclab.standup.smi.ui.main

import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseActivity
import kaist.iclab.standup.smi.databinding.ActivityMainBinding
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.StandUpMissionHandler
import kaist.iclab.standup.smi.repository.StatRepository
import kaist.iclab.standup.smi.ui.dashboard.DashboardViewModel
import kaist.iclab.standup.smi.ui.timeline.TimelineViewModel
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : BaseActivity<ActivityMainBinding, MainViewModel>(){
    override val viewModel: MainViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.activity_main

    private val repository: StandUpMissionHandler by inject()
    private val stat: StatRepository by inject()

    val dashboardViewModel: DashboardViewModel by viewModel()
    val timelineViewModel: TimelineViewModel by viewModel()

    override fun beforeExecutePendingBindings() {
        lifecycleScope.launchWhenCreated {
            RemotePrefs.sync()
            //Debug.generateDebugData(repository, resources)
        }

        val navController = findNavController(R.id.fragment_nav_host)
        dataBinding.navBottom.setupWithNavController(navController)
        dataBinding.navBottom.setOnNavigationItemReselectedListener {  }
    }
}