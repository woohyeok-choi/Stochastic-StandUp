package kaist.iclab.standup.smi.ui.place

import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.doMapOperation
import kaist.iclab.standup.smi.common.snackBar
import kaist.iclab.standup.smi.common.wrap
import kaist.iclab.standup.smi.databinding.FragmentPlaceBinding
import kotlinx.android.synthetic.main.fragment_place.*
import org.koin.androidx.viewmodel.ext.android.viewModel

class PlaceDetailFragment : BaseFragment<FragmentPlaceBinding, PlaceDetailViewModel>(),
    PlaceDetailNavigator {
    override val viewModel: PlaceDetailViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_place

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private val args: PlaceDetailFragmentArgs by navArgs()

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    override fun beforeExecutePendingBindings() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewModel.navigator = this

        latitude = args.location.latitude
        longitude = args.location.longitude

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

        val adapter = PlaceDetailMissionListAdapter()

        dataBinding.listMission.adapter = adapter
        dataBinding.listMission.addItemDecoration(
            DividerItemDecoration(
                requireContext(),
                LinearLayoutManager.VERTICAL
            )
        )

        viewModel.missions.observe(this) {
            it?.let { adapter.submitList(it) }
        }

        doMapOperation { map ->
            map.addMarker(
                MarkerOptions().position(
                    LatLng(latitude, longitude)
                ).icon(
                    BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_selected)
                )
            )
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15F)
            )
        }
        viewModel.loadData(latitude, longitude)
    }

    private fun changeBottomSheetState(bottomSheet: View, state: Int) {
        val padding = when (state) {
            BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomSheet.height * 0.40F
            BottomSheetBehavior.STATE_COLLAPSED -> 0F
            else -> null
        }

        doMapOperation { map ->
            if (padding != null) map.setPadding(0, 0, 0, padding.toInt())
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), 15F)
            )
        }
    }

    private fun doMapOperation(op: (GoogleMap) -> Unit) {
        (childFragmentManager.findFragmentById(R.id.fragment_map) as? SupportMapFragment)?.doMapOperation { map ->
            if (map.isMyLocationEnabled) map.isMyLocationEnabled = false
            if (!map.uiSettings.isZoomControlsEnabled) map.uiSettings.isZoomControlsEnabled = true
            if (!map.uiSettings.isZoomGesturesEnabled) map.uiSettings.isZoomGesturesEnabled = true
            if (map.uiSettings.isMyLocationButtonEnabled) map.uiSettings.isMyLocationButtonEnabled =
                false
            if (!map.uiSettings.isScrollGesturesEnabled) map.uiSettings.isZoomGesturesEnabled = true
            if (map.uiSettings.isTiltGesturesEnabled) map.uiSettings.isZoomGesturesEnabled = false
            op.invoke(map)
        }
    }

    override fun navigateError(throwable: Throwable?) {
        snackBar(
            view = dataBinding.root,
            anchorId = R.id.nav_bottom,
            isShort = false,
            msg = throwable.wrap().toString(this),
            actionName = getString(R.string.general_retry)
        ) {
            viewModel.loadData(latitude, longitude)
        }
    }
}