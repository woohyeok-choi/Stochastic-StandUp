package kaist.iclab.standup.smi.ui.timeline

import kaist.iclab.standup.smi.base.BaseNavigator

interface TimelineNavigator : BaseNavigator {
    fun navigateDailyStatError(throwable: Throwable?)
    fun navigatePlaceStatError(throwable: Throwable?)
    fun navigatePlaceRenameError(throwable: Throwable?)
}