package za.co.skoolswap.ui.home

import android.os.Handler
import android.os.Looper
import androidx.viewpager2.widget.ViewPager2
import java.lang.ref.WeakReference

class BannerAutoScrollHelper(
    viewPager: ViewPager2,
    private val interval: Long = 8000L
) {
    private val weakViewPager = WeakReference(viewPager)
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var isPaused = false

    private val scrollRunnable = object : Runnable {
        override fun run() {
            // ✅ Only proceed if ViewPager is still alive and we're not paused
            if (!isPaused) {
                weakViewPager.get()?.let { viewPager ->
                    // ✅ Check if we're at the end of the infinite loop
                    val nextItem = viewPager.currentItem + 1
                    viewPager.setCurrentItem(nextItem, true)
                    handler.postDelayed(this, interval)
                }
            } else {
                // ✅ If paused, don't reschedule
                isRunning = false
            }
        }
    }

    fun startAutoScroll() {
        if (!isRunning) {
            isRunning = true
            isPaused = false
            // ✅ Clear any pending callbacks first
            handler.removeCallbacks(scrollRunnable)
            handler.postDelayed(scrollRunnable, interval)
        }
    }

    fun stopAutoScroll() {
        isRunning = false
        isPaused = false
        handler.removeCallbacks(scrollRunnable)
        // ✅ Clean up references to prevent memory leaks
        weakViewPager.clear()
    }

    fun pauseAutoScroll() {
        isPaused = true
        handler.removeCallbacks(scrollRunnable)
    }

    fun resumeAutoScroll() {
        if (isRunning && !isPaused) {
            // ✅ Only resume if we were running
            isPaused = false
            // ✅ Clear any pending callbacks first
            handler.removeCallbacks(scrollRunnable)
            handler.postDelayed(scrollRunnable, interval)
        } else if (isPaused) {
            // ✅ Resume from paused state
            isPaused = false
            handler.removeCallbacks(scrollRunnable)
            handler.postDelayed(scrollRunnable, interval)
        }
    }

    // ✅ Clean up method for when the ViewHolder is destroyed
    fun destroy() {
        stopAutoScroll()
        handler.removeCallbacksAndMessages(null)
        weakViewPager.clear()
    }

    // ✅ Check if auto-scroll is currently running
    fun isRunning(): Boolean = isRunning && !isPaused
}