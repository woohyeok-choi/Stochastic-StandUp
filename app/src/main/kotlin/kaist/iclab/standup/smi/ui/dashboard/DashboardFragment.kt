package kaist.iclab.standup.smi.ui.dashboard

import android.util.Log
import androidx.fragment.app.commit
import androidx.viewpager2.widget.ViewPager2
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.snackBar
import kaist.iclab.standup.smi.common.toast
import kaist.iclab.standup.smi.common.wrap
import kaist.iclab.standup.smi.databinding.FragmentDashboardBinding
import kaist.iclab.standup.smi.pref.LocalPrefs
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.Duration
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class DashboardFragment : BaseFragment<FragmentDashboardBinding, DashboardViewModel>(),
    DashboardNavigator {
    override val viewModel: DashboardViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard

    private val defaultTimeZone = DateTimeZone.getDefault()
    private val firstDate = DateTime(if (BuildConfig.FIREBASE_TEST_MODE) 1536146147659 else LocalPrefs.firstUsageTime, defaultTimeZone).withTimeAtStartOfDay()
    private val nowDate = DateTime(if (BuildConfig.FIREBASE_TEST_MODE) 1540605842591 else System.currentTimeMillis(), defaultTimeZone).withTimeAtStartOfDay()
    private val nDays = Duration(firstDate, nowDate).toStandardDays().days + 1

    override fun beforeExecutePendingBindings() {
        viewModel.navigator = this

        val adapter = DashboardStatPagerAdapter(
            nItemCount = nDays,
            fragment = this
        )

        childFragmentManager.commit {
            replace(R.id.container_daily_overview,
                DashboardChildDailyOverviewFragment()
            )
            replace(R.id.container_weekly_chart,
                DashboardChildWeeklyChartFragment()
            )
        }

        dataBinding.containerPager.adapter = adapter
        dataBinding.containerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                loadDailyData(position)
            }
        })
        dataBinding.containerPager.setCurrentItem(adapter.itemCount - 1, false)

        viewModel.loadChart(CHART_TYPE_TOTAL_SEDENTARY_TIME)
    }

    private fun loadDailyData(position: Int) {
        viewModel.loadData(firstDate.plusDays(position))
    }

    override fun navigateError(throwable: Throwable?) {
        snackBar(
            view = dataBinding.root,
            anchorId = R.id.nav_bottom,
            isShort = false,
            msg = throwable.wrap().toString(this),
            actionName = getString(R.string.general_retry)
        ) {
            viewModel.refresh()
        }
    }

    override fun navigatePreviousDate() {
        dataBinding.containerPager.currentItem -= 1
        loadDailyData(dataBinding.containerPager.currentItem)
    }

    override fun navigateNextDate() {
        dataBinding.containerPager.currentItem += 1
        loadDailyData(dataBinding.containerPager.currentItem)
    }

    override fun navigateChartType(chartType: Int) {
        viewModel.loadChart(chartType)
    }

    companion object {
        const val CHART_TYPE_TOTAL_SEDENTARY_TIME = 0x00
        const val CHART_TYPE_AVG_SEDENTARY_TIME = 0x01
        const val CHART_TYPE_NUM_PROLONGED_SEDENTARINESS = 0x02
        const val ARG_HAS_NEXT = "${BuildConfig.APPLICATION_ID}.ARG_HAS_NEXT"
        const val ARG_HAS_PREVIOUS = "${BuildConfig.APPLICATION_ID}.ARG_HAS_PREVIOUS"
    }
}
