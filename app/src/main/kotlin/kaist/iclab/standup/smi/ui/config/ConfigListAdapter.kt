package kaist.iclab.standup.smi.ui.config

import android.view.InflateException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.databinding.ItemConfigHeaderBinding
import kaist.iclab.standup.smi.databinding.ItemConfigReadOnlyBinding
import kaist.iclab.standup.smi.databinding.ItemConfigReadWriteBinding
import kotlin.collections.ArrayList

class ConfigListAdapter(private val onItemClick: ((item: ConfigItem<*>?, position: Int) -> Unit)) : RecyclerView.Adapter<ConfigListAdapter.ViewHolder>() {
    var items: ArrayList<ConfigData> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemViewType(position: Int): Int {
        return when (items.getOrNull(position)) {
            is ConfigHeader -> VIEW_TYPE_HEADER
            is ReadOnlyConfigItem -> VIEW_TYPE_READ_ONLY
            is BooleanConfigItem -> VIEW_TYPE_BOOLEAN
            is NumberConfigItem -> VIEW_TYPE_NUMBER
            is NumberRangeConfigItem -> VIEW_TYPE_NUMBER_RANGE
            is LocalTimeConfigItem -> VIEW_TYPE_TIME
            is LocalTimeRangeConfigItem -> VIEW_TYPE_TIME_RANGE
            else -> 0x00
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when(viewType) {
            VIEW_TYPE_HEADER -> ConfigHeaderViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_config_header,
                    parent,
                    false
                )
            )
            VIEW_TYPE_READ_ONLY -> ReadOnlyConfigViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_config_read_only,
                    parent,
                    false
                ),
                onItemClick
            )
            else -> ReadWriteConfigViewHolder(
                DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_config_read_write,
                    parent,
                    false
                ),
                onItemClick,
                viewType
            )
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items.getOrNull(position)?.let { holder.bind(it, position) }
    }

    abstract class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun <T : ConfigData> bind(item : T, position: Int)
    }

    class ConfigHeaderViewHolder(
        private val binding: ItemConfigHeaderBinding
    ) : ViewHolder(binding.root) {
        override fun <T : ConfigData> bind(item: T, position: Int) {
            binding.item = item as? ConfigHeader
        }
    }

    class ReadOnlyConfigViewHolder(
        private val binding: ItemConfigReadOnlyBinding,
        private val onItemClick: ((item: ConfigItem<*>?, position: Int) -> Unit)
    ) : ViewHolder(binding.root) {
        override fun <T : ConfigData> bind(item: T, position: Int) {
            val i = item as? ReadOnlyConfigItem
            binding.item = i
            binding.root.setOnClickListener {
                onItemClick.invoke(i, position)
            }
        }
    }

    class ReadWriteConfigViewHolder(
        private val binding: ItemConfigReadWriteBinding,
        private val onItemClick: ((item: ConfigItem<*>?, position: Int) -> Unit),
        private val viewType: Int
    ) : ViewHolder(binding.root) {
        override fun <T : ConfigData> bind(item: T, position: Int) {
            when(viewType) {
                VIEW_TYPE_BOOLEAN -> {
                    val i = item as? BooleanConfigItem
                    binding.setVariable(BR.item, i)
                    binding.root.setOnClickListener {
                        onItemClick.invoke(i, position)
                    }
                }
                VIEW_TYPE_NUMBER -> {
                    val i = item as? NumberConfigItem
                    binding.setVariable(BR.item, i)
                    binding.root.setOnClickListener {
                        onItemClick.invoke(i, position)
                    }
                }
                VIEW_TYPE_NUMBER_RANGE -> {
                    val i = item as? NumberRangeConfigItem
                    binding.setVariable(BR.item, i)
                    binding.root.setOnClickListener {
                        onItemClick.invoke(i, position)
                    }
                }
                VIEW_TYPE_TIME -> {
                    val i = item as? LocalTimeConfigItem
                    binding.setVariable(BR.item, i)
                    binding.root.setOnClickListener {
                        onItemClick.invoke(i, position)
                    }
                }
                VIEW_TYPE_TIME_RANGE -> {
                    val i = item as? LocalTimeRangeConfigItem
                    binding.setVariable(BR.item, i)
                    binding.root.setOnClickListener {
                        onItemClick.invoke(i, position)
                    }
                }
            }


        }
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0x01
        private const val VIEW_TYPE_READ_ONLY = 0x02
        private const val VIEW_TYPE_BOOLEAN = 0x03
        private const val VIEW_TYPE_NUMBER = 0x04
        private const val VIEW_TYPE_NUMBER_RANGE = 0x05
        private const val VIEW_TYPE_TIME = 0x06
        private const val VIEW_TYPE_TIME_RANGE = 0x07
    }
}