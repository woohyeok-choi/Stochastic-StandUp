package kaist.iclab.standup.smi.ui.timeline

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.os.bundleOf
import androidx.core.view.iterator
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.fonfon.kgeohash.toGeoHash
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.SupportMapFragment
import com.google.android.libraries.maps.model.BitmapDescriptorFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.getMapAsSuspend
import kaist.iclab.standup.smi.common.snackBar
import kaist.iclab.standup.smi.common.wrap
import kaist.iclab.standup.smi.data.PlaceStat
import kaist.iclab.standup.smi.databinding.FragmentTimelineBinding
import kaist.iclab.standup.smi.repository.SedentaryMissionEvent
import kaist.iclab.standup.smi.tracker.LocationTracker
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class TimelineFragment : BaseFragment<FragmentTimelineBinding, TimelineViewModel>(),
    TimelineNavigator, TimelinePlaceOrderDialogFragment.OnPlaceOrderChangedListener,
    TimelinePlaceRenameDialogFragment.OnTextChangedListener, TimelinePlaceListAdapter.OnAdapterListener,
    TimelineDailyListAdapter.OnAdapterListener {
    override val viewModel: TimelineViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FragmentContainerView>
    private var googleMap: GoogleMap? = null

    private val locationTracker: LocationTracker by inject()
    private val timeZone = DateTimeZone.getDefault()
    private val nowDate = DateTime(
        if (BuildConfig.FIREBASE_TEST_MODE) 1540605842591 else System.currentTimeMillis(),
        timeZone
    ).withTimeAtStartOfDay()

    private val markers: MutableMap<Pair<Double, Double>, Marker> = mutableMapOf()

    private var lastFocusLocation: Pair<Double, Double> = 0.0 to 0.0

    override fun beforeExecutePendingBindings() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        viewModel.navigator = this

        bottomSheetBehavior = BottomSheetBehavior.from(dataBinding.containerTimeline).apply {
            setExpandedOffset(resources.getDimensionPixelSize(R.dimen.small_action_bar_size_double))
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

        dataBinding.root.setOnTouchListener { _, _ ->
            val isExpanded = dataBinding.isExpanded == true && dataBinding.isDailyMode == true
            if (isExpanded) toggleCalendar()
            isExpanded
        }

        dataBinding.containerToolbarTitle.setOnClickListener {
            if (dataBinding.isDailyMode == true) toggleCalendar()
        }

        dataBinding.calendarView.apply {
            currentDate = nowDate.toDate()
            shouldSelectFirstDayOfMonthOnScroll(false)
            setLocale(TimeZone.getDefault(), Locale.getDefault())
            setListener(object : CompactCalendarView.CompactCalendarViewListener {
                override fun onDayClick(dateClicked: Date?) {
                    dateClicked?.let { changeDate(it) }
                }

                override fun onMonthScroll(firstDayOfNewMonth: Date?) {
                    firstDayOfNewMonth?.time?.let { dataBinding.monthMillis = it }
                }
            })
        }

        dataBinding.containerTabs.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabSelected(tab: TabLayout.Tab?) {
                changeTab(tab?.position == 0)
            }
        })

        dataBinding.containerTimeline.setPadding(
            0,
            0,
            0,
            resources.getDimensionPixelSize(R.dimen.small_action_bar_size_double)
        )

        dataBinding.isExpanded = false
        dataBinding.isDailyMode = true
        dataBinding.monthMillis = nowDate.millis

        dataBinding.containerTabs.selectTab(null, false)
        dataBinding.containerTabs.selectTab(dataBinding.containerTabs.getTabAt(0))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.place_order, menu)

        menu.iterator().forEach { item ->
            item.icon?.let {
                DrawableCompat.setTint(it, resources.getColor(R.color.light_grey, null))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_place_order) return super.onOptionsItemSelected(item)

        val dialog = TimelinePlaceOrderDialogFragment.newInstance(
            isDescending = viewModel.currentOrderDescendingDirection,
            field = viewModel.currentOrderField
        )

        dialog.setTargetFragment(this, 0)
        dialog.show(parentFragmentManager, javaClass.name)

        return true
    }

    override fun onTextChanged(prevText: String, curText: String, extra: Bundle?) {
        val latitude = extra?.getDouble(ARG_PLACE_LATITUDE, Double.NaN) ?: Double.NaN
        val longitude = extra?.getDouble(ARG_PLACE_LONGITUDE, Double.NaN) ?: Double.NaN

        if (latitude.isNaN() || longitude.isNaN()) return

        lifecycleScope.launch {
            viewModel.renamePlace(
                latitude = latitude,
                longitude = longitude,
                newName = curText
            )

            clearMap()

            if (dataBinding.isDailyMode == true) {
                viewModel.refreshDailyStat().join()
            } else{
                viewModel.refreshPlaceStats()
            }
        }
    }

    override fun onBind(item: PlaceStat) {
        val location = item.id.toGeoHash().toLocation()

        lifecycleScope.launch {
            addMarker(
                latitude = location.latitude,
                longitude = location.longitude
            )
        }
    }

    override fun onClick(item: PlaceStat) {
        val location = item.id.toGeoHash().toLocation()

        lifecycleScope.launch {
            selectPlace(latitude = location.latitude, longitude = location.longitude)
        }
    }

    override fun onLongClick(item: PlaceStat) {
        val location = item.id.toGeoHash().toLocation()

        lifecycleScope.launch {
            renamePlace(placeName = item.name, latitude = location.latitude, longitude = location.longitude)
        }
    }

    override fun onBind(item: SedentaryMissionEvent) {
        lifecycleScope.launch {
            addMarker(
                latitude = item.latitude,
                longitude = item.longitude
            )
        }
    }

    override fun onClick(item: SedentaryMissionEvent) {
        lifecycleScope.launch {
            selectPlace(latitude = item.latitude, longitude = item.longitude)
        }
    }

    override fun onLongClick(item: SedentaryMissionEvent) {
        lifecycleScope.launch {
            renamePlace(placeName = item.placeName ?: "", latitude = item.latitude, longitude = item.longitude)
        }
    }

    override fun navigateDailyStatError(throwable: Throwable?) {
        snackBar(
            view = dataBinding.root,
            anchorId = R.id.nav_bottom,
            isShort = false,
            msg = throwable.wrap().toString(this),
            actionName = getString(R.string.general_retry)
        ) {
            viewModel.refreshDailyStat()
        }
    }

    override fun navigatePlaceStatError(throwable: Throwable?) {
        snackBar(
            view = dataBinding.root,
            anchorId = R.id.nav_bottom,
            isShort = false,
            msg = throwable.wrap().toString(this),
            actionName = getString(R.string.general_retry)
        ) {
            viewModel.retryToLoadPlaceStats()
        }
    }

    override fun navigatePlaceRenameError(throwable: Throwable?) {
        snackBar(
            view = dataBinding.root,
            anchorId = R.id.nav_bottom,
            isShort = false,
            msg = throwable.wrap().toString(this)
        )
    }

    private suspend fun selectPlace(latitude: Double, longitude: Double) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        changeFocus(latitude, longitude)
    }

    private suspend fun renamePlace(
        placeName: String,
        latitude: Double,
        longitude: Double
    ) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        changeFocus(latitude, longitude)

        val dialog = TimelinePlaceRenameDialogFragment.newInstance(
            name = placeName,
            extra = bundleOf(
                ARG_PLACE_LATITUDE to latitude,
                ARG_PLACE_LONGITUDE to longitude
            )
        )
        dialog.setTargetFragment(this@TimelineFragment, 0)
        dialog.show(parentFragmentManager, javaClass.name)
    }

    private fun toggleCalendar() {
        val isExpanded = dataBinding.isExpanded == true
        val isAnimating = dataBinding.calendarView.isAnimating

        if (isAnimating) return

        if (isExpanded) {
            dataBinding.calendarView.hideCalendarWithAnimation()
        } else {
            dataBinding.calendarView.currentDate?.let { prevDate ->
                dataBinding.monthMillis = prevDate.time
                dataBinding.calendarView.currentDate = prevDate
            }
            dataBinding.calendarView.showCalendarWithAnimation()
        }

        dataBinding.isExpanded = !isExpanded
    }

    override fun onOrderChanged(isDescending: Boolean, field: Int) {
        lifecycleScope.launch {
            clearMap()

            viewModel.loadPlaceStats(
                isDescending = isDescending,
                field = field
            )
        }
    }

    private fun changeDate(date: Date) = lifecycleScope.launch {
        clearMap()

        dataBinding.calendarView.currentDate = date
        viewModel.loadDailyStat(DateTime(date.time, timeZone))
        toggleCalendar()
    }

    private fun changeTab(isDailyMode: Boolean) = lifecycleScope.launch {
        clearMap()

        if (isDailyMode) {
            childFragmentManager.commit {
                replace(R.id.container_timeline, TimelineChildDailyListFragment())
            }
            val dateTime = dataBinding.calendarView.currentDate?.let {
                DateTime(it.time, timeZone)
            } ?: nowDate
            setHasOptionsMenu(false)

            viewModel.loadDailyStat(dateTime)
        } else {
            childFragmentManager.commit {
                replace(R.id.container_timeline, TimelineChildPlaceListFragment())
            }
            setHasOptionsMenu(true)

            viewModel.loadPlaceStats()
        }

        dataBinding.isDailyMode = isDailyMode

        locationTracker.getLastLocation()?.let {
            changeFocus(
                it.latitude,
                it.longitude,
                if (isDailyMode) DEFAULT_ZOOM_LEVEL_DAILY_MODE else DEFAULT_ZOOM_LEVEL_PLACE_MODE
            )
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
                        isMyLocationEnabled = true
                        uiSettings.isZoomControlsEnabled = true
                        uiSettings.isZoomGesturesEnabled = true
                    }
        }
        return googleMap
    }

    private suspend fun addMarker(latitude: Double, longitude: Double) {
        val marker = getMap()?.addMarker(
            MarkerOptions().position(
                LatLng(latitude, longitude)
            ).icon(
                BitmapDescriptorFactory.fromResource(R.drawable.marker)
            )
        ) ?: return
        markers[(latitude to longitude)] = marker
    }

    private suspend fun clearMap() {
        getMap()?.clear()
        markers.clear()
    }

    private suspend fun changeFocus(
        lat: Double? = null,
        lng: Double? = null,
        zoomLevel: Float? = null
    ) {
        if (lat != null && lng != null) {
            lastFocusLocation = lat to lng
        }
        val (latitude, longitude) = lastFocusLocation

        val camera = if (zoomLevel == null) {
            CameraUpdateFactory.newLatLng(LatLng(latitude, longitude))
        } else {
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel)
        }

        markers.keys.forEach { (curLat, curLng) ->
            val isSame = curLat == latitude && curLng == longitude
            val icon = if (isSame) {
                BitmapDescriptorFactory.fromResource(R.drawable.marker_selected)
            } else {
                BitmapDescriptorFactory.fromResource(R.drawable.marker)
            }
            val zIndex = if (isSame) {
                Float.MAX_VALUE
            } else {
                0F
            }
            markers[(curLat to curLng)]?.setIcon(icon)
            markers[(curLat to curLng)]?.zIndex = zIndex
        }

        getMap()?.animateCamera(camera)
    }

    companion object {
        private val PREFIX = TimelineFragment::class.java.name
        private const val DEFAULT_ZOOM_LEVEL_DAILY_MODE = 15F
        private const val DEFAULT_ZOOM_LEVEL_PLACE_MODE = 12F

        private val ARG_PLACE_LATITUDE = "$PREFIX.ARG_PLACE_LATITUDE"
        private val ARG_PLACE_LONGITUDE = "$PREFIX.ARG_PLACE_LONGITUDE"

        const val EXTRA_FIELD_VISIT_TIME = 0x00
        const val EXTRA_FIELD_DURATION = 0x01
        const val EXTRA_FIELD_INCENTIVE = 0x02
        const val EXTRA_FIELD_MISSIONS = 0x03
        const val EXTRA_FIELD_VISITS = 0x04
    }
}