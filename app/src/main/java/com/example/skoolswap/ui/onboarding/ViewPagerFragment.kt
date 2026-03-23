package com.example.skoolswap.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ViewPagerFragment : Fragment() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
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

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayout)
        viewPager.adapter = adapter
        TabLayoutMediator(tabLayout, viewPager) { _, _ ->
            // Optionally set tab titles here if needed
        }.attach()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // You can observe something here if needed
    }
}