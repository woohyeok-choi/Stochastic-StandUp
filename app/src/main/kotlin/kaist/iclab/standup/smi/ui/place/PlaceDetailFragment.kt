package kaist.iclab.standup.smi.ui.place

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fonfon.kgeohash.toGeoHash
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.getMapAsSuspend
import kaist.iclab.standup.smi.databinding.FragmentPlaceBinding
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaceFragment : BaseFragment<FragmentPlaceBinding, PlaceViewModel>(), PlaceNavigator {
    override val viewModel: PlaceViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_place

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private var googleMap: GoogleMap? = null

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun beforeExecutePendingBindings() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        viewModel.navigator = this

        latitude = arguments?.getDouble(ARG_LATITUDE, Double.NaN).let {
            if (it == null || it.isNaN()) latitude else it
        }

        longitude = arguments?.getDouble(ARG_LONGITUDE, Double.NaN).let {
            if (it == null || it.isNaN()) longitude else it
        }

        bottomSheetBehavior = BottomSheetBehavior.from(dataBinding.containerMission).apply {
            setExpandedOffset(resources.getDimensionPixelSize(R.dimen.bottom_sheet_peek_height))
            isFitToContents = false
            halfExpandedRatio = 0.5F

            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }

                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    changeBottomSheetState(bottomSheet, newState)
                }
            })
        }

        val adapter = PlaceMissionListAdapter()

        dataBinding.listMission.adapter = adapter
        dataBinding.listMission.addItemDecoration(DividerItemDecoration(requireContext(), LinearLayoutManager.VERTICAL))

        viewModel.missions.observe(this) {
            it?.let { adapter.submitList(it) }
        }

        lifecycleScope.launch {
            changeFocus()
            addMarker()
        }
    }

    private fun changeBottomSheetState(bottomSheet: View, state: Int) = lifecycleScope.launch {
        if (state == BottomSheetBehavior.STATE_HALF_EXPANDED) {
            val newPadding = bottomSheet.height * 0.40F
            getMap()?.setPadding(0, 0, 0, newPadding.toInt())
        } else if (state == BottomSheetBehavior.STATE_COLLAPSED) {
            getMap()?.setPadding(0, 0, 0, 0)
        }
        changeFocus()
    }

    private suspend fun getMap(): GoogleMap? {
        if (googleMap == null) {
            googleMap =
                (childFragmentManager.findFragmentById(R.id.fragment_map) as? SupportMapFragment)?.getMapAsSuspend()
                    ?.apply {
                        isMyLocationEnabled = false
                        uiSettings.isZoomGesturesEnabled = false
                        uiSettings.isZoomControlsEnabled = false
                        uiSettings.isMyLocationButtonEnabled = false
                        uiSettings.isScrollGesturesEnabled = false
                        uiSettings.isTiltGesturesEnabled = false
                    }
        }
        return googleMap
    }

    private suspend fun addMarker() {
        getMap()?.addMarker(
            MarkerOptions().position(
                LatLng(latitude, longitude)
            ).icon(
                BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_selected)
            )
        )
    }

    private suspend fun changeFocus() {
        val camera = CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15F)
        getMap()?.animateCamera(camera)
    }

    override fun navigateError(throwable: Throwable?) {

    }

    companion object {
        private val PREFIX = PlaceFragment::javaClass.name

        private val ARG_LATITUDE = "$PREFIX.ARG_LATITUDE"
        private val ARG_LONGITUDE = "$PREFIX.ARG_LONGITUDE"

        fun newInstance(latitude: Double, longitude: Double) = PlaceFragment().apply {
            arguments = bundleOf(
                ARG_LATITUDE to latitude,
                ARG_LONGITUDE to longitude
            )
        }
    }
}