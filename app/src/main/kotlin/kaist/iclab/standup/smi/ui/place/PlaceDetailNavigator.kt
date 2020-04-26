package kaist.iclab.standup.smi.ui.place

import kaist.iclab.standup.smi.base.BaseNavigator

interface PlaceDetailNavigator : BaseNavigator{
    fun navigateError(throwable: Throwable?)
}