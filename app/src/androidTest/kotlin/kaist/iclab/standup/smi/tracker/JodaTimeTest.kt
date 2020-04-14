package kaist.iclab.standup.smi.tracker

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.danlew.android.joda.JodaTimeAndroid
import org.joda.time.LocalDate
import org.joda.time.Period

import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit

class JodaTimeTest {

    @Before
    fun setUp() {
        JodaTimeAndroid.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun jodaTime_compareDays() {
        val millis = System.currentTimeMillis()
        val curTime = LocalDate.fromCalendarFields(Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        })
        val prevTime = curTime.minusDays(30)

        assertThat(prevTime == curTime).isFalse()

        val period = Period(prevTime, curTime)

        assertThat(period.toStandardDays().days).isEqualTo(30)
    }

    @Test
    fun jodaTime_isSameDay() {
        val millis = System.currentTimeMillis()
        val curTime = LocalDate.fromCalendarFields(Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        })

        val otherTime = LocalDate.fromCalendarFields(Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis - TimeUnit.SECONDS.toMillis(150)
        })

        assertThat(curTime).isEqualTo(otherTime)
    }
}