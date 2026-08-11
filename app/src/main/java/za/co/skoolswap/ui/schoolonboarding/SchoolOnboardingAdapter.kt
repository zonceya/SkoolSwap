package za.co.skoolswap.ui.schoolonboarding

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import za.co.skoolswap.R
import za.co.skoolswap.domain.model.School

class SchoolOnboardingAdapter (
    private val onSchoolClick: (School) -> Unit
) : RecyclerView.Adapter<SchoolOnboardingAdapter.SchoolViewHolder>() {

    private var schools: List<School> = emptyList()

    // Update the list of schools
    fun submitList(newSchools: List<School>) {
        schools = newSchools
        notifyDataSetChanged()
    }

    // Create new view holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchoolViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_school, parent, false)
        return SchoolViewHolder(view, onSchoolClick)
    }

    // Bind data to view holder
    override fun onBindViewHolder(holder: SchoolViewHolder, position: Int) {
        holder.bind(schools[position])
    }

    // Total number of schools
    override fun getItemCount(): Int = schools.size

    // ViewHolder class
    class SchoolViewHolder(
        itemView: View,
        private val onSchoolClick: (School) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val schoolLogo: ImageView = itemView.findViewById(R.id.schoolLogo)
        private val schoolName: TextView = itemView.findViewById(R.id.onboaringschoolName)
        private val schoolType: TextView = itemView.findViewById(R.id.onboaringschoolType)

        fun bind(school: School) {
            // Set school name
            schoolName.text = school.name

            // Set school type
            schoolType.text = school.schoolType ?: "School"

            // Load school logo
            if (!school.logoUrl.isNullOrEmpty()) {
                // Use Glide to load the image from URL
                Glide.with(itemView.context)
                    .load(school.logoUrl)
                    .placeholder(R.drawable.ic_school_placeholder)  // Show while loading
                    .error(R.drawable.ic_school_placeholder)       // Show if error
                    .circleCrop()                                   // Make it circular
                    .into(schoolLogo)
                schoolLogo.visibility = View.VISIBLE
            } else {
                // No logo, show placeholder
                schoolLogo.setImageResource(R.drawable.ic_school_placeholder)
                schoolLogo.visibility = View.VISIBLE
            }

            // Click listener
            itemView.setOnClickListener {
                onSchoolClick(school)
            }
        }
    }
}