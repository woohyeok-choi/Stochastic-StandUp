package kaist.iclab.standup.smi.ui.config

import android.content.Intent
import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable


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

    fun choice(init: SingleChoiceConfigItem.() -> Unit) = add(SingleChoiceConfigItem(), init)

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
data class SingleChoiceConfigItem(
    override var id: String = "",
    override var title: String = "",
    override var value: (() -> Int)? = null,
    override var formatter: ((Int) -> String)? = null,
    override var isSavable: ((newValue: Int) -> Boolean)? = null,
    override var onSave: ((newValue: Int) -> Unit)? = null,
    var valueFormatter: ((Int) -> String)? = null,
    var options: IntArray = intArrayOf()
) :ReadWriteConfigItem<Int>(id, title, formatter, value, isSavable, onSave), Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readSerializable() as? (() -> Int),
        parcel.readSerializable() as? ((Int) -> String),
        parcel.readSerializable() as? ((Int) -> Boolean),
        parcel.readSerializable() as? ((Int) -> Unit),
        parcel.readSerializable() as? ((Int) -> String),
        intArrayOf().apply { parcel.readIntArray(this) }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(title)
        parcel.writeSerializable(value as? Serializable)
        parcel.writeSerializable(formatter as? Serializable)
        parcel.writeSerializable(isSavable as? Serializable)
        parcel.writeSerializable(onSave as? Serializable)
        parcel.writeSerializable(valueFormatter as? Serializable)
        parcel.writeIntArray(options)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SingleChoiceConfigItem

        if (id != other.id) return false
        if (title != other.title) return false
        if (value != other.value) return false
        if (formatter != other.formatter) return false
        if (isSavable != other.isSavable) return false
        if (onSave != other.onSave) return false
        if (valueFormatter != other.valueFormatter) return false
        if (!options.contentEquals(other.options)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + (value?.hashCode() ?: 0)
        result = 31 * result + (formatter?.hashCode() ?: 0)
        result = 31 * result + (isSavable?.hashCode() ?: 0)
        result = 31 * result + (onSave?.hashCode() ?: 0)
        result = 31 * result + (valueFormatter?.hashCode() ?: 0)
        result = 31 * result + options.contentHashCode()
        return result
    }

    companion object CREATOR : Parcelable.Creator<SingleChoiceConfigItem> {
        override fun createFromParcel(parcel: Parcel): SingleChoiceConfigItem {
            return SingleChoiceConfigItem(parcel)
        }

        override fun newArray(size: Int): Array<SingleChoiceConfigItem?> {
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
    var valueFormatter: ((Long) -> String)? = null,
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
        parcel.readSerializable() as? ((Long) -> String),
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
        parcel.writeSerializable(valueFormatter as? Serializable)
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
    var valueFormatter: ((Long) -> String)? = null,
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
        parcel.readSerializable() as? ((Long) -> String),
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
        parcel.writeSerializable(valueFormatter as? Serializable)
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


