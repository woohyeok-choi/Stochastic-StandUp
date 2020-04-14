package kaist.iclab.standup.smi.ui.timeline


import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fonfon.kgeohash.GeoHash
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentTimelinePlaceListBinding
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class TimelineChildPlaceListFragment : BaseFragment<FragmentTimelinePlaceListBinding, TimelineViewModel>() {
    override val viewModel: TimelineViewModel by sharedViewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline_place_list

    override fun beforeExecutePendingBindings() {
        val adapter = TimelinePlaceListAdapter()

        adapter.onClick = {
            val location = GeoHash(it.id).toLocation()
            (parentFragment as? TimelineNavigator)?.navigatePlaceClick(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }

        adapter.onLongClick = {
            val location = GeoHash(it.id).toLocation()
            (parentFragment as? TimelineNavigator)?.navigatePlaceLongClick(
                placeName = it.name,
                latitude = location.latitude,
                longitude = location.longitude
            )
        }

        adapter.onBind = {
            val location = GeoHash(it.id).toLocation()
            (parentFragment as? TimelineNavigator)?.navigateAddMarker(location.latitude, location.longitude)
        }

        viewModel.placeStats.observe(this) { stats ->
            stats?.let { adapter.submitList(it) }
        }

        dataBinding.listTimeline.adapter = adapter
        dataBinding.listTimeline.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))
    }
}