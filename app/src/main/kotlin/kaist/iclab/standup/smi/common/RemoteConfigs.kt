package kaist.iclab.standup.smi.common

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import github.agustarc.koap.*
import github.agustarc.koap.delegator.ReadWritePreference
import java.util.concurrent.TimeUnit
import kotlin.reflect.KProperty


private val SERIALIZER: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

@Suppress("UNCHECKED_CAST")
open class FirebaseRemoteConfigHolder {
    internal suspend fun bind() {
        val instance = FirebaseRemoteConfig.getInstance()
        val setting = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(TimeUnit.HOURS.toMillis(1))
            .build()
        instance.setConfigSettingsAsync(setting).asSuspend()
        instance.fetchAndActivate().asSuspend()
    }

    internal fun <T> getPrimitiveValue(key: String, config: FirebaseRemoteConfig, default: T): T =
        when (default) {
            is String -> config.getString(key) as T
            is Long -> config.getLong(key) as T
            is Int -> config.getLong(key).toInt() as T
            is Float -> config.getDouble(key).toFloat() as T
            is Double -> config.getDouble(key) as T
            is Boolean -> config.getBoolean(key) as T
            else -> default
        }

    internal fun getString(key: String, config: FirebaseRemoteConfig): String = config.getString(key)
}

open class FirebaseRemoteConfigReadPrimitiveProperty<T>(
    private val key: String? = null,
    private val default: T
) {
    operator fun getValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>): T {
        return thisRef.getPrimitiveValue(
            if (key.isNullOrBlank()) property.name else key,
            FirebaseRemoteConfig.getInstance(),
            default
        )
    }
}

class FirebaseRemoteConfigReadSerializable<T>(
    private val key: String? = null,
    private val clazz: Class<T>
) {
    operator fun getValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>): T? {
        val value = thisRef.getString(if (key.isNullOrBlank()) property.name else key, FirebaseRemoteConfig.getInstance())
        return SERIALIZER.adapter(clazz).fromJson(value)
    }
}

class ReadWriteSerializable<T>(
    private val key: String? = null,
    private val clazz: Class<T>,
    default: T
) : ReadWritePreference<PreferenceHolder, T?>(default = default){
    override fun getValue(thisRef: PreferenceHolder, property: KProperty<*>): T? {
        val key = if (key.isNullOrBlank()) property.name else key
        return if (Koap.isTestMode || cacheUsable()) {
            field
        } else {
            if (thisRef.hasKey(key, property)) {
                val json = thisRef.getString(key, property, "")
                field = SERIALIZER.adapter(clazz).fromJson(json)
                cacheLoaded = true
                field
            } else {
                field
            }
        }
    }

    override fun setValue(thisRef: PreferenceHolder, property: KProperty<*>, value: T?) {
        if (field == value) return
        val key = if (key.isNullOrBlank()) property.name else key
        val json = SERIALIZER.adapter(clazz).toJson(value)
        thisRef.putString(key, property, json)
        field = value
    }

    override fun setCacheValue(thisRef: PreferenceHolder, property: KProperty<*>) {
        val key = if (key.isNullOrBlank()) property.name else key
        if (thisRef.hasKey(key, property)) {
            val json = thisRef.getString(key, property, "")
            field = SERIALIZER.adapter(clazz).fromJson(json)
        }
    }
}

class FirebaseRemoteConfigReadString(key: String? = null, default: String) :
    FirebaseRemoteConfigReadPrimitiveProperty<String>(key, default)

class FirebaseRemoteConfigReadLong(key: String? = null, default: Long) :
    FirebaseRemoteConfigReadPrimitiveProperty<Long>(key, default)

class FirebaseRemoteConfigReadInt(key: String? = null, default: Int) :
    FirebaseRemoteConfigReadPrimitiveProperty<Int>(key, default)

class FirebaseRemoteConfigReadFloat(key: String? = null, default: Float) :
    FirebaseRemoteConfigReadPrimitiveProperty<Float>(key, default)

class FirebaseRemoteConfigReadDouble(key: String? = null, default: Double) :
    FirebaseRemoteConfigReadPrimitiveProperty<Double>(key, default)

class FirebaseRemoteConfigReadBoolean(key: String? = null, default: Boolean) :
    FirebaseRemoteConfigReadPrimitiveProperty<Boolean>(key, default)