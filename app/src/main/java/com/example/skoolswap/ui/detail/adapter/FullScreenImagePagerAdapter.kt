package com.example.skoolswap.ui.detail.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemFullScreenImageBinding
import timber.log.Timber

private const val TAG = "FullScreenPagerAdapter"

class FullScreenImagePagerAdapter(
    private val imageUrls: List<String>,
    private val onSingleTap: () -> Unit = {}
) : RecyclerView.Adapter<FullScreenImagePagerAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFullScreenImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentTarget: CustomTarget<Bitmap>? = null
        private var isTapHandled = false

        private val gestureDetector = GestureDetector(
            binding.root.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true

                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    // ✅ Only trigger if we're not mid-dismissal
                    if (!isTapHandled) {
                        isTapHandled = true
                        onSingleTap()
                        // Reset after a short delay to allow re-tap if needed
                        binding.root.postDelayed({ isTapHandled = false }, 300)
                    }
                    return true
                }
            }
        )

        init {
            binding.photoView.apply {
                setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                setMaxScale(8f)
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            }

            // ✅ Combined touch handling
            binding.photoView.setOnTouchListener { view, event ->
                // Let gesture detector process the event first
                val handled = gestureDetector.onTouchEvent(event)

                // If gesture detector didn't handle it (e.g., pan/zoom), let SSIV handle it
                if (!handled) {
                    // SSIV will handle pan/zoom gestures
                    view.performClick()
                }

                // Return false to let SSIV continue processing for pan/zoom
                false
            }
        }

        fun bind(url: String) {
            Timber.tag(TAG).d("Binding URL: $url")
            isTapHandled = false  // ✅ Reset tap state when binding new image

            // Cancel any in-flight load for this holder
            currentTarget?.let { Glide.with(binding.root.context).clear(it) }

            // Reset SSIV state cleanly
            binding.photoView.recycle()

            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    Timber.tag(TAG).d("Bitmap ready for: $url, size: ${resource.width}x${resource.height}")
                    binding.photoView.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Timber.tag(TAG).e("Load failed for: $url")
                    // Could set a placeholder drawable if needed
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Timber.tag(TAG).d("Load cleared for: $url")
                    // Don't call recycle() - SSIV may still be using the bitmap
                }
            }

            currentTarget = target

            Glide.with(binding.root.context)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .override(1600, 1600)
                .placeholder(R.drawable.ic_create_item_placeholder)
                .error(R.drawable.ic_create_item_placeholder)
                .into(target)
        }

        fun clear() {
            currentTarget?.let { Glide.with(binding.root.context).clear(it) }
            currentTarget = null
            binding.photoView.recycle()
            isTapHandled = false
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFullScreenImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageUrls[position])
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    override fun getItemCount() = imageUrls.size
}