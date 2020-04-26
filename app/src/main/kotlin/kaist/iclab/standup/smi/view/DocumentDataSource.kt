package kaist.iclab.standup.smi.view

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import kaist.iclab.standup.smi.common.DocumentEntity
import kaist.iclab.standup.smi.common.DocumentEntityClass
import kaist.iclab.standup.smi.common.Status
import kaist.iclab.standup.smi.common.asSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

class DocumentDataSource<T : DocumentEntity>(
    private val scope: CoroutineScope,
    private val context: CoroutineContext = EmptyCoroutineContext,
    private var query: Query?,
    private val onError: (suspend (Exception) -> Unit)? = null,
    private val entityClass: DocumentEntityClass<T>
) : PageKeyedDataSource<DocumentSnapshot, T>() {

    val initStatus: MutableLiveData<Status> = MutableLiveData(Status.init())
    val loadStatus: MutableLiveData<Status> = MutableLiveData(Status.init())

    private val retryOption: AtomicReference<(() -> Unit)?> = AtomicReference()

    override fun loadInitial(
        params: LoadInitialParams<DocumentSnapshot>,
        callback: LoadInitialCallback<DocumentSnapshot, T>
    ) {
        Log.d(javaClass.simpleName, "loadInitial(): query = $query")

        initStatus.postValue(Status.loading())

        scope.launch(context) {
            try {
                val snapshots = query?.limit(params.requestedLoadSize.toLong())?.get()
                    ?.asSuspend()?.documents?.filterNotNull() ?: listOf()
                val stats = snapshots.map { entityClass.fromDocumentSnapshot(it) }
                callback.onResult(stats, null, snapshots.lastOrNull())
                initStatus.postValue(Status.success())
            } catch (e: Exception) {
                retryOption.set { loadInitial(params, callback) }
                initStatus.postValue(Status.failure(e))
                onError?.invoke(e)
            }
        }
    }

    override fun loadAfter(
        params: LoadParams<DocumentSnapshot>,
        callback: LoadCallback<DocumentSnapshot, T>
    ) {
        Log.d(javaClass.simpleName, "loadAfter()")
        loadStatus.postValue(Status.loading())

        scope.launch(context) {
            try {
                val snapshots =
                    query?.startAfter(params.key)?.limit(params.requestedLoadSize.toLong())?.get()
                        ?.asSuspend()?.documents?.filterNotNull() ?: listOf()
                val stats = snapshots.map { entityClass.fromDocumentSnapshot(it) }
                callback.onResult(stats, snapshots.lastOrNull())
                loadStatus.postValue(Status.success())
            } catch (e: Exception) {
                retryOption.set { loadAfter(params, callback) }
                loadStatus.postValue(Status.failure(e))
                onError?.invoke(e)
            }
        }
    }

    fun retry() {
        scope.launch(context) {
            retryOption.getAndSet(null)?.invoke()
        }
    }

    override fun loadBefore(
        params: LoadParams<DocumentSnapshot>,
        callback: LoadCallback<DocumentSnapshot, T>
    ) {
    }

    class Factory<T : DocumentEntity>(
        private val scope: CoroutineScope,
        private val dispatcher: CoroutineContext,
        var query: Query?,
        private val onError: (suspend (Exception) -> Unit)?,
        private val entityClass: DocumentEntityClass<T>
    ) : DataSource.Factory<DocumentSnapshot, T>() {
        val sourceLiveData: MutableLiveData<DocumentDataSource<T>> = MutableLiveData()

        private var latestSource: DocumentDataSource<T>? = null

        fun updateQuery(newQuery: Query?) {
            query = newQuery
            latestSource?.invalidate()
        }

        fun retry() {
            latestSource?.retry()
        }

        fun refresh() {
            latestSource?.invalidate()
        }

        override fun create(): DataSource<DocumentSnapshot, T> {
            Log.d(javaClass.simpleName, "create()")
            val source = DocumentDataSource(
                scope,
                dispatcher,
                query,
                onError,
                entityClass
            )
            latestSource = source
            sourceLiveData.postValue(source)
            return source
        }
    }
}