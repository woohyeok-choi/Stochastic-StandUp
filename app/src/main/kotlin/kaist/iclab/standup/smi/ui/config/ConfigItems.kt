package kaist.iclab.standup.smi.ui.config

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable
import java.util.concurrent.TimeUnit


class Config {
    val items: ArrayList<ConfigData> = arrayListOf()

    fun header(init: ConfigHeader.() -> Unit) : ConfigHeader{
        val h = ConfigHeader()
        h.init()
        items.add(h)
        items.addAll(h.items)
        return h
    }
}

fun config(init: Config.() -> Unit): Config {
    val c = Config()
    c.init()
    return c
}

open class ConfigData(
    open var id: String,
    open var title: String
)

data class ConfigHeader(
    override var id: String = "",
    override var title: String = ""
) : ConfigData(id, title) {
    val items: ArrayList<ConfigItem<*>> = arrayListOf()

    private fun <T : ConfigItem<*>> add(item: T, init: T.() -> Unit) : T {
        item.init()
        items.add(item)
        return item
    }

    fun readOnly(init: ReadOnlyConfigItem.() -> Unit) = add(ReadOnlyConfigItem(), init)

    fun boolean(init: BooleanConfigItem.() -> Unit) = add(BooleanConfigItem(), init)

    fun number(init: NumberConfigItem.() -> Unit) = add(NumberConfigItem(), init)

    fun numberRange(init: NumberRangeConfigItem.() -> Unit) = add(NumberRangeConfigItem(), init)

    fun localTime(init: LocalTimeConfigItem.() -> Unit) = add(LocalTimeConfigItem(), init)

    fun localTimeRange(init: LocalTimeRangeConfigItem.() -> Unit) = add(LocalTimeRangeConfigItem(), init)
}

open class ConfigItem<T>(
    override var id: String,
    override var title: String,
    open var formatter: ((T) -> String)? = null
) : ConfigData(id, title)

@Suppress("UNCHECKED_CAST")
data class ReadOnlyConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var formatter: ((Unit) -> String)? = null,
    var onAction: (() -> Intent)? = null
) : ConfigItem<Unit>(id, title, formatter), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? ((Unit) -> String)?,
        parcel.readSerializable() as? (() -> Intent)?
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(onAction as? Serializable)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReadOnlyConfigItem> {
        override fun createFromParcel(parcel: Parcel): ReadOnlyConfigItem {
            return ReadOnlyConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<ReadOnlyConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}

open class ReadWriteConfigItem<T : Serializable>(
    override var id: String = "",
    override var title: String = "",
    override var formatter: ((T) -> String)? = null,
    open var value: (() -> T)? = null,
    open var isSavable: ((newValue: T) -> Boolean)? = null,
    open var onSave: ((newValue: T) -> Unit)? = null
) : ConfigItem<T>(id, title, formatter)

@Suppress("UNCHECKED_CAST")
data class BooleanConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Boolean)? = null,
    override var formatter: ((Boolean) -> String)? = null,
    override var isSavable: ((newValue: Boolean) -> Boolean)? = null,
    override var onSave: ((newValue: Boolean) -> Unit)? = null
) : ReadWriteConfigItem<Boolean>(id, title, formatter, value, isSavable, onSave), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Boolean),
        parcel.readSerializable() as? ((Boolean) -> String),
        parcel.readSerializable() as? ((Boolean) -> Boolean),
        parcel.readSerializable() as? ((Boolean) -> Unit)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<BooleanConfigItem> {
        override fun createFromParcel(parcel: Parcel): BooleanConfigItem {
            return BooleanConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<BooleanConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}

@Suppress("UNCHECKED_CAST")
data class NumberConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Long)? = null,
    override var formatter: ((Long) -> String)? = null,
    override var isSavable: ((newValue: Long) -> Boolean)? = null,
    override var onSave: ((newValue: Long) -> Unit)? = null,
    var min: Long = 0,
    var max: Long = 100
) : ReadWriteConfigItem<Long>(id, title, formatter, value, isSavable, onSave), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Long),
        parcel.readSerializable() as? ((Long) -> String),
        parcel.readSerializable() as? ((Long) -> Boolean),
        parcel.readSerializable() as? ((Long) -> Unit),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
        parcel.writeLong(min)
        parcel.writeLong(max)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NumberConfigItem> {
        override fun createFromParcel(parcel: Parcel): NumberConfigItem {
            return NumberConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<NumberConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}

@Suppress("UNCHECKED_CAST")
data class NumberRangeConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Pair<Long, Long>)? = null,
    override var formatter: ((Pair<Long, Long>) -> String)? = null,
    override var isSavable: ((newValue: Pair<Long, Long>) -> Boolean)? = null,
    override var onSave: ((newValue: Pair<Long, Long>) -> Unit)? = null,
    var min: Long = 0,
    var max: Long = 100
) : ReadWriteConfigItem<Pair<Long, Long>>(id, title, formatter, value, isSavable, onSave),
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Pair<Long, Long>),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> String),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> Boolean),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> Unit),
        parcel.readLong(),
        parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
        parcel.writeLong(min)
        parcel.writeLong(max)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<NumberRangeConfigItem> {
        override fun createFromParcel(parcel: Parcel): NumberRangeConfigItem {
            return NumberRangeConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<NumberRangeConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}

@Suppress("UNCHECKED_CAST")
data class LocalTimeConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Long)? = null,
    override var formatter: ((Long) -> String)? = null,
    override var isSavable: ((newValue: Long) -> Boolean)? = null,
    override var onSave: ((newValue: Long) -> Unit)? = null
) : ReadWriteConfigItem<Long>(id, title, formatter, value, isSavable, onSave), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Long),
        parcel.readSerializable() as? ((Long) -> String),
        parcel.readSerializable() as? ((Long) -> Boolean),
        parcel.readSerializable() as? ((Long) -> Unit)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocalTimeConfigItem> {
        override fun createFromParcel(parcel: Parcel): LocalTimeConfigItem {
            return LocalTimeConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<LocalTimeConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}

@Suppress("UNCHECKED_CAST")
data class LocalTimeRangeConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Pair<Long, Long>)? = null,
    override var formatter: ((Pair<Long, Long>) -> String)? = null,
    override var isSavable: ((newValue: Pair<Long, Long>) -> Boolean)? = null,
    override var onSave: ((newValue: Pair<Long, Long>) -> Unit)? = null
) : ReadWriteConfigItem<Pair<Long, Long>>(id, title, formatter, value, isSavable, onSave),
    Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Pair<Long, Long>),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> String),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> Boolean),
        parcel.readSerializable() as? ((Pair<Long, Long>) -> Unit)

    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LocalTimeRangeConfigItem> {
        override fun createFromParcel(parcel: Parcel): LocalTimeRangeConfigItem {
            return LocalTimeRangeConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<LocalTimeRangeConfigItem?> {
            return arrayOfNulls(size)
        }
    }
}


