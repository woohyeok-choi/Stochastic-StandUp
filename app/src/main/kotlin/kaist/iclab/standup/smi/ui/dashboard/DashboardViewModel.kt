package kaist.iclab.standup.smi.ui.dashboard

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import kaist.iclab.standup.smi.base.BaseViewModel
import org.joda.time.LocalDate

class DashboardViewModel(
                         navigator: DashboardNavigator) : BaseViewModel<DashboardNavigator>(navigator) {
    val localDate: MutableLiveData<LocalDate> = MutableLiveData()



    override suspend fun onLoad(extras: Bundle?) {
    }

}