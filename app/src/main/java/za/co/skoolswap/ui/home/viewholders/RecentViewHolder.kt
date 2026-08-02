package za.co.skoolswap.ui.home.viewholders

import android.content.res.Configuration
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import za.co.skoolswap.R
import za.co.skoolswap.databinding.ItemRecentRowBinding
import za.co.skoolswap.domain.model.Item
import za.co.skoolswap.domain.model.homefeed.Section
import za.co.skoolswap.ui.home.adapter.HorizontalItemsAdapter

class RecentViewHolder(
    private val binding: ItemRecentRowBinding,
    private val onItemClick: (Item, String) -> Unit,
    private val onViewAllClick: (String) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private val itemsAdapter = HorizontalItemsAdapter(
        sectionType = "recent",
        onItemClick = onItemClick
    )

    init {
        binding.recentRecycler.apply {
            layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = itemsAdapter
            setHasFixedSize(true)
        }

        binding.root.findViewById<TextView>(R.id.viewAll)?.setOnClickListener {
            onViewAllClick("recent")
        }
    }

    fun bind(section: Section.Recent) {
        val sectionTitle = binding.root.findViewById<TextView>(R.id.sectionTitle)
        val viewAll = binding.root.findViewById<TextView>(R.id.viewAll)

        sectionTitle?.text = section.title

        val isDarkMode = (itemView.context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val textColor = if (isDarkMode) {
            ContextCompat.getColor(itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(itemView.context, R.color.black)
        }

        sectionTitle?.setTextColor(textColor)
        viewAll?.setTextColor(textColor)

        itemsAdapter.updateItems(section.items)
    }
}