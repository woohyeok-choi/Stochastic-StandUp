package kaist.iclab.standup.smi.common


import com.google.firebase.firestore.*
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.common.throwError
import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor


@Suppress("UNCHECKED_CAST")
abstract class DataField<T : Any>(val name: String) {
    abstract fun fromSnapshot(value: DocumentSnapshot): T?

    operator fun getValue(thisRef: DocumentEntity, property: KProperty<*>): T? = thisRef.readValues[this] as? T

    operator fun setValue(thisRef: DocumentEntity, property: KProperty<*>, value: T?) {
        value?.let { thisRef.writeValues[this] = it }
    }
}

class IntField(name: String) : DataField<Int>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): Int? = value.getLong(name)?.toInt()
}

class LongField(name: String) : DataField<Long>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): Long? = value.getLong(name)
}

class FloatField(name: String) : DataField<Float>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): Float? = value.getDouble(name)?.toFloat()

}

class DoubleField(name: String) : DataField<Double>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): Double? = value.getDouble(name)
}

class StringField(name: String) : DataField<String>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): String? = value.getString(name)
}

class BooleanField(name: String) : DataField<Boolean>(name) {
    override fun fromSnapshot(value: DocumentSnapshot): Boolean? = value.getBoolean(name) ?: false
}

abstract class Documents(val name: String) {
    internal val fields = mutableListOf<DataField<*>>()

    private fun <T : Any> registerField(field: DataField<T>): DataField<T> {
        fields.add(field)
        return field
    }

    fun integer(name: String) = registerField(
        IntField(
            name
        )
    )
    fun long(name: String) = registerField(
        LongField(
            name
        )
    )
    fun float(name: String) = registerField(
        FloatField(
            name
        )
    )
    fun double(name: String) = registerField(
        DoubleField(
            name
        )
    )
    fun string(name: String) = registerField(
        StringField(
            name
        )
    )
    fun boolean(name: String) = registerField(
        BooleanField(
            name
        )
    )
}

abstract class DocumentEntity {
    internal val readValues: MutableMap<DataField<*>, Any> = mutableMapOf()
    internal val writeValues: MutableMap<DataField<*>, Any> = mutableMapOf()

    var id: String? = null
}


class QueryBuilder(private val ref: CollectionReference) {
    internal var query: Query? = null

    infix fun <T : Any> DataField<T>.greaterThan(value: T) {
        query = query?.whereGreaterThan(name, value) ?: ref.whereGreaterThan(name, value)
    }

    infix fun <T : Any> DataField<T>.greaterThanOrEqualTo(value: T) {
        query = query?.whereGreaterThanOrEqualTo(name, value) ?: ref.whereGreaterThanOrEqualTo(
            name,
            value
        )
    }

    infix fun <T : Any> DataField<T>.lessThan(value: T) {
        query = query?.whereLessThan(name, value) ?: ref.whereLessThan(name, value)
    }

    infix fun <T : Any> DataField<T>.lessThanOrEqualTo(value: T) {
        query = query?.whereLessThanOrEqualTo(name, value) ?: ref.whereLessThanOrEqualTo(name, value)
    }

    infix fun <T : Any> DataField<T>.equalTo(value: T?) {
        query = query?.whereEqualTo(name, value) ?: ref.whereEqualTo(name, value)
    }

    fun <T : Any> orderBy(field: DataField<T>, isAscending: Boolean = true) {
        query = query?.orderBy(field.name, if(isAscending) Query.Direction.ASCENDING else Query.Direction.DESCENDING)
    }

    fun limit(n: Long) {
        query = query?.limit(n)
    }
}

@Suppress("UNCHECKED_CAST")
abstract class DocumentEntityClass<T : DocumentEntity>(private val documents: Documents, type: Class<T>? = null) {
    private val klass : Class<*> = type ?: javaClass.enclosingClass as Class<T>
    private val constructor  = klass.kotlin.primaryConstructor!!
    private val name : String = documents.name

    private fun checkReference(ref: DocumentReference?) : DocumentReference = ref ?: throwError(R.string.error_no_document_reference)

    suspend fun create(ref: DocumentReference?, body: T.() -> Unit): String? {
        val instance = constructor.call() as T
        instance.body()
        val data = instance.writeValues.mapKeys { it.key.name }
        return checkReference(ref).collection(name).add(data).asSuspend()?.id
    }

    suspend fun update(ref: DocumentReference?, id: String, body: T.() -> Unit) {
        val instance = constructor.call() as T
        instance.body()
        val data = instance.writeValues.mapKeys { it.key.name }
        checkReference(ref).collection(name).document(id).set(data).asSuspend()
    }

    suspend fun get(ref: DocumentReference?, id: String) : T? {
        val data = checkReference(ref).collection(name).document(id).get().asSuspend() ?: return null
        val instance = constructor.call() as T
        instance.id = data.id

        documents.fields.forEach { field ->
            field.fromSnapshot(data)?.let { instance.readValues[field] = it }
        }
        return instance
    }

    suspend fun select(ref: DocumentReference?, body: QueryBuilder.() -> Unit) : List<T> {
        val builder = QueryBuilder(
            checkReference(ref).collection(name)
        )
        builder.body()
        val query = builder.query!!
        return query.get().asSuspend()?.documents?.map { snapshot ->
            val instance = constructor.call() as T
            instance.id = snapshot.id

            documents.fields.forEach { field ->
                field.fromSnapshot(snapshot)?.let { instance.readValues[field] = it }
            }
            instance
        } ?: listOf()
    }
}