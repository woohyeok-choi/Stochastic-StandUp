package kaist.iclab.standup.smi.base

interface BaseNavigator {
    fun navigateError(throwable: Throwable? = null)
}