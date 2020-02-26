package kaist.iclab.standup.smi.ui.config

import android.os.Bundle
import kaist.iclab.standup.smi.base.BaseViewModel

class ConfigViewModel(navigator: ConfigNavigator) : BaseViewModel<ConfigNavigator>(navigator) {
    override suspend fun onLoad(extras: Bundle?) {
    }
}