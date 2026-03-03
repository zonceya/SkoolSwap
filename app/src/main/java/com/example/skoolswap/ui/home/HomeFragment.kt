// ui/home/HomeFragment.kt
package com.example.skoolswap.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

// Simple data class for banner items
data class BannerItem(
    val imageUrl: String,
    val title: String? = null
)

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // Your banner items with titles
    private val bannerItems = listOf(
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_1.png",
            title = "Pre Sold School Items"
        ),
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_2.jpg",
            title = null  // No text
        ),
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_3.jpg",
            title = null
        )
    )

    private lateinit var bannerAdapter: BannerAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.VISIBLE

        // Setup banner slider
        setupBannerSlider()
        setupIndicatorDots()
       // setupTabLayout()

        return binding.root
    }
    // In your HomeFragment.kt, inside setupTabLayout()


    /*private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Get the text view from tab and set appearance
                val textView = tab?.view?.findViewById<TextView>(androidx.appcompat.R.id.textView)
                textView?.setTextAppearance(requireContext(), R.style.CustomTabTextSelected)

                when (tab?.position) {
                    0 -> Log.d("HomeFragment", "Home selected")
                    1 -> Log.d("HomeFragment", "Shops selected")
                    2 -> Log.d("HomeFragment", "Schools selected")
                    3 -> Log.d("HomeFragment", "Sales selected")
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                val textView = tab?.view?.findViewById<TextView>(androidx.appcompat.R.id.textView)
                textView?.setTextAppearance(requireContext(), R.style.CustomTabText)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Optional
            }
        })
    }*/
    private fun setupBannerSlider() {
        bannerAdapter = BannerAdapter(bannerItems)

        // Make ViewPager visible
        binding.bannerViewPager.visibility = View.VISIBLE

        binding.bannerViewPager.apply {
            adapter = bannerAdapter
            offscreenPageLimit = 1
            setCurrentItem(Int.MAX_VALUE / 2, false)

            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    updateIndicatorDots(position % bannerItems.size)
                }
            })
        }
    }

    private fun setupIndicatorDots() {
        // Make indicator layout visible
        binding.indicatorDots.visibility = View.VISIBLE

        // Create indicator dots based on number of banners
        binding.indicatorDots.removeAllViews()

        bannerItems.forEachIndexed { index, _ ->
            val dot = android.widget.ImageView(requireContext()).apply {
                setImageResource(
                    if (index == 0) R.drawable.dot_active
                    else R.drawable.dot_inactive
                )
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 0, 8, 0)
            }
            binding.indicatorDots.addView(dot)
        }
    }

    private fun updateIndicatorDots(currentIndex: Int) {
        for (i in 0 until binding.indicatorDots.childCount) {
            val dot = binding.indicatorDots.getChildAt(i) as android.widget.ImageView
            dot.setImageResource(
                if (i == currentIndex) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}