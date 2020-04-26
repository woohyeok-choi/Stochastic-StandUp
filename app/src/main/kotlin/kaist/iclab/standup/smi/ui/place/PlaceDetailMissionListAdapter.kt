package kaist.iclab.standup.smi.ui.place

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.data.Mission
import kaist.iclab.standup.smi.databinding.ItemMissionBinding

class PlaceMissionListAdapter: PagedListAdapter<Mission, PlaceMissionListAdapter.ViewHolder>(
    DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ItemMissionBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_mission,
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        holder.bind(item)
    }

    class ViewHolder(private val binding: ItemMissionBinding) : RecyclerView.ViewHolder (binding.root){
        fun bind(item: Mission) {
            binding.mission = item
            binding.isSubItem = false
        }
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Mission>() {
            override fun areItemsTheSame(oldItem: Mission, newItem: Mission): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Mission, newItem: Mission): Boolean = oldItem == newItem
        }
    }


}