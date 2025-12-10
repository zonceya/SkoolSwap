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