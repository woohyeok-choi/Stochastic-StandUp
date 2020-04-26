package kaist.iclab.standup.smi.ui.timeline

interface TimelineItemListener {
    fun onItemBind(latitude: Double, longitude: Double)
    fun onItemClick(latitude: Double, longitude: Double)
    fun onItemLongClick(latitude: Double, longitude: Double)
}