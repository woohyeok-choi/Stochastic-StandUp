package kaist.iclab.standup.smi.ui.timeline

import android.location.Location
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.forEach
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.github.sundeepk.compactcalendarview.CompactCalendarView
import com.google.android.libraries.maps.CameraUpdate
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
import kaist.iclab.standup.smi.common.*
import kaist.iclab.standup.smi.databinding.FragmentTimelineBinding
import kaist.iclab.standup.smi.tracker.LocationTracker
import kaist.iclab.standup.smi.ui.dialog.SingleChoiceDialogFragment
import kotlinx.android.synthetic.main.fragment_timeline.*
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*


class TimelineFragment : BaseFragment<FragmentTimelineBinding, TimelineViewModel>(),
    TimelineNavigator, OnTimelineItemListener {
    override val viewModel: TimelineViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_timeline

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FragmentContainerView>

    private val locationTracker: LocationTracker by inject()
    private val timeZone = DateTimeZone.getDefault()
    private val nowDate = DateTime(
        if (BuildConfig.FIREBASE_TEST_MODE) 1540605842591 else System.currentTimeMillis(),
        timeZone
    ).withTimeAtStartOfDay()

    private val markers: MutableMap<Pair<Double, Double>, Marker> = mutableMapOf()

    private var lastFocusedLocation: Pair<Double, Double> = 0.0 to 0.0

    override fun beforeExecutePendingBindings() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
        }

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
            val isExpanded = dataBinding.isExpanded == true && viewModel.isDailyMode.value != false
            if (isExpanded) toggleCalendar()
            isExpanded
        }

        dataBinding.containerToolbarTitle.setOnClickListener {
            if (viewModel.isDailyMode.value != false) toggleCalendar()
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
        dataBinding.monthMillis = nowDate.millis

        dataBinding.containerTabs.selectTab(null, false)
        dataBinding.containerTabs.selectTab(dataBinding.containerTabs.getTabAt(0))
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.place_order, menu)
        menu.forEach { item ->
            item.icon?.let {
                DrawableCompat.setTint(it, resources.getColor(R.color.light_grey, null))
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId != R.id.menu_place_order) return super.onOptionsItemSelected(item)
        TimelinePlaceOrderDialogFragment.newInstance(
            isDescending = viewModel.currentOrderDescendingDirection,
            field = viewModel.currentOrderField
        ) { isDescending, field ->
            viewModel.loadPlaceStats(isDescending, field)
        }.show(parentFragmentManager, null)

        return true
    }

    override fun onItemBind(name: String, latitude: Double, longitude: Double) {
        lifecycleScope.launch {
            getMap()?.run {
                val marker = addMarker(
                    MarkerOptions().position(
                        LatLng(latitude, longitude)
                    ).icon(
                        BitmapDescriptorFactory.fromResource(R.drawable.ic_marker)
                    )
                )
                markers[(latitude to longitude)] = marker
            }
        }
    }

    override fun onItemClick(name: String, latitude: Double, longitude: Double) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        lastFocusedLocation = latitude to longitude

        selectMarker(latitude, longitude)

        lifecycleScope.launch {
            getMap()?.run { animateCamera(getCameraUpdate(latitude, longitude)) }
        }
    }

    override fun onItemLongClick(name: String, latitude: Double, longitude: Double) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
        lastFocusedLocation = latitude to longitude

        selectMarker(latitude, longitude)

        lifecycleScope.launch {
            getMap()?.run { animateCamera(getCameraUpdate(latitude, longitude)) }
        }

        val itemPlaceRename = getString(R.string.timeline_place_rename)
        val itemPlaceDetail = getString(R.string.timeline_place_detail)
        val location = Location(javaClass.name).apply {
            this.latitude = latitude
            this.longitude = longitude
        }

        SingleChoiceDialogFragment.newInstance(
            items = arrayOf(itemPlaceRename, itemPlaceDetail)
        ) { item ->
            when(item) {
                itemPlaceRename -> TimelinePlaceRenameDialogFragment.newInstance(name) { newName ->
                    viewModel.renamePlace(newName, latitude, longitude)
                }.show(parentFragmentManager, null)
                itemPlaceDetail -> findNavController().navigate(
                    TimelineFragmentDirections.actionPlaceDetail(
                        location = location
                    )
                )
            }
        }.show(parentFragmentManager, null)
    }

    override suspend fun navigateBeforeDataLoad() {
        markers.clear()

        val (latitude, longitude) = locationTracker.getLastLocation()?.let { it.latitude to it.longitude } ?: lastFocusedLocation

        val camera = getCameraUpdate(
            latitude,
            longitude,
            if (viewModel.isDailyMode.value != false) DEFAULT_ZOOM_LEVEL_DAILY_MODE else DEFAULT_ZOOM_LEVEL_PLACE_MODE
        )

        getMap()?.run {
            clear()
            animateCamera(camera)
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

    private fun changeDate(date: Date) {
        dataBinding.calendarView.currentDate = date
        viewModel.loadDailyStat(DateTime(date.time, timeZone))
        toggleCalendar()
    }

    private fun changeTab(isDailyMode: Boolean) {
        viewModel.isDailyMode.value = isDailyMode
        setHasOptionsMenu(!isDailyMode)

        if (isDailyMode) {
            childFragmentManager.commit {
                replace(R.id.container_timeline, TimelineChildDailyListFragment())
            }
            val dateTime = dataBinding.calendarView.currentDate?.let {
                DateTime(it.time, timeZone)
            } ?: nowDate
            viewModel.loadDailyStat(dateTime)
        } else {
            childFragmentManager.commit {
                replace(R.id.container_timeline, TimelineChildPlaceListFragment())
            }
            viewModel.loadPlaceStats()
        }
    }

    private fun changeBottomSheetState(bottomSheet: View, state: Int) = lifecycleScope.launch {
        val padding = when (state) {
            BottomSheetBehavior.STATE_HALF_EXPANDED -> bottomSheet.height * 0.40F
            BottomSheetBehavior.STATE_COLLAPSED -> 0F
            else -> null
        }

        getMap()?.run {
            if (padding != null) setPadding(0, 0, 0, padding.toInt())
            val (latitude, longitude) = lastFocusedLocation
            animateCamera(getCameraUpdate(latitude, longitude))
        }
    }

    private suspend fun getMap() : GoogleMap? =
        (childFragmentManager.findFragmentById(R.id.fragment_map) as? SupportMapFragment)?.getMap { map ->
            if (!map.isMyLocationEnabled) map.isMyLocationEnabled = true
            if (!map.uiSettings.isZoomControlsEnabled) map.uiSettings.isZoomControlsEnabled = true
            if (!map.uiSettings.isZoomGesturesEnabled) map.uiSettings.isZoomGesturesEnabled = true
        }

    private fun getCameraUpdate(latitude: Double, longitude: Double, zoomLevel: Float? = null) : CameraUpdate {
        return if (zoomLevel == null) {
            CameraUpdateFactory.newLatLng(LatLng(latitude, longitude))
        } else {
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoomLevel)
        }
    }

    private fun selectMarker(latitude: Double, longitude: Double) {
        markers.keys.forEach { (curLat, curLng) ->
            val isSame = curLat == latitude && curLng == longitude
            val icon = if (isSame) {
                BitmapDescriptorFactory.fromResource(R.drawable.ic_marker_selected)
            } else {
                BitmapDescriptorFactory.fromResource(R.drawable.ic_marker)
            }
            val zIndex = if (isSame) {
                Float.MAX_VALUE
            } else {
                0F
            }
            markers[(curLat to curLng)]?.setIcon(icon)
            markers[(curLat to curLng)]?.zIndex = zIndex
        }
    }

    companion object {
        private const val DEFAULT_ZOOM_LEVEL_DAILY_MODE = 15F
        private const val DEFAULT_ZOOM_LEVEL_PLACE_MODE = 12F
        const val EXTRA_FIELD_VISIT_TIME = 0x00
        const val EXTRA_FIELD_INCENTIVE = 0x01
        const val EXTRA_FIELD_MISSIONS = 0x02
        const val EXTRA_FIELD_VISITS = 0x03
    }

}