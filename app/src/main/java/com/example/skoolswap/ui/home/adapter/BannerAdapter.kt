package com.example.skoolswap.ui.home.adapter

import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemBannerBinding
import com.example.skoolswap.domain.model.BannerItem

class BannerAdapter(
    private val bannerItems: List<BannerItem>
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private val maxRetryCount = 3
    private val retryDelayMs = 2000L

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BannerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val actualPosition = position % bannerItems.size
        holder.bind(bannerItems[actualPosition])
    }

    override fun getItemCount() = Int.MAX_VALUE

    inner class BannerViewHolder(
        private val binding: ItemBannerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var retryCount = 0
        val handler = Handler(Looper.getMainLooper())
        private var currentUrl = ""

        fun bind(bannerItem: BannerItem) {
            retryCount = 0
            currentUrl = bannerItem.imageUrl

            // Handle title
            if (!bannerItem.title.isNullOrEmpty()) {
                binding.bannerTitle.text = bannerItem.title
                binding.bannerTitle.visibility = View.VISIBLE
                binding.gradientOverlay.visibility = View.VISIBLE
            } else {
                binding.bannerTitle.visibility = View.GONE
                binding.gradientOverlay.visibility = View.GONE
            }

            loadImage(bannerItem.imageUrl)
        }

        private fun loadImage(url: String) {
            Glide.with(binding.root.context)
                .load(url)
                .override(397, 262)
                .centerCrop()
                // Glide automatically handles ETag/Last-Modified with these settings:
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache image + headers
                .skipMemoryCache(false) // Keep in memory for speed
                .placeholder(R.drawable.banner_placeholder)
                .error(R.drawable.banner_error)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (retryCount < maxRetryCount) {
                            retryCount++
                            handler.postDelayed({
                                loadImage(url)
                            }, retryDelayMs * retryCount)
                            return true
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        retryCount = 0
                        return false
                    }
                })
                .into(binding.bannerImage)
        }
    }

    override fun onViewRecycled(holder: BannerViewHolder) {
        super.onViewRecycled(holder)
        holder.handler.removeCallbacksAndMessages(null)
    }
}