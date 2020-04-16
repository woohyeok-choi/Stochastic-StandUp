package kaist.iclab.standup.smi.common

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import github.agustarc.koap.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KProperty

private val SERIALIZER: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

@Suppress("UNCHECKED_CAST")
open class FirebaseRemoteConfigHolder(name: String,
                                      default: Boolean = false,
                                      mode: Int = Context.MODE_PRIVATE,
                                      cacheStrategy: CacheStrategy = CacheStrategy.LAZY
): PreferenceHolder(name, default, mode, cacheStrategy) {
    private var mLocalMode: AtomicBoolean = AtomicBoolean(false)

    var localMode: Boolean
        get() = mLocalMode.get()
        set(value) { mLocalMode.set(value) }

    suspend fun sync() {
        val instance = FirebaseRemoteConfig.getInstance()
        val setting = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(TimeUnit.HOURS.toMillis(1))
            .build()
        instance.setConfigSettingsAsync(setting).asSuspend()
        instance.fetchAndActivate().asSuspend()
    }

    fun <T> getValue(key: String, property: KProperty<*>, default: T): T {
        return if (localMode) {
            getPreferencePrimitiveValue(key, property, default)
        } else {
            val config = FirebaseRemoteConfig.getInstance()
            when (default) {
                is String -> config.getString(key) as T
                is Long -> config.getLong(key) as T
                is Int -> config.getLong(key).toInt() as T
                is Float -> config.getDouble(key).toFloat() as T
                is Boolean -> config.getBoolean(key) as T
                else -> default
            }
        }
    }

    fun <T> putValue(key: String, property: KProperty<*>, value: T) {
        if (localMode) {
            putPreferencePrimitiveValue(key, property, value)
        }
    }

    fun getStringValue(key: String, property: KProperty<*>, default: String): String {
        return if (localMode) {
            getString(key, property, default)
        } else {
            FirebaseRemoteConfig.getInstance().getString(key)
        }
    }
}


open class FirebaseRemoteConfigProperty<T>(
    private val key: String? = null,
    private val default: T
) {
    operator fun getValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>): T {
        return thisRef.getValue(
            if (key.isNullOrBlank()) property.name else key, property, default
        )
    }

    operator fun setValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>, value: T) {
        thisRef.putValue(if(key.isNullOrBlank()) property.name else key, property, value)
    }
}

class FirebaseRemoteConfigString(key: String? = null, default: String) :
    FirebaseRemoteConfigProperty<String>(key, default)

class FirebaseRemoteConfigLong(key: String? = null, default: Long) :
    FirebaseRemoteConfigProperty<Long>(key, default)

class FirebaseRemoteConfigInt(key: String? = null, default: Int) :
    FirebaseRemoteConfigProperty<Int>(key, default)

class FirebaseRemoteConfigFloat(key: String? = null, default: Float) :
    FirebaseRemoteConfigProperty<Float>(key, default)

class FirebaseRemoteConfigBoolean(key: String? = null, default: Boolean) :
    FirebaseRemoteConfigProperty<Boolean>(key, default)


class FirebaseRemoteConfigSerializable<T>(
    private val key: String? = null,
    private val clazz: Class<T>
) {
    operator fun getValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>): T? {
        val value = thisRef.getString(if (key.isNullOrBlank()) property.name else key, property, "")
        return SERIALIZER.adapter(clazz).fromJson(value)
    }

    operator fun setValue(thisRef: PreferenceHolder, property: KProperty<*>, value: T?) {
        val key = if (key.isNullOrBlank()) property.name else key
        val json = SERIALIZER.adapter(clazz).toJson(value)
        thisRef.putString(key, property, json)
    }
}

class FirebaseRemoteConfigList<T>(
    private val key: String? = null,
    private val clazz: Class<T>,
    private val default: List<T> = listOf()
) {
    operator fun getValue(thisRef: FirebaseRemoteConfigHolder, property: KProperty<*>): List<T> {
        val value = thisRef.getString(if (key.isNullOrBlank()) property.name else key, property, "")
        val type = Types.newParameterizedType(List::class.java, clazz)
        return SERIALIZER.adapter<List<T>>(type).fromJson(value) ?: default
    }

    operator fun setValue(thisRef: PreferenceHolder, property: KProperty<*>, value: List<T>?) {
        val key = if (key.isNullOrBlank()) property.name else key
        val type = Types.newParameterizedType(List::class.java, clazz)
        val json = SERIALIZER.adapter<List<T>>(type).toJson(value)
        thisRef.putString(key, property, json)
    }
}