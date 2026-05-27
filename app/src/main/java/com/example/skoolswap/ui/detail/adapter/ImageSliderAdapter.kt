package com.example.skoolswap.ui.detail.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemImageSliderBinding
import com.example.skoolswap.ui.detail.zoom.FullScreenImageViewerDialogFragment

private const val TAG = "ImageSliderAdapter"

class ImageSliderAdapter(
    val imageUrls: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    init {
        Log.d(TAG, "=== ImageSliderAdapter CREATED ===")
        Log.d(TAG, "Number of images: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            Log.d(TAG, "  Image $index: $url")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Log.d(TAG, "onCreateViewHolder called, viewType: $viewType")
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.d(TAG, "onBindViewHolder called for position: $position")
        val url = imageUrls[position]
        val isCover = position == 0
        Log.d(TAG, "Binding position $position - URL: $url, isCover: $isCover")
        holder.bind(url, position, isCover)
    }

    override fun getItemCount(): Int {
        val count = imageUrls.size
        Log.d(TAG, "getItemCount: $count")
        return count
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }

    inner class ViewHolder(
        private val binding: ItemImageSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            Log.d(TAG, "ViewHolder created with binding: ${binding.root.javaClass.simpleName}")
        }

        fun bind(url: String, position: Int, isCover: Boolean) {
            Log.d(TAG, "=== BINDING VIEWHOLDER START ===")
            Log.d(TAG, "Position: $position, URL: $url")
            Log.d(TAG, "Adapter position: $adapterPosition")
            Log.d(TAG, "Binding absolute position: $position")
            Log.d(TAG, "isCover: $isCover")

            // Clear any pending requests and cancel ongoing loads
            Log.d(TAG, "Clearing previous Glide requests for position $position")
            Glide.with(binding.root.context)
                .clear(binding.imageView)

            Log.d(TAG, "Starting new Glide load for URL: $url")
            Glide.with(binding.root.context)
                .load(url)
                .placeholder(R.drawable.ic_create_item_placeholder)
                .error(R.drawable.ic_create_item_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.e(TAG, "❌ Glide onLoadFailed for position $position")
                        Log.e(TAG, "  URL: $url")
                        Log.e(TAG, "  Exception: ${e?.message}")
                        e?.logRootCauses(TAG)
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Log.d(TAG, "✅ Glide onResourceReady for position $position")
                        Log.d(TAG, "  URL: $url")
                        Log.d(TAG, "  DataSource: $dataSource")
                        Log.d(TAG, "  Resource dimensions: ${resource?.minimumWidth}x${resource?.minimumHeight}")
                        return false
                    }
                })
                .into(binding.imageView)

            // Set click listener
            binding.imageView.setOnClickListener {
                Log.d(TAG, "Image clicked at position: $position")
                val transitionName = "image_transition_$position"
                ViewCompat.setTransitionName(binding.imageView, transitionName)
                Log.d(TAG, "Transition name set: $transitionName")

                val dialog = FullScreenImageViewerDialogFragment.newInstance(
                    imageUrls = imageUrls,
                    startingPosition = position,
                    transitionName = transitionName
                )
                Log.d(TAG, "Created FullScreenImageViewerDialogFragment with $position images")

                val fragment = binding.root.findFragment<androidx.fragment.app.Fragment>()
                if (fragment != null) {
                    Log.d(TAG, "Found parent fragment, showing dialog")
                    dialog.show(fragment.parentFragmentManager, "full_screen_viewer")
                } else {
                    Log.e(TAG, "Could not find parent fragment")
                }
            }

            Log.d(TAG, "=== BINDING VIEWHOLDER END ===")
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        Log.d(TAG, "onAttachedToRecyclerView called")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Log.d(TAG, "onDetachedFromRecyclerView called")
    }
}