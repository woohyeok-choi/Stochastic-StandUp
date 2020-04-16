package kaist.iclab.standup.smi.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.data.PlaceStat
import kaist.iclab.standup.smi.databinding.ItemPlaceTimelineBinding

class TimelinePlaceListAdapter :
    PagedListAdapter<PlaceStat, TimelinePlaceListAdapter.ViewHolder>(DIFF_CALLBACK) {
    interface OnAdapterListener {
        fun onBind(item: PlaceStat)
        fun onClick(item: PlaceStat)
        fun onLongClick(item: PlaceStat)
    }

    var listener: OnAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemPlaceTimelineBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_place_timeline,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return

        holder.bind(
            item = item,
            onClick = { listener?.onClick(it) },
            onLongClick = { listener?.onLongClick(it) }
        )

        listener?.onBind(item)
    }

    class ViewHolder(private val binding: ItemPlaceTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: PlaceStat,
            onClick: (item: PlaceStat) -> Unit,
            onLongClick: (item: PlaceStat) -> Unit
        ) {
            binding.place = item

            binding.containerPlace.setOnClickListener {
                onClick.invoke(item)
            }

            binding.containerPlace.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<PlaceStat>() {
            override fun areItemsTheSame(oldItem: PlaceStat, newItem: PlaceStat): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: PlaceStat, newItem: PlaceStat): Boolean =
                oldItem == newItem
        }
    }
}