package kaist.iclab.standup.smi.common

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.ContentLoadingProgressBar
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.view.ProgressFloatingActionButton
import kotlin.math.abs

@BindingAdapter("url")
fun loadCircularImage(view: ImageView, circularImg: String?) {
    Glide.with(view)
        .load(circularImg)
        .apply(
            RequestOptions
                .circleCropTransform()
                .placeholder(R.drawable.ic_placehoder)
        )
        .circleCrop()
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(view)
}

@BindingAdapter("crossFadeIn", "duration", "isGone")
fun crossFadeIn(view: View, crossFadeIn: Boolean?, duration: Int?, isGone: Boolean?) {
    if (crossFadeIn == true) {
        view.apply {
            alpha = 0F
            visibility = View.VISIBLE
            animate().alpha(1F).setDuration(duration?.toLong() ?: 500).setListener(null)
        }
    } else {
        view.visibility = if (isGone == true) View.GONE else View.INVISIBLE
    }
}

@BindingAdapter("crossFadeOut", "duration", "isGone")
fun crossFadeOut(view: View, crossFadeOut: Boolean?, duration: Int?, isGone: Boolean?) {
    if (crossFadeOut == true) {
        view.apply {
            alpha = 1F
            animate().alpha(0F).setDuration(duration?.toLong() ?: 500).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = if (isGone == true) View.GONE else View.INVISIBLE
                }
            })
        }
    } else {
        view.visibility = View.VISIBLE
    }
}

@BindingAdapter("crossFade", "duration", "isGone")
fun crossFade(view: View, crossFade: Boolean?, duration: Int?, isGone: Boolean?) {
    if (crossFade == true) {
        view.apply {
            alpha = 0F
            visibility = View.VISIBLE
            animate().alpha(1F).setDuration(duration?.toLong() ?: 500).setListener(null)
        }
    } else {
        view.apply {
            alpha = 1F
            animate().alpha(0F).setDuration(duration?.toLong() ?: 500).setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    view.visibility = if (isGone == true) View.GONE else View.INVISIBLE
                }
            })
        }
    }
}

@BindingAdapter("toggleRotate", "duration", "clockWise")
fun toggleRotate180(view: View, toggleRotate: Boolean?, duration: Int?, clockWise: Boolean?) {
    if (toggleRotate == null || duration == null) return
    val angle = if (view.rotation == 0F) {
        if (clockWise == true) 180F else -180F
    } else {
        0F
    }
    view.animate().rotation(angle).setDuration(duration.toLong()).start()
}

@BindingAdapter("tint")
fun setTint(view: TextView, tint: Int?) {
    if (tint != null) {
        view.compoundDrawableTintList = ColorStateList.valueOf(tint)
    }
}

@BindingAdapter("incentive", "succeeded")
fun setFormattedIncentiveString(view: TextView, incentive: Int?, succeeded: Boolean?) {
    if (incentive == null || succeeded == null) return
    val text = when {
        incentive > 0 && succeeded -> view.context.getString(
            R.string.timeline_list_item_mission_bid_positive_success, abs(incentive)
        )
        incentive > 0 && !succeeded -> view.context.getString(
            R.string.timeline_list_item_mission_bid_positive_failure, abs(incentive)
        )
        incentive < 0 && succeeded -> view.context.getString(
            R.string.timeline_list_item_mission_bid_negative_success, abs(incentive)
        )
        incentive < 0 && !succeeded -> view.context.getString(
            R.string.timeline_list_item_mission_bid_negative_failure, abs(incentive)
        )
        else -> view.context.getString(R.string.timeline_list_item_mission_bid_no_point)
    }
    view.text = text
}

@BindingAdapter("selected")
fun setTextViewSelected(view: TextView, selected: Int?) {
    if (selected == null) return
    view.isSelected = view.id == selected
}