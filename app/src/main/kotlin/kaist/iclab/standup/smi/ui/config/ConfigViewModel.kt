package kaist.iclab.standup.smi.ui.config

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.standup.smi.BuildConfig
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.StandUpIntentService
import kaist.iclab.standup.smi.StandUpService
import kaist.iclab.standup.smi.base.BaseViewModel
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.common.checkPermissions
import kaist.iclab.standup.smi.common.hourMinuteToString
import kaist.iclab.standup.smi.common.millisToHourMinute
import kaist.iclab.standup.smi.pref.LocalPrefs
import kaist.iclab.standup.smi.pref.RemotePrefs
import kaist.iclab.standup.smi.ui.splash.SplashActivity
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.concurrent.TimeUnit

class ConfigViewModel(
    private val context: Context,
    private val permissions: Array<String>
) : BaseViewModel<ConfigNavigator>() {

    val items = liveData {
        emit(
            config {
                header {
                    id = "$PREFIX.GENERAL"
                    title = resStr(R.string.config_header_general)

                    readOnly {
                        id = "$PREFIX.USER_NAME"
                        title = resStr(R.string.config_item_user_name)
                        formatter = {
                            FirebaseAuth.getInstance().currentUser?.displayName
                                ?: context.getString(R.string.general_unknown)
                        }
                    }

                    readOnly {
                        id = "$PREFIX.USER_EMAIL"
                        title = resStr(R.string.config_item_email)
                        formatter = {
                            FirebaseAuth.getInstance().currentUser?.email
                                ?: context.getString(R.string.general_unknown)
                        }
                    }

                    readOnly {
                        id = "$PREFIX.PERMISSIONS"
                        title = context.getString(R.string.config_item_permissions)
                        formatter = {
                            if (context.checkPermissions(permissions)) {
                                resStr(R.string.config_item_permissions_granted)
                            } else {
                                resStr(R.string.config_item_permissions_denied)
                            }
                        }
                        onActivity = {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                .setData(Uri.parse("package:${context.packageName}"))
                        }
                    }
                }

                header {
                    id = "$PREFIX.MISSIONS"
                    title = resStr(R.string.config_header_mission)

                    boolean {
                        id = "$PREFIX.MISSION_ON_OFF"
                        title = resStr(R.string.config_item_is_mission_triggered)
                        value = { LocalPrefs.isMissionOn }
                        formatter = {
                            if (it) {
                                resStr(R.string.config_item_is_mission_triggered_on)
                            } else {
                                resStr(R.string.config_item_is_mission_triggered_off)
                            }
                        }
                        onSave = { LocalPrefs.isMissionOn = it }
                    }

                    localTimeRange {
                        id = "$PREFIX.MISSION_AVAILABLE_TIME"
                        title = resStr(R.string.config_item_mission_time_range)
                        value = {
                            LocalPrefs.activeStartTimeMs to LocalPrefs.activeEndTimeMs
                        }
                        formatter = { (from, to) ->
                            resStr(
                                R.string.general_range,
                                from.millisToHourMinute().hourMinuteToString(),
                                to.millisToHourMinute().hourMinuteToString()
                            )
                        }
                        isSavable = { (from, to) ->
                            from <= to && to - from > TimeUnit.HOURS.toMillis(9)
                        }
                        onSave = { (from, to) ->
                            LocalPrefs.activeStartTimeMs = from
                            LocalPrefs.activeEndTimeMs = to
                        }
                    }

                    number {
                        id = "$PREFIX.DO_NOT_DISTURB_UNTIL"
                        title = resStr(R.string.config_item_do_not_disturb)
                        value = { 0 }
                        formatter = {
                            val curTime = System.currentTimeMillis()
                            val value = LocalPrefs.doNotDisturbUntil
                            checkDoNotDisturb(curTime)

                            if (curTime > value) {
                                resStr(
                                    R.string.config_item_do_not_disturb_unset,
                                    LocalPrefs.doNotDisturbCount
                                )
                            } else {
                                resStr(
                                    R.string.config_item_do_not_disturb_set,
                                    TimeUnit.MILLISECONDS.toMinutes(value - curTime),
                                    LocalPrefs.doNotDisturbCount
                                )
                            }
                        }
                        isSavable = {
                            val curTime = System.currentTimeMillis()

                            checkDoNotDisturb(curTime)

                            LocalPrefs.doNotDisturbCount > 0
                        }
                        valueFormatter = {
                            resStr(R.string.config_item_do_not_disturb_value_formatter, it)
                        }
                        onSave = { value ->
                            val curTime = System.currentTimeMillis()
                            LocalPrefs.doNotDisturbUntil =
                                curTime + TimeUnit.MINUTES.toMillis(value)
                            LocalPrefs.doNotDisturbCount--
                            StandUpService.startService(context)
                        }
                        min = 30
                        max = 120
                    }
                }

                header {
                    id = "$PREFIX.DEBUG"
                    title = resStr(R.string.config_header_debug)

                    number {
                        id = "$PREFIX.MIN_STAY_TIME"
                        title = resStr(R.string.config_item_min_stay_time)
                        value = { TimeUnit.MILLISECONDS.toMinutes(RemotePrefs.minTimeForStayEvent) }
                        formatter = { resStr(R.string.general_minute_abbrev, it) }
                        onSave = { RemotePrefs.minTimeForStayEvent = TimeUnit.MINUTES.toMillis(it) }
                        valueFormatter = { resStr(R.string.general_minute_abbrev, it) }
                        min = 1L
                        max = 40L
                    }

                    number {
                        id = "$PREFIX.MIN_MISSION_TRIGGER_TIME"
                        title = resStr(R.string.config_item_min_mission_trigger_time)
                        value =
                            { TimeUnit.MILLISECONDS.toMinutes(RemotePrefs.minTimeForMissionTrigger) }
                        formatter = { resStr(R.string.general_minute_abbrev, it) }
                        isSavable = { it in min..max }
                        onSave =
                            { RemotePrefs.minTimeForMissionTrigger = TimeUnit.MINUTES.toMillis(it) }
                        valueFormatter = { resStr(R.string.general_minute_abbrev, it) }
                        min = 2L
                        max = 150L
                    }

                    number {
                        id = "$PREFIX.MISSION_TIMEOUT"
                        title = resStr(R.string.config_item_mission_timeout)
                        value =
                            { TimeUnit.MILLISECONDS.toMinutes(RemotePrefs.timeoutForMissionExpired) }
                        formatter = { resStr(R.string.general_minute_abbrev, it) }
                        isSavable = { it in min..max }
                        onSave =
                            { RemotePrefs.timeoutForMissionExpired = TimeUnit.MINUTES.toMillis(it) }
                        valueFormatter = { resStr(R.string.general_minute_abbrev, it) }
                        min = 1L
                        max = 20L
                    }

                    number {
                        id = "$PREFIX.MAX_DAILY_BUDGET"
                        title = resStr(R.string.config_max_daily_budget)
                        value = { RemotePrefs.maxDailyBudget.toLong() }
                        formatter = { resStr(R.string.general_points_abbrev, it) }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.maxDailyBudget = it.toInt() }
                        valueFormatter = { resStr(R.string.general_points_abbrev, it) }
                        min = 500
                        max = 2000
                    }

                    number {
                        id = "$PREFIX.MAX_DO_NOT_DISTURB"
                        title = resStr(R.string.config_max_daily_do_not_disturb)
                        value = { RemotePrefs.maxDoNotDisturb.toLong() }
                        formatter = { resStr(R.string.general_times, it) }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.maxDoNotDisturb = it.toInt() }
                        valueFormatter = { resStr(R.string.general_times, it) }
                        min = 0
                        max = 10
                    }

                    choice {
                        id = "$PREFIX.INCENTIVE_MODE"
                        title = resStr(R.string.config_incentive_mode)
                        value = { RemotePrefs.incentiveMode }
                        onSave = { RemotePrefs.incentiveMode = it }
                        formatter = { value ->
                            when (value) {
                                RemotePrefs.INCENTIVE_MODE_FIXED -> resStr(R.string.config_incentive_mode_fixed)
                                RemotePrefs.INCENTIVE_MODE_STOCHASTIC -> resStr(R.string.config_incentive_mode_stochastic)
                                else -> resStr(R.string.config_incentive_mode_none)
                            }
                        }
                        valueFormatter = { value ->
                            when (value) {
                                RemotePrefs.INCENTIVE_MODE_FIXED -> resStr(R.string.config_incentive_mode_fixed)
                                RemotePrefs.INCENTIVE_MODE_STOCHASTIC -> resStr(R.string.config_incentive_mode_stochastic)
                                else -> resStr(R.string.config_incentive_mode_none)
                            }
                        }
                        options = intArrayOf(
                            RemotePrefs.INCENTIVE_MODE_NONE,
                            RemotePrefs.INCENTIVE_MODE_FIXED,
                            RemotePrefs.INCENTIVE_MODE_STOCHASTIC
                        )
                    }

                    number {
                        id = "$PREFIX.WIN_SIZE_IN_MILLIS"
                        title = resStr(R.string.config_win_size_incentive_calc)
                        value = { TimeUnit.MILLISECONDS.toDays(RemotePrefs.winSizeInMillis) }
                        formatter = { resStr(R.string.general_days, it) }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.winSizeInMillis = TimeUnit.DAYS.toMillis(it) }
                        valueFormatter = { resStr(R.string.general_days, it) }
                        min = 1
                        max = 14
                    }

                    number {
                        id = "$PREFIX.WIN_SIZE_IN_NUMBER"
                        title = resStr(R.string.config_num_data_incentive_calc)
                        value = { RemotePrefs.winSizeInNumber.toLong() }
                        formatter = { RemotePrefs.winSizeInNumber.toString() }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.winSizeInNumber = it.toInt() }
                        min = 10
                        max = 500
                    }

                    numberRange {
                        id = "$PREFIX.INCENTIVE_RANGE"
                        title = resStr(R.string.config_range_incentive)
                        value =
                            { RemotePrefs.minIncentives.toLong() to RemotePrefs.maxIncentives.toLong() }
                        formatter = { (from, to) ->
                            resStr(
                                R.string.general_range,
                                resStr(R.string.general_points_abbrev, from),
                                resStr(R.string.general_points_abbrev, to)
                            )
                        }
                        isSavable =
                            { (from, to) -> from in (min..max) && to in (min..max) && from <= to }
                        onSave = { (from, to) ->
                            RemotePrefs.minIncentives = from.toInt()
                            RemotePrefs.maxIncentives = to.toInt()
                        }
                        valueFormatter = { resStr(R.string.general_points_abbrev, it) }
                        min = 10
                        max = 1000
                    }

                    number {
                        id = "$PREFIX.INCENTIVE_DEFAULT"
                        title = resStr(R.string.config_default_incentive)
                        value = { RemotePrefs.defaultIncentives.toLong() }
                        formatter = { resStr(R.string.general_points_abbrev, it) }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.defaultIncentives = it.toInt() }
                        valueFormatter = { resStr(R.string.general_points_abbrev, it) }
                        min = 10
                        max = 1000
                    }

                    number {
                        id = "$PREFIX.INCENTIVE_UNIT"
                        title = resStr(R.string.config_unit_incentive)
                        value = { RemotePrefs.unitIncentives.toLong() }
                        formatter = { resStr(R.string.general_points_abbrev, it) }
                        isSavable = { it in min..max }
                        onSave = { RemotePrefs.unitIncentives = it.toInt() }
                        valueFormatter = { resStr(R.string.general_points_abbrev, it) }
                        min = 10
                        max = 100
                    }

                    readOnly {
                        id = "$PREFIX.MOCK_ENTER_STILL"
                        title = resStr(R.string.config_mock_still_event)
                        formatter = { resStr(R.string.config_mock_still_event_desc) }
                        onAction = { StandUpIntentService.enterIntoStill(it) }
                    }

                    readOnly {
                        id = "$PREFIX.MOCK_EXIT_STILL"
                        title = resStr(R.string.config_mock_stand_up_event)
                        formatter = { resStr(R.string.config_mock_stand_up_event_desc) }
                        onAction = { StandUpIntentService.exitFromStill(it) }
                    }
                }

                header {
                    id = "$PREFIX.OTHERS"
                    title = resStr(R.string.config_header_others)

                    readOnly {
                        id = "$PREFIX.VERSION"
                        title = resStr(R.string.config_item_app_version)
                        formatter = { "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})" }
                    }

                    readOnly {
                        id = "$PREFIX.SIGN_OUT"
                        title = resStr(R.string.config_item_sign_out)
                        formatter = { resStr(R.string.config_item_sign_out_desc) }
                        onAction = {
                            navigator?.navigateSignOut()
                        }
                    }

                }
            }.items
        )
    }

    private fun checkDoNotDisturb(curTime: Long) {
        val timeZone = DateTimeZone.getDefault()
        val curDateTime = DateTime(curTime, timeZone).withTimeAtStartOfDay()

        if (LocalPrefs.doNotDisturbLastTimeSettingMillis <= 0) {
            LocalPrefs.doNotDisturbLastTimeSettingMillis = 0
        }

        val lastSettingDateTime = DateTime(LocalPrefs.doNotDisturbLastTimeSettingMillis, timeZone)

        if (curDateTime > lastSettingDateTime) {
            LocalPrefs.doNotDisturbLastTimeSettingMillis = curDateTime.millis
            LocalPrefs.doNotDisturbCount = RemotePrefs.maxDoNotDisturb
        }
    }

    private fun resStr(@StringRes stringRes: Int, vararg params: Any): String =
        context.getString(stringRes, *params)

    companion object {
        private val PREFIX = ConfigViewModel::class.java.name
    }
}