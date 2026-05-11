package com.example.skoolswap.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.ui.onboarding.screens.OnboardingFirstScreen
import com.example.skoolswap.ui.onboarding.screens.OnboardingSecondScreen
import com.example.skoolswap.ui.onboarding.screens.OnboardingThirdScreen
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewPagerFragment : Fragment() {

    @Inject
    lateinit var appPreferences: AppPreferences
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_view_pager, container, false)

        val fragmentList = arrayListOf<Fragment>(
            OnboardingFirstScreen(),
            OnboardingSecondScreen(),
            OnboardingThirdScreen()
        )

        val adapter = ViewPagerAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        viewPager = view.findViewById(R.id.viewPager)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager.adapter = adapter
        // Setup tab indicators with dots
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.customView = null
            // Set initial icons
            when (position) {
                0 -> tab.setIcon(R.drawable.dot_inactive)
                1 -> tab.setIcon(R.drawable.dot_inactive)
                2 -> tab.setIcon(R.drawable.dot_active)
            }
        }.attach()

        // Update dots when swiping
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateTabIcons(position)
            }
        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val callback = object : OnBackPressedCallback(true) { // true = callback is enabled
            override fun handleOnBackPressed() {
                if (viewPager.currentItem == 0) {
                    // On first screen, go back to login
                    findNavController().popBackStack()
                } else {
                    // Go to previous onboarding screen
                    viewPager.currentItem -= 1
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            callback
        )
    }
    private fun updateTabIcons(currentPosition: Int) {
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            when {
                i == currentPosition -> tab?.setIcon(R.drawable.dot_active)
                else -> tab?.setIcon(R.drawable.dot_inactive)
            }
        }
    }
}