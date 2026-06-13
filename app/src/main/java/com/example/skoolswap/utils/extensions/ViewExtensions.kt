package com.example.skoolswap.utils.extensions

import android.view.View
import androidx.core.view.isVisible

fun View.show() {
    visibility = View.VISIBLE
}

fun View.hide() {
    visibility = View.GONE
}

fun View.toggleVisibility() {
    isVisible = !isVisible
}
fun Int.formatViewCount(): String {
    return when {
        this >= 1_000_000 -> "${this / 1_000_000}M"
        this >= 1_000 -> "${this / 1_000}k"
        else -> this.toString()
    }
}
fun Float.dpToPx(context: android.content.Context): Float {
    return this * context.resources.displayMetrics.density
}