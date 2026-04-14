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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.databinding.FragmentHomeBinding
import com.example.skoolswap.domain.model.BannerItem
import com.example.skoolswap.domain.model.Item
import com.example.skoolswap.ui.home.adapter.BannerAdapter
import com.example.skoolswap.ui.home.adapter.HomeFeedAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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

        // Only load home feed if we don't have cached data
        if (viewModel.homeFeed.value == null) {
            viewModel.loadHomeFeed()
        }

        return binding.root
    }

    private fun setupRecyclerView() {
        homeAdapter = HomeFeedAdapter(
            onItemClick = { item, source ->
                navigateToItemDetail(item.id, source)
            },
            onViewAllClick = { sectionType ->
                when (sectionType) {
                    "recommended" -> {
                        navigateToProducts("recommended", "Recommended For You", null, null)
                    }
                    "essentials" -> {
                        navigateToProducts("essentials", "School Essentials", null, null)
                    }
                    "trending" -> {
                        navigateToProducts("trending", "Trending", "today", null)
                    }
                    "recent" -> {
                        navigateToProducts("recent", "Recently Added", "all", null)
                    }
                }
            }
        )

        binding.homeRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = homeAdapter
        }
    }

    private fun navigateToProducts(
        sectionType: String,
        title: String,
        period: String? = null,
        categoryId: Int? = null
    ) {
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", sectionType)
            putString("SECTION_TITLE", title)
            period?.let { putString("PERIOD", it) }
            if (categoryId != null) {
                putInt("CATEGORY_ID", categoryId)
            }
        }

        findNavController().navigate(
            R.id.action_homeFragment_to_productsFragment,
            bundle
        )
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

        when (selectedTab.id) {
            R.id.tabHome -> {
                // Only reload if we're not already on Home or if data is empty
                // This prevents unnecessary reloads when coming back from navigation
                if (viewModel.homeFeed.value == null) {
                    viewModel.loadHomeFeed()
                }
            }
            R.id.tabUniform -> {
                navigateToUniformTab()
            }
            R.id.tabSport -> {
                navigateToSportTab()
            }
            R.id.tabRecent -> {
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
        findNavController().navigate(R.id.action_homeFragment_to_uniformFragment)
    }

    private fun navigateToSportTab() {
        findNavController().navigate(R.id.action_homeFragment_to_sportFragment)
    }

    private fun navigateToRecentTab() {
        navigateToProducts("recent", "Recently Added", "all", null)
    }
}