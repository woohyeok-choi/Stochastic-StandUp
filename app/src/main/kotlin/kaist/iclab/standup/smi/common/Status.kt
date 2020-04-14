package kaist.iclab.standup.smi.common

data class Result<T>(val state: Int, val data: T? = null, val error: StandUpError? = null) {
    companion object {
        private const val STATE_INIT = 0

        const val STATE_LOADING = 1
        const val STATE_SUCCESS = 2
        const val STATE_FAILURE = -1

        fun <R> init() = Result<R>(STATE_INIT)
        fun <R> loading() = Result<R>(STATE_LOADING)
        fun <R> success(data: R?) = Result<R>(STATE_SUCCESS, data)
        fun <R> failure(t: StandUpError? = null) = Result<R>(STATE_FAILURE, null, t)
    }

    fun isSucceeded(): Boolean = state == STATE_SUCCESS

    fun isLoading(): Boolean = state == STATE_LOADING

    fun isFailed(): Boolean = state == STATE_FAILURE
}