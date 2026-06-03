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

        // Keep a reference to cancel Glide on recycle
        private var currentTarget: CustomTarget<Bitmap>? = null

        private val gestureDetector = GestureDetector(
            binding.root.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    onSingleTap()
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

            binding.photoView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
                false // let SSIV still handle pan/zoom
            }
        }

        fun bind(url: String) {
            Timber.tag(TAG).d("Binding URL: $url")

            // Cancel any in-flight load for this holder
            currentTarget?.let { Glide.with(binding.root.context).clear(it) }

            // Reset SSIV state cleanly — do NOT call resetScaleAndCenter before setImage,
            // it can leave SSIV in an inconsistent internal state on recycled views
            binding.photoView.recycle()

            val target = object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    Timber.tag(TAG).d("Bitmap ready for: $url, size: ${resource.width}x${resource.height}")
                    // No isAttachedToWindow check — SSIV handles detached state gracefully
                    binding.photoView.setImage(ImageSource.bitmap(resource))
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Timber.tag(TAG).e("Load failed for: $url")
                    // Show placeholder state — SSIV doesn't show drawables so just leave recycled
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Do NOT call recycle() here — this fires during Glide's
                    // internal cleanup and the bitmap may still be in use by SSIV
                    Timber.tag(TAG).d("Load cleared for: $url")
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