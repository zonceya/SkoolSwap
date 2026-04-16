package com.example.skoolswap.ui.detail.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ItemImageSliderBinding
import com.example.skoolswap.domain.model.ItemImage

class ImageSliderAdapter(
    private val images: List<ItemImage>
) : RecyclerView.Adapter<ImageSliderAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemImageSliderBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    class ViewHolder(
        private val binding: ItemImageSliderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: ItemImage) {
            Glide.with(binding.root.context)
                .load(image.url)
                .placeholder(R.drawable.ic_create_item_placeholder  )
                .error(R.drawable.ic_create_item_placeholder)
                .centerCrop()
                .into(binding.imageView)
        }
    }
}