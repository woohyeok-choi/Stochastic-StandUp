package kaist.iclab.standup.smi.ui.timeline

interface OnTimelineItemListener {
    fun onItemBind(name: String, latitude: Double, longitude: Double)
    fun onItemClick(name: String, latitude: Double, longitude: Double)
    fun onItemLongClick(name: String, latitude: Double, longitude: Double)
}