package kaist.iclab.standup.smi.ui.dashboard

import android.util.Log
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import kaist.iclab.standup.smi.ui.dashboard.DashboardFragment.Companion.ARG_HAS_NEXT
import kaist.iclab.standup.smi.ui.dashboard.DashboardFragment.Companion.ARG_HAS_PREVIOUS
import org.joda.time.DateTime
import org.joda.time.Duration

class DashboardStatPagerAdapter(
    private val nItemCount: Int,
    fragment: Fragment
) : FragmentStateAdapter(fragment) {
    override fun getItemCount(): Int = nItemCount

    override fun createFragment(position: Int): Fragment {
        Log.d(javaClass.name, "createFragment(position = $position): itemCount = $itemCount")

        return DashboardChildStatPageFragment()
            .apply {
                arguments = bundleOf(
                    ARG_HAS_NEXT to (position != itemCount - 1),
                    ARG_HAS_PREVIOUS to (position > 0)
                )
            }
    }

}