package kaist.iclab.standup.smi.ui.dashboard

import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorRes
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.observe
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.sharedViewModelFromFragment
import kaist.iclab.standup.smi.databinding.FragmentDashboardWeeklyChartBinding
import org.joda.time.DateTime
import java.util.*

class DashboardChildWeeklyChartFragment : BaseFragment<FragmentDashboardWeeklyChartBinding, DashboardViewModel>() {
    override val viewModel: DashboardViewModel by sharedViewModelFromFragment()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_dashboard_weekly_chart

    private val interpolator = DecelerateInterpolator()

    override fun beforeExecutePendingBindings() {
        dataBinding.chart.apply {
            isDoubleTapToZoomEnabled = false
            description.isEnabled = false
            isClickable = false
            isDragEnabled = false
            isSelected = false
            setTouchEnabled(false)
            setDrawBarShadow(false)
            setDrawGridBackground(false)
            setPinchZoom(false)
            setDrawBorders(false)

            axisLeft.axisMinimum = 0F
            axisLeft.labelCount = 5
            axisLeft.isGranularityEnabled = false
            axisLeft.setDrawAxisLine(false)
            axisLeft.setDrawGridLines(false)

            axisRight.axisMinimum = 0F
            axisRight.labelCount = 5
            axisRight.isGranularityEnabled = false
            axisRight.setDrawGridLines(false)
            axisRight.setDrawAxisLine(false)
            axisRight.setValueFormatter { value, _ ->
                getString(R.string.general_points_abbrev, value.toInt())
            }

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.setDrawAxisLine(false)
            xAxis.granularity = 1F
            xAxis.spaceMin = 0.5F
            xAxis.spaceMax = 0.5F

            legend.isEnabled = false

            data = CombinedData().apply {
                setData(BarData().apply {
                    barWidth = 0.3F
                })
                setData(LineData())
            }
        }

        viewModel.currentDateTime.observe(this) { dateTime ->
            dataBinding.chart.xAxis.setValueFormatter { value, axis ->
                val diff = axis.axisMaximum - value
                dateTime.minusDays(diff.toInt()).dayOfWeek().getAsShortText(Locale.getDefault())
            }
        }

        viewModel.weeklyChartData().observe(this) { (data, chartType) ->
            val colorRes  = when (chartType) {
                DashboardFragment.CHART_TYPE_TOTAL_SEDENTARY_TIME -> R.color.blue
                DashboardFragment.CHART_TYPE_AVG_SEDENTARY_TIME -> R.color.violet
                DashboardFragment.CHART_TYPE_NUM_PROLONGED_SEDENTARINESS -> R.color.magenta
                else -> return@observe
            }

            dataBinding.chart.data.setData(
                buildBarData(
                    data = data.mapValues { (_, v) -> v.first },
                    colorRes = colorRes,
                    barData = dataBinding.chart.barData
                )
            )
            dataBinding.chart.data.setData(
                buildLineData(
                    data = data.mapValues { (_, v) -> v.second },
                    lineData = dataBinding.chart.lineData
                )
            )
            dataBinding.chart.data.notifyDataChanged()
            dataBinding.chart.notifyDataSetChanged()
            dataBinding.chart.animateY(500) {
                interpolator.getInterpolation(it)
            }
        }

        viewModel.loadChart(DashboardFragment.CHART_TYPE_TOTAL_SEDENTARY_TIME)
    }

    private fun buildBarData(data: Map<DateTime, Long>,
                             @ColorRes colorRes: Int,
                             barData: BarData) : BarData {
        val sortedData = TreeMap(data)
        val barEntries = sortedData.keys.mapIndexed { index, dateTime ->
            BarEntry(index.toFloat(), sortedData[dateTime]?.toFloat() ?: 0F)
        }
        val prevDataSet = barData.dataSets?.firstOrNull() as? BarDataSet

        val dataSet = if (prevDataSet == null) {
            BarDataSet(barEntries, "").apply {
                axisDependency = YAxis.AxisDependency.LEFT
                valueTextSize = 0F
            }
        } else {
            prevDataSet.values = barEntries
            prevDataSet
        }

        dataSet.color = ResourcesCompat.getColor(resources, colorRes, null)
        dataSet.label = ""

        barData.clearValues()
        barData.addDataSet(dataSet)

        return barData
    }

    private fun buildLineData(data: Map<DateTime, Int>, lineData: LineData) : LineData {
        val sortedData = TreeMap(data)
        val entries = sortedData.keys.mapIndexed { index, dateTime ->
            Entry(index.toFloat(), sortedData[dateTime]?.toFloat() ?: 0F)
        }
        val prevDataSet = lineData.dataSets?.firstOrNull() as? LineDataSet

        val dataSet = if(prevDataSet == null) {
            LineDataSet(entries, getString(R.string.dashboard_chart_label_incentives)).apply {
                axisDependency = YAxis.AxisDependency.RIGHT
                valueTextSize = 0F
                color = ResourcesCompat.getColor(resources, R.color.yellow, null)
                circleRadius = 5F
                lineWidth = 2F
                setCircleColor(ResourcesCompat.getColor(resources, R.color.dark_yellow, null))
            }
        } else {
            prevDataSet.values = entries
            prevDataSet
        }

        lineData.clearValues()
        lineData.addDataSet(dataSet)

        return lineData
    }
}