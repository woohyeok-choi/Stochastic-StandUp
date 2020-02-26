package kaist.iclab.standup.smi.common

data class Status(val state: Int, val error: StandUpError? = null) {
    companion object {
        private const val STATE_INIT = 0
        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1

        fun init() = Status(STATE_INIT)
        fun loading() = Status(STATE_LOADING)
        fun success() = Status(STATE_SUCCESS)
        fun failure(t: StandUpError? = null) = Status(STATE_FAILURE, t)
    }

    fun isSucceeded(): Boolean = state == STATE_SUCCESS

    fun isLoading(): Boolean = state == STATE_LOADING

    fun isFailed(): Boolean = state == STATE_FAILURE
}