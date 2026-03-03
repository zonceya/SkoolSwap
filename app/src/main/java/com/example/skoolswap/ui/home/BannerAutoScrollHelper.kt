// ui/home/BannerAutoScrollHelper.kt
package com.example.skoolswap.ui.home

import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2
import java.lang.ref.WeakReference

class BannerAutoScrollHelper(
    viewPager: ViewPager2,
    private val interval: Long = 3000 // 3 seconds
) {
    private val weakViewPager = WeakReference(viewPager)
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    private val scrollRunnable = object : Runnable {
        override fun run() {
            weakViewPager.get()?.let { viewPager ->
                val nextItem = viewPager.currentItem + 1
                viewPager.setCurrentItem(nextItem, true)
                handler.postDelayed(this, interval)
            }
        }
    }

    fun startAutoScroll() {
        if (!isRunning) {
            isRunning = true
            handler.postDelayed(scrollRunnable, interval)
        }
    }

    fun stopAutoScroll() {
        isRunning = false
        handler.removeCallbacks(scrollRunnable)
    }

    fun pauseAutoScroll() {
        handler.removeCallbacks(scrollRunnable)
    }

    fun resumeAutoScroll() {
        if (isRunning) {
            handler.postDelayed(scrollRunnable, interval)
        }
    }
}