package kaist.iclab.standup.smi.ui.dashboard

import kaist.iclab.standup.smi.base.BaseNavigator

interface DashboardNavigator : BaseNavigator {
    fun navigateError(throwable: Throwable?)
    fun navigatePreviousDate()
    fun navigateNextDate()
    fun navigateChartType(chartType: Int)
}