package kaist.iclab.standup.smi.common

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import kaist.iclab.standup.smi.R
import kaist.iclab.standup.smi.view.ProgressFloatingActionButton

@BindingAdapter("circularImg")
fun loadCircularImage(view: ImageView, circularImg: String) {
    Glide.with(view)
        .load(circularImg)
        .transition(DrawableTransitionOptions.withCrossFade())
        .centerCrop()
        .placeholder(R.drawable.placehoder)
        .into(view)
}

@BindingAdapter("indeterminate")
fun setIndeterminate(view: ProgressFloatingActionButton, indeterminate: Boolean) {
    view.isInIndeterminate = indeterminate
}

@BindingAdapter("progress")
fun setProgress(view: ProgressFloatingActionButton, progress: Int) {
    view.progress = progress
}