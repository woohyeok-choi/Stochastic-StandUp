package kaist.iclab.standup.smi.io.pref

import github.agustarc.koap.PreferenceHolder
import github.agustarc.koap.delegator.ReadWriteString
import kaist.iclab.standup.smi.BuildConfig

object LocalPrefs : PreferenceHolder(BuildConfig.PREF_NAME) {
    var lastEventId: String by ReadWriteString(default = "")
}