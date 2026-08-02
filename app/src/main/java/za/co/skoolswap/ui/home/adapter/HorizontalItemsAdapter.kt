package za.co.skoolswap.ui.home.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import za.co.skoolswap.R
import za.co.skoolswap.databinding.ItemHomeProductBinding
import za.co.skoolswap.domain.model.Item
import java.text.NumberFormat
import java.util.Locale

class HorizontalItemsAdapter(
    private val sectionType: String,
    private val onItemClick: (Item, String) -> Unit
) : ListAdapter<Item, HorizontalItemsAdapter.ViewHolder>(ItemDiffCallback()) {

    fun updateItems(newItems: List<Item>) {
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHomeProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(
        private val binding: ItemHomeProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position), sectionType)
                }
            }
        }

        fun bind(item: Item) {
            binding.productTitle.text = item.name
            binding.productPrice.text = formatPrice(item.price)

            val imageUrl = item.resolveImageUrl()
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .placeholder(R.drawable.ic_create_item_placeholder)
                            .error(R.drawable.ic_create_item_placeholder)
                            .fitCenter()
                    )
                    .into(binding.productImage)
            } else {
                binding.productImage.setImageResource(R.drawable.ic_create_item_placeholder)
            }

            val isSold = item.status == "sold" || item.quantity <= 0
            if (isSold) {
                binding.soldBadge.visibility = View.VISIBLE
                binding.productTitle.alpha = 0.6f
                binding.productPrice.alpha = 0.6f
            } else {
                binding.soldBadge.visibility = View.GONE
                binding.productTitle.alpha = 1f
                binding.productPrice.alpha = 1f
            }
        }

        private fun formatPrice(price: Double): String {
            return try {
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))
                formatter.currency = java.util.Currency.getInstance("ZAR")
                formatter.format(price)
            } catch (e: Exception) {
                "R${String.format("%.2f", price)}"
            }
        }
    }
}