package kaist.iclab.standup.smi.ui.timeline

import kaist.iclab.standup.smi.base.BaseNavigator

interface TimelineNavigator : BaseNavigator {
    suspend fun navigateBeforeDataLoad()
    fun navigateDailyStatError(throwable: Throwable?)
    fun navigatePlaceStatError(throwable: Throwable?)
    fun navigatePlaceRenameError(throwable: Throwable?)
}