package kaist.iclab.standup.smi

import android.content.Intent
import android.os.IBinder
import kaist.iclab.standup.smi.base.BaseService
import kaist.iclab.standup.smi.io.SedentaryTrackRepository
import org.koin.android.ext.android.inject

class StandUpService : BaseService() {
    private val tracker: SedentaryTrackRepository by inject()

    override fun onBind(intent: Intent?): IBinder? = null
}