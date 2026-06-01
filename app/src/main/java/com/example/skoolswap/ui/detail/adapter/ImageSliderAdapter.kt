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
import timber.log.Timber

private const val TAG = "ImageSliderAdapter"

class ImageSliderAdapter(
    val imageUrls: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    init {
        Timber.tag(TAG).d("=== ImageSliderAdapter CREATED ===")
        Timber.tag(TAG).d("Number of images: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            Timber.tag(TAG).d("  Image $index: $url")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.tag(TAG).d("onCreateViewHolder called, viewType: $viewType")
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageUrls[position], position)
    }

    override fun getItemCount(): Int {
        val count = imageUrls.size
        Timber.tag(TAG).d("getItemCount: $count")
        return count
    }


    inner class ViewHolder(
        private val binding: ItemImageSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            Timber.tag(TAG)
                .d("ViewHolder created with binding: ${binding.root.javaClass.simpleName}")
        }

        // AFTER
        fun bind(url: String, position: Int) {
            Glide.with(binding.root.context).clear(binding.imageView)

            Glide.with(binding.root.context)
                .load(url)
                .placeholder(R.drawable.ic_create_item_placeholder)
                .error(R.drawable.ic_create_item_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .centerCrop()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.tag(TAG).e("Load failed pos $position: ${e?.message}")
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?, model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        Timber.tag(TAG).d("Loaded pos $position from $dataSource")
                        return false
                    }
                })
                .into(binding.imageView)

            binding.imageView.setOnClickListener {
                val transitionName = "image_transition_$position"
                ViewCompat.setTransitionName(binding.imageView, transitionName)

                val dialog = FullScreenImageViewerDialogFragment.newInstance(
                    imageUrls = imageUrls,
                    startingPosition = position,
                    transitionName = transitionName
                )

                val fragment = binding.root.findFragment<androidx.fragment.app.Fragment>()
                if (fragment != null) {
                    dialog.show(fragment.parentFragmentManager, "full_screen_viewer")
                } else {
                    Timber.tag(TAG).e("Could not find parent fragment")
                }
            }
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        Log.d(TAG, "onAttachedToRecyclerView called")
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Timber.tag(TAG).d("onDetachedFromRecyclerView called")
    }
}