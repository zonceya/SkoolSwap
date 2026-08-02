package za.co.skoolswap.ui.detail.adapter

import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment  // ✅ ADD THIS IMPORT
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import za.co.skoolswap.R
import za.co.skoolswap.common.constants.AppConstants.LogTags
import za.co.skoolswap.databinding.ItemImageSliderBinding
import za.co.skoolswap.ui.detail.zoom.FullScreenImageViewerDialogFragment
import timber.log.Timber

class ImageSliderAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    init {
        Timber.tag(LogTags.UI).d("=== ImageSliderAdapter CREATED ===")
        Timber.tag(LogTags.UI).d("Number of images: ${imageUrls.size}")
        imageUrls.forEachIndexed { index, url ->
            Timber.tag(LogTags.UI).d("  Image $index: $url")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        Timber.tag(LogTags.UI).d("onCreateViewHolder called, viewType: $viewType")
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(imageUrls[position], position)
    }

    override fun getItemCount(): Int = imageUrls.size

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.clear()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        try {
            Glide.get(recyclerView.context).clearMemory()
        } catch (e: Exception) {
            Timber.tag(LogTags.UI).e("Error clearing Glide memory: ${e.message}")
        }
    }

    inner class ViewHolder(
        private val binding: ItemImageSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentPosition: Int = 0
        private var currentUrl: String? = null

        private val gestureDetector = GestureDetector(
            binding.root.context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent) = true
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val transitionName = "image_transition_$currentPosition"
                    ViewCompat.setTransitionName(binding.imageView, transitionName)

                    val dialog = FullScreenImageViewerDialogFragment.newInstance(
                        imageUrls = imageUrls,
                        startingPosition = currentPosition,
                        transitionName = transitionName
                    )

                    try {
                        // ✅ Now works with the import
                        val fragment = binding.root.findFragment<Fragment>()
                        fragment?.parentFragmentManager?.let { fm ->
                            if (fm.findFragmentByTag("full_screen_viewer") == null) {
                                dialog.show(fm, "full_screen_viewer")
                            }
                        } ?: run {
                            Timber.tag(LogTags.UI).e("Could not find parent fragment")
                        }
                    } catch (e: IllegalStateException) {
                        Timber.tag(LogTags.UI).e("Could not find parent fragment: ${e.message}")
                    }

                    return true
                }
            }
        )

        init {
            binding.imageView.setOnTouchListener { _, event ->
                gestureDetector.onTouchEvent(event)
            }
        }

        fun bind(url: String, position: Int) {
            if (currentUrl == url && currentPosition == position) {
                Timber.tag(LogTags.UI).d("Skipping bind - same URL: $url")
                return
            }

            currentPosition = position
            currentUrl = url
            Timber.tag(LogTags.UI).d("📸 Binding image $position: $url")

            Glide.with(binding.root.context).clear(binding.imageView)

            Glide.with(binding.root.context)
                .load(url)
                .placeholder(R.drawable.ic_create_item_placeholder)
                .error(R.drawable.ic_create_item_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .fitCenter()
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?, model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (currentUrl == url) {
                            Timber.tag(LogTags.UI).e("❌ Load failed pos $position: ${e?.message}")
                        }
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?, model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (currentUrl == url) {
                            Timber.tag(LogTags.UI).d("✅ Loaded pos $position from $dataSource")
                        }
                        return false
                    }
                })
                .into(binding.imageView)
        }

        fun clear() {
            try {
                Glide.with(binding.root.context).clear(binding.imageView)
            } catch (e: Exception) {
                Timber.tag(LogTags.UI).e("Error clearing image: ${e.message}")
            }
            currentUrl = null
        }
    }
}