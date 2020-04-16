package kaist.iclab.standup.smi.ui.config

import android.content.Intent
import androidx.lifecycle.observe
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.databinding.FragmentConfigBinding
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment<FragmentConfigBinding, ConfigViewModel>(), ConfigNavigator, BaseBottomSheetDialogFragment.OnDismissListener {
    override val viewModel: ConfigViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_config

    private lateinit var adapter: ConfigListAdapter

    override fun beforeExecutePendingBindings() {
        viewModel.navigator = this

        adapter = ConfigListAdapter { item, position ->
            when (item) {
                is ReadOnlyConfigItem -> item.onAction?.invoke()?.let { startActivityForResult(it, position) }
                is SingleChoiceConfigItem -> showDialogFragment(ConfigSingleChoiceDialogFragment.newInstance(item), position)
                is BooleanConfigItem -> showDialogFragment(ConfigBooleanDialogFragment.newInstance(item), position)
                is NumberConfigItem -> showDialogFragment(ConfigNumberDialogFragment.newInstance(item), position)
                is NumberRangeConfigItem -> showDialogFragment(ConfigNumberRangeDialogFragment.newInstance(item), position)
                is LocalTimeConfigItem -> showDialogFragment(ConfigTimeDialogFragment.newInstance(item), position)
                is LocalTimeRangeConfigItem -> showDialogFragment(ConfigTimeRangeDialogFragment.newInstance(item), position)
            }
        }

        dataBinding.listConfigItem.adapter = adapter

        viewModel.items.observe(this) {
            adapter.items = it
        }
    }

    override fun onDismiss(requestCode: Int) {
        adapter.notifyItemChanged(requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter.notifyItemChanged(requestCode)
    }

    private fun showDialogFragment(fragment: BaseBottomSheetDialogFragment<*>, position: Int) {
        fragment.setTargetFragment(this, position)
        fragment.show(parentFragmentManager, javaClass.name)
    }
}