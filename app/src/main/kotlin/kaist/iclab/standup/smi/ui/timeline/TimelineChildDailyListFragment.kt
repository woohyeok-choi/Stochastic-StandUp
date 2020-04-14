package kaist.iclab.standup.smi.ui.timeline

import androidx.lifecycle.observe
import com.fonfon.kgeohash.GeoHash
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelineDailyListBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TimelineChildDailyListFragment : BaseFragment<FragmentTimelineDailyListBinding, TimelineViewModel>() {
    override val viewModel: TimelineViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline_daily_list

    override fun beforeExecutePendingBindings() {
        val adapter = TimelineDailyListAdapter()

        adapter.onClick = {
            val location = GeoHash(it.placeId).toLocation()
            (parentFragment as? TimelineNavigator)?.navigatePlaceClick(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }

        adapter.onLongClick = {
            val location = GeoHash(it.placeId).toLocation()
            (parentFragment as? TimelineNavigator)?.navigatePlaceLongClick(
                placeName = it.placeName ?: "",
                latitude = location.latitude,
                longitude = location.longitude
            )
        }

        adapter.onBind = {
            (parentFragment as? TimelineNavigator)?.navigateAddMarker(it.latitude, it.longitude)
        }

        viewModel.dailyStats.observe(this) { items ->
            items?.let {
                adapter.items = it
            }
        }

        dataBinding.listTimeline.adapter = adapter
    }
}