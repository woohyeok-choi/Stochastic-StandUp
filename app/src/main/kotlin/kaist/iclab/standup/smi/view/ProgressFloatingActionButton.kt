package kaist.iclab.standup.smi.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.internal.DescendantOffsetUtils
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kaist.iclab.standup.smi.R
import kotlin.math.min

class ProgressFloatingActionButton(context: Context, attributeSet: AttributeSet) : FrameLayout(context, attributeSet) {
    private val progressBar: ProgressBar = ProgressBar(context)
    private var floatingActionButton: FloatingActionButton? = null

    init {
        isFocusable = true
        isClickable = true

        addView(progressBar)
    }

    var progress: Int
        get() = progressBar.progress
        set(value) { progressBar.progress = value }

    var isInIndeterminate
        get() = progressBar.isIndeterminate
        set(value) { progressBar.isIndeterminate = value }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val fab = children.find { it is FloatingActionButton } ?: throw IllegalArgumentException("A child should be an instance of FloatActionButton.")
        floatingActionButton = fab as? FloatingActionButton
        resize()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resize()
    }

    private fun resize() {
        val additionalSize = resources.getDimensionPixelSize(R.dimen.fab_with_progress_size)
        floatingActionButton?.let { fab ->
            progressBar.updateLayoutParams {
                Log.d("ZXCV", "${fab.width}/$additionalSize")
                width = fab.width + additionalSize
                height = fab.height + additionalSize
                (this as? LayoutParams)?.gravity = Gravity.CENTER
            }
            fab.updateLayoutParams {
                (this as? LayoutParams)?.gravity = Gravity.CENTER
            }
        }
    }

}

