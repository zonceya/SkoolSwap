// ui/home/HomeFragment.kt
package com.example.skoolswap.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.example.skoolswap.domain.model.BannerItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var autoScrollHelper: BannerAutoScrollHelper

    private val bannerItems = listOf(
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_1.jpg",
            title = null
        ),
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_2.jpg",
            title = null
        ),
        BannerItem(
            imageUrl = "https://cdn.skoolswap.co.za/banners/home_3.jpg",
            title = null
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.VISIBLE

        setupCustomTabs()
        setupBannerSlider()
        setupIndicatorDots()

        return binding.root
    }
    private fun selectTab(selectedTab: TextView) {
        val tabs = listOf(
            binding.tabHome,
            binding.tabShops,
            binding.tabSchools,
            binding.tabSales
        )

        tabs.forEach { tab ->
            if (tab == selectedTab) {
                // Selected tab - Black background, White text, Bold
                tab.setBackgroundResource(R.drawable.tablayout_selector) // You need this drawable
                tab.setTextColor(resources.getColor(android.R.color.white, null))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                // Unselected tab - Transparent background, Black text, Normal
                tab.setBackgroundResource(R.drawable.tablayout_unselected) // You need this drawable
                tab.setTextColor(resources.getColor(android.R.color.black, null))
                tab.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        // Handle content switching
        when (selectedTab.id) {
            R.id.tabHome -> {
                Log.d("HomeFragment", "Home Selected")
                // Load home content
            }
            R.id.tabShops -> {
                Log.d("HomeFragment", "Shops Selected")
            }
            R.id.tabSchools -> {
                Log.d("HomeFragment", "Schools Selected")
            }
            R.id.tabSales -> {
                Log.d("HomeFragment", "Sales Selected")
            }
        }
    }
    // In your HomeFragment.kt
    // ui/home/HomeFragment.kt
    private fun setupCustomTabs() {

        val tabs = listOf(
            binding.tabHome,
            binding.tabShops,
            binding.tabSchools,
            binding.tabSales
        )

        tabs.forEach { tab ->
            tab.setOnClickListener {
                selectTab(tab)
            }
        }
    }

    private fun setupBannerSlider() {
        bannerAdapter = BannerAdapter(bannerItems)

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

        // Setup auto-scroll
        autoScrollHelper = BannerAutoScrollHelper(binding.bannerViewPager, 8000)

        // Pause on touch
        binding.bannerViewPager.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    autoScrollHelper.pauseAutoScroll()
                    false
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    autoScrollHelper.resumeAutoScroll()
                    false
                }
                else -> false
            }
        }

        autoScrollHelper.startAutoScroll()
    }

    private fun setupIndicatorDots() {
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

    override fun onResume() {
        super.onResume()
        autoScrollHelper?.resumeAutoScroll()
    }

    override fun onPause() {
        super.onPause()
        autoScrollHelper?.pauseAutoScroll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollHelper?.stopAutoScroll()
        _binding = null
    }
}