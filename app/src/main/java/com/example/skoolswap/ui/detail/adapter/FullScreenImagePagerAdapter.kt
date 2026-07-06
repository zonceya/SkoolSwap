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
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.databinding.ItemFullScreenImageBinding
import timber.log.Timber

// Private constants - internal to this file only
private const val MAX_SCALE = 8f
private const val TAP_RESET_DELAY_MS = 300L
private const val IMAGE_OVERRIDE_SIZE = 1600

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
                    if (!isTapHandled) {
                        isTapHandled = true
                        onSingleTap()
                        binding.root.postDelayed({ isTapHandled = false }, TAP_RESET_DELAY_MS)
                    }
                    return true
                }
            }
        )

        init {
            binding.photoView.apply {
                setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
                setMaxScale(MAX_SCALE)
                setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
            }

            binding.photoView.setOnTouchListener { view, event ->
                val handled = gestureDetector.onTouchEvent(event)

                if (!handled) {
                    view.performClick()
                }

                false
            }
        }

        fun bind(url: String) {
            Timber.tag(LogTags.UI).d("Binding URL: $url")
            isTapHandled = false

            currentTarget?.let { Glide.with(binding.root.context).clear(it) }

            binding.photoView.recycle()

            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    Timber.tag(LogTags.UI).d("Bitmap ready for: $url, size: ${resource.width}x${resource.height}")
                    binding.photoView.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Timber.tag(LogTags.UI).e("Load failed for: $url")
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    Timber.tag(LogTags.UI).d("Load cleared for: $url")
                }
            }

            currentTarget = target

            Glide.with(binding.root.context)
                .asBitmap()
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .override(IMAGE_OVERRIDE_SIZE, IMAGE_OVERRIDE_SIZE)
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