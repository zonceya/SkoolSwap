// ui/profile/adapter/SchoolAdapter.kt
package com.example.skoolswap.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.skoolswap.databinding.ItemSchoolBinding
import com.example.skoolswap.domain.model.School

class SchoolAdapter(
    private val onItemClick: (School) -> Unit
) : ListAdapter<School, SchoolAdapter.SchoolViewHolder>(SchoolDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val binding = ItemSchoolBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SchoolViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SchoolViewHolder(
        private val binding: ItemSchoolBinding,
        private val onItemClick: (School) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(school: School) {
            binding.textSchoolName.text = school.name
            binding.textSchoolLocation.text = school.provinceName ?: "Location not specified"

            binding.root.setOnClickListener {
                onItemClick(school)
            }
        }
    }

    class SchoolDiffCallback : DiffUtil.ItemCallback<School>() {
        override fun areItemsTheSame(oldItem: School, newItem: School): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: School, newItem: School): Boolean {
            return oldItem == newItem
        }
    }
}