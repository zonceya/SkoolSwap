// ui/home/BannerAdapter.kt
package com.example.skoolswap.ui.home

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemBannerBinding
import com.bumptech.glide.load.engine.DiskCacheStrategy

class BannerAdapter(
    private val bannerItems: List<BannerItem>
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    private val maxRetryCount = 3
    private val retryDelayMs = 2000L // 2 seconds

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
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
        private val handler = Handler(Looper.getMainLooper())

        fun bind(bannerItem: BannerItem) {
            // Reset retry count for new bind
            retryCount = 0

            val imageUrlWithCacheBuster = "${bannerItem.imageUrl}?v=${System.currentTimeMillis()}"

            // Handle title
            if (!bannerItem.title.isNullOrEmpty()) {
                binding.bannerTitle.text = bannerItem.title
                binding.bannerTitle.visibility = View.VISIBLE
                binding.gradientOverlay.visibility = View.VISIBLE
            } else {
                binding.bannerTitle.visibility = View.GONE
                binding.gradientOverlay.visibility = View.GONE
            }

            // Load image with retry logic
            loadImageWithRetry(imageUrlWithCacheBuster)
        }

        private fun loadImageWithRetry(imageUrl: String) {
            Glide.with(binding.root.context)
                .load(imageUrl)
                .placeholder(R.drawable.banner_placeholder)
                .error(R.drawable.banner_error)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .override(397, 262)
                .centerCrop()
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e("BannerAdapter", "Failed to load: $imageUrl (attempt ${retryCount + 1}/$maxRetryCount)")

                        if (retryCount < maxRetryCount) {
                            retryCount++

                            // Show retry message on placeholder
                            binding.bannerImage.setImageResource(R.drawable.banner_retry)

                            // Retry after delay
                            handler.postDelayed({
                                Log.d("BannerAdapter", "Retrying: $imageUrl (attempt $retryCount/$maxRetryCount)")
                                loadImageWithRetry(imageUrl)
                            }, retryDelayMs)

                            return true
                        } else {
                            // Max retries reached, show error
                            Log.e("BannerAdapter", "Failed after $maxRetryCount attempts: $imageUrl")
                            binding.bannerImage.setImageResource(R.drawable.banner_error)
                            return false
                        }
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d("BannerAdapter", "Successfully loaded: $imageUrl after ${retryCount} retries")
                        retryCount = 0 // Reset on success
                        return false
                    }
                })
                .into(binding.bannerImage)
        }
    }
}