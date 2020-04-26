package kaist.iclab.standup.smi.ui.config

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import kaist.iclab.standup.smi.BR
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.StandUpService
import kaist.iclab.standup.smi.base.BaseBottomSheetDialogFragment
import kaist.iclab.standup.smi.base.BaseFragment
import kaist.iclab.standup.smi.common.asSuspend
import kaist.iclab.standup.smi.databinding.FragmentConfigBinding
import kaist.iclab.standup.smi.ui.splash.SplashActivity
import kotlinx.android.synthetic.main.fragment_config.*
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class ConfigFragment : BaseFragment<FragmentConfigBinding, ConfigViewModel>(), ConfigNavigator {
    override val viewModel: ConfigViewModel by viewModel()
    override val viewModelVariable: Int = BR.viewModel
    override val layoutId: Int = R.layout.fragment_config

    private lateinit var adapter: ConfigListAdapter

    override fun beforeExecutePendingBindings() {
        (activity as? AppCompatActivity)?.setSupportActionBar(toolbar)
        (activity as? AppCompatActivity)?.supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
        }
        viewModel.navigator = this

        adapter = ConfigListAdapter { item, position ->
            when (item) {
                is ReadOnlyConfigItem -> {
                    item.onActivity?.invoke()?.let { startActivityForResult(it, position) }
                    item.onAction?.invoke(requireContext())
                }
                is SingleChoiceConfigItem -> ConfigSingleChoiceDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
                is BooleanConfigItem -> ConfigBooleanDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
                is NumberConfigItem -> ConfigNumberDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
                is NumberRangeConfigItem -> ConfigNumberRangeDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
                is LocalTimeConfigItem -> ConfigTimeDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
                is LocalTimeRangeConfigItem -> ConfigTimeRangeDialogFragment.newInstance(item) {
                    adapter.notifyItemChanged(position)
                }.show(parentFragmentManager, null)
            }
        }

        dataBinding.listConfigItem.adapter = adapter

        viewModel.items.observe(this) {
            adapter.items = it
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        adapter.notifyItemChanged(requestCode)
    }

    override fun navigateSignOut() {
        FirebaseAuth.getInstance().signOut()

        lifecycleScope.launch {
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            try {
                GoogleSignIn.getClient(requireActivity(), options).signOut().asSuspend()
            } catch (e: Exception) { }

            activity?.finish()
            StandUpService.stopService(requireContext())

            startActivity(
                Intent(context, SplashActivity::class.java)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}