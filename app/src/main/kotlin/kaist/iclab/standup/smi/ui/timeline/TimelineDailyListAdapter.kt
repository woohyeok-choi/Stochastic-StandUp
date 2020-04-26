package kaist.iclab.standup.smi.ui.timeline

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.databinding.ItemDailyTimelineBinding
import kaist.iclab.standup.smi.databinding.ItemMissionBinding
import kaist.iclab.standup.smi.repository.SedentaryMissionEvent
import kaist.iclab.standup.smi.repository.sumIncentives

class TimelineDailyListAdapter : RecyclerView.Adapter<TimelineDailyListAdapter.ViewHolder>() {
    var items: List<SedentaryMissionEvent> = arrayListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var listener: OnTimelineItemListener? = null

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_daily_timeline,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        val latitude = item.event.latitude
        val longitude = item.event.longitude
        val name = item.place?.name ?: ""

        holder.bind(
            item = item,
            isFirst = position == 0,
            isLast = position == items.lastIndex,
            onClick = { listener?.onItemClick(name, latitude, longitude) },
            onLongClick = { listener?.onItemLongClick(name, latitude, longitude) }
        )

        listener?.onItemBind(name, latitude, longitude)
    }

    class ViewHolder(private val binding: ItemDailyTimelineBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: SedentaryMissionEvent,
            isFirst: Boolean, isLast: Boolean,
            onClick: (item: SedentaryMissionEvent) -> Unit,
            onLongClick: (item: SedentaryMissionEvent) -> Unit
        ) {
            binding.event = item
            binding.incentive = item.missions.sumIncentives()
            binding.isFirstElement = isFirst
            binding.isLastElement = isLast
            binding.isCollapsed = true

            binding.containerIncentive.setOnClickListener {
                val isCollapsed = binding.isCollapsed == true
                binding.isCollapsed = !isCollapsed
            }

            binding.containerPlace.setOnClickListener {
                onClick.invoke(item)
            }

            binding.containerPlace.setOnLongClickListener {
                onLongClick.invoke(item)
                true
            }

            binding.containerIncentiveDetail.removeAllViews()

            item.missions.sortedByDescending { it.triggerTime }.forEach { mission ->
                val subBinding: ItemMissionBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(itemView.context),
                    R.layout.item_mission,
                    binding.containerIncentiveDetail,
                    false
                )
                subBinding.mission = mission
                subBinding.isSubItem = true
                binding.containerIncentiveDetail.addView(subBinding.root)
            }
        }
    }

}