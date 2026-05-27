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
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
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
        binding.photoView.apply {
            // Allow panning and zooming but let ViewPager2 handle horizontal swipes at min zoom
            setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CENTER_INSIDE)
            setMaxScale(8f)
            setPanLimit(SubsamplingScaleImageView.PAN_LIMIT_INSIDE)
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val url = imageUrls[position]

        // Reset zoom to default before loading new image
        holder.binding.photoView.resetScaleAndCenter()

        // Clear any previous load
        Glide.with(holder.itemView.context).clear(holder.binding.photoView)

        Glide.with(holder.itemView.context)
            .asBitmap()
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .skipMemoryCache(false)
            .override(1600, 1600)
            .placeholder(R.drawable.ic_create_item_placeholder)
            .error(R.drawable.ic_create_item_placeholder)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    if (holder.binding.photoView.isAttachedToWindow) {
                        holder.binding.photoView.setImage(ImageSource.bitmap(resource))
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    holder.binding.photoView.recycle()
                }
            })
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.photoView.recycle()
        Glide.with(holder.itemView.context).clear(holder.binding.photoView)
    }

    override fun getItemCount(): Int = imageUrls.size
}