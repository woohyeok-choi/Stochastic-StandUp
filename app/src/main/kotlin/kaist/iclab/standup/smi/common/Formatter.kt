package kaist.iclab.standup.smi.common

import android.content.Context
import android.text.format.DateUtils
import kaist.iclab.standup.smi.R
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Formatter {
    private val HOUR_IN_MILLIS = TimeUnit.HOURS.toMillis(1)
    private val DAYS_IN_MILLIS = TimeUnit.DAYS.toMillis(1)
    private val WEEK_IN_MILLIS = DAYS_IN_MILLIS * 7
    private val MONTH_IN_MILLIS = WEEK_IN_MILLIS * 4
    private val YEAR_IN_MILLIS = DAYS_IN_MILLIS * 365

    @JvmStatic
    fun getRelativeTimeSpanString(context: Context, millis: Long) : CharSequence {
        val now = System.currentTimeMillis()
        val span = abs(now - millis)
        val nowTime = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = now
        }
        val thenTime = GregorianCalendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        }

        return when {
            span < DAYS_IN_MILLIS -> {
                DateUtils.formatDateRange(context, millis, millis, DateUtils.FORMAT_SHOW_TIME)
            }
            span in (DAYS_IN_MILLIS until WEEK_IN_MILLIS) -> {
                DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.DAY_IN_MILLIS)
            }
            span in (WEEK_IN_MILLIS until MONTH_IN_MILLIS) -> {
                DateUtils.getRelativeTimeSpanString(millis, now, DateUtils.WEEK_IN_MILLIS)
            }
            span > MONTH_IN_MILLIS && nowTime.get(Calendar.YEAR) == thenTime.get(Calendar.YEAR) -> {
                DateUtils.formatDateRange(context, millis, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
            }
            else -> {
                DateUtils.formatDateRange(context, millis, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_NUMERIC_DATE)
            }
        }
    }

    @JvmStatic
    fun getElapsedTimeString(context: Context, millis: Long) : CharSequence {
        val (time, resId) = when {
            millis < HOUR_IN_MILLIS -> TimeUnit.MILLISECONDS.toMinutes(millis) to R.string.general_minute_abbrev
            millis in (HOUR_IN_MILLIS until DAYS_IN_MILLIS) -> TimeUnit.MILLISECONDS.toHours(millis) to R.string.general_hours
            millis in (DAYS_IN_MILLIS until WEEK_IN_MILLIS) -> TimeUnit.MILLISECONDS.toDays(millis) to R.string.general_days
            millis in (WEEK_IN_MILLIS until MONTH_IN_MILLIS) -> TimeUnit.MILLISECONDS.toDays(millis) / 7 to R.string.general_weeks
            millis in (MONTH_IN_MILLIS until YEAR_IN_MILLIS) -> TimeUnit.MILLISECONDS.toDays(millis) / 28 to R.string.general_months
            else -> TimeUnit.MILLISECONDS.toDays(millis) / 365 to R.string.general_years_abbrev
        }
        return context.getString(resId, time.toInt())
    }

    @JvmStatic
    fun getSIPrefixNumberString(number: Long) : CharSequence =
        if (number < 1e3) number.toString()
        else if (number >= 1e3 && number < 1e6) String.format("%.1fk", number.toFloat() / 1e3)
        else String.format("%.1fM", number.toFloat() / 1e6)

    @JvmStatic
    fun percentage(numerator: Int, denominator: Int) : Int {
        return if (denominator == 0) 0 else (numerator.toFloat() / denominator * 100).toInt().coerceAtMost(100)
    }

    @JvmStatic
    fun percentage(numerator: Long, denominator: Long) : Int {
        return if (denominator == 0L) 0 else (numerator.toFloat() / denominator * 100).toInt().coerceAtMost(100)
    }
}