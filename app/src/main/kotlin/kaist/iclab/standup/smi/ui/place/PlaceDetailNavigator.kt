package kaist.iclab.standup.smi.ui.place

import kaist.iclab.standup.smi.base.BaseNavigator

interface PlaceNavigator : BaseNavigator{
    fun navigateError(throwable: Throwable?)
}