package com.example.skoolswap.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.example.skoolswap.domain.model.BannerItem
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.ui.home.adapter.BannerAdapter
import com.example.skoolswap.ui.home.adapter.HomeFeedAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.viewpager2.widget.ViewPager2

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeAdapter: HomeFeedAdapter
    private lateinit var bannerAdapter: BannerAdapter
    private lateinit var autoScrollHelper: BannerAutoScrollHelper

    private val bannerItems = listOf(
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_1.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_2.jpg"),
        BannerItem(imageUrl = "https://cdn.skoolswap.co.za/banners/home_3.jpg")
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
        setupRecyclerView()
        observeViewModel()

        return binding.root
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeFeedAdapter(
            onItemClick = { item, source ->
                // Navigate to item detail
                navigateToItemDetail(item.id, source)
            },
            onViewAllClick = { sectionType ->
                // Navigate to full category view
                when (sectionType) {
                    "recommended" -> navigateToRecommendedAll()
                    "trending" -> navigateToTrendingAll()
                    "recent" -> navigateToRecentAll()
                }
            }
        )

        binding.homeRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeAdapter
        }
    }

    private fun setupCustomTabs() {
        val tabs = listOf(
            binding.tabHome,
            binding.tabUniform,
            binding.tabSport,
            binding.tabRecent
        )

        tabs.forEach { tab ->
            tab.setOnClickListener {
                selectTab(tab)
            }
        }

        // Select HOME tab by default
        selectTab(binding.tabHome)
    }

    private fun selectTab(selectedTab: TextView) {
        val tabs = listOf(
            binding.tabHome,
            binding.tabUniform,
            binding.tabSport,
            binding.tabRecent
        )

        tabs.forEach { tab ->
            if (tab == selectedTab) {
                tab.setBackgroundResource(R.drawable.tablayout_selector)
                tab.setTextColor(resources.getColor(android.R.color.white, null))
                tab.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                tab.setBackgroundResource(R.drawable.tablayout_unselected)
                tab.setTextColor(resources.getColor(android.R.color.black, null))
                tab.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }

        // Load content based on selected tab
        when (selectedTab.id) {
            R.id.tabHome -> {
                viewModel.loadHomeFeed()
            }
            R.id.tabUniform -> {
                // Navigate to Uniform fragment or show filter
                navigateToUniformTab()
            }
            R.id.tabSport -> {
                // Navigate to Sport fragment
                navigateToSportTab()
            }
            R.id.tabRecent -> {
                // Navigate to Recent fragment
                navigateToRecentTab()
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.homeFeed.collect { feed ->
                feed?.let {
                    homeAdapter.submitList(it.sections)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                // Show/hide loading
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

        autoScrollHelper = BannerAutoScrollHelper(binding.bannerViewPager, 8000)
        autoScrollHelper.startAutoScroll()
    }

    private fun setupIndicatorDots() {
        binding.indicatorDots.removeAllViews()

        bannerItems.forEachIndexed { index, _ ->
            val dot = ImageView(requireContext()).apply {
                setImageResource(
                    if (index == 0) R.drawable.dot_active
                    else R.drawable.dot_inactive
                )
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 0, 8, 0)
            }
            binding.indicatorDots.addView(dot)
        }
    }

    private fun updateIndicatorDots(currentIndex: Int) {
        for (i in 0 until binding.indicatorDots.childCount) {
            val dot = binding.indicatorDots.getChildAt(i) as ImageView
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

    private fun navigateToItemDetail(itemId: String, source: String) {
        // Navigation logic
    }

    private fun navigateToUniformTab() {
        // Navigate to UniformFragment
    }

    private fun navigateToSportTab() {
        // Navigate to SportFragment
    }

    private fun navigateToRecentTab() {
        // Navigate to RecentFragment
    }

    private fun navigateToRecommendedAll() {
        // Navigate to full recommended list
    }

    private fun navigateToTrendingAll() {
        // Navigate to full trending list
    }

    private fun navigateToRecentAll() {
        // Navigate to full recent list
    }
}