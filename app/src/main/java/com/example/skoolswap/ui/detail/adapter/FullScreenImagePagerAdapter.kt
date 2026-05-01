package com.example.skoolswap.ui.detail.adapter

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemFullScreenImageBinding
class FullScreenImagePagerAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<FullScreenImagePagerAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFullScreenImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFullScreenImageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = imageUrls[position]

        // CRITICAL: Clear previous image before loading new one
        holder.binding.photoView.recycle()
        holder.binding.photoView.setImage(ImageSource.uri("")) // Clear current image

        // Load image with better caching strategy
        Glide.with(holder.itemView.context)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache both original and resized
            .skipMemoryCache(false) // Use memory cache
            .override(1600, 1600) // Limit max size to prevent OOM
            .placeholder(R.drawable.ic_create_item_placeholder)
            .error(R.drawable.ic_create_item_placeholder)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (holder.binding.photoView.isAttachedToWindow) {
                        holder.binding.photoView.setImage(ImageSource.bitmap(resource))
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Clear the view when load is cancelled
                    holder.binding.photoView.recycle()
                }
            })
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Clean up when view is recycled
        holder.binding.photoView.recycle()
        Glide.with(holder.itemView.context).clear(holder.binding.photoView)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        // Clean up when adapter is detached
        Glide.get(recyclerView.context).clearMemory()
    }

    override fun getItemCount(): Int = imageUrls.size
}