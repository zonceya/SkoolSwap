package com.example.skoolswap.ui.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.example.skoolswap.ui.onboarding.screens.OnboardingFirstScreen
import com.example.skoolswap.ui.onboarding.screens.OnboardingSecondScreen
import com.example.skoolswap.ui.onboarding.screens.OnboardingThirdScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewPagerFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var dotIndicator: LinearLayout
    private lateinit var dots: Array<ImageView?>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_view_pager, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentList = arrayListOf<Fragment>(
            OnboardingFirstScreen(),
            OnboardingSecondScreen(),
            OnboardingThirdScreen()
        )

        val adapter = ViewPagerAdapter(
            fragmentList,
            childFragmentManager,
            lifecycle
        )

        viewPager = view.findViewById(R.id.viewPager)
        dotIndicator = view.findViewById(R.id.dotIndicator)

        viewPager.adapter = adapter

        // Setup dot indicators
        setupDotIndicators(fragmentList.size)

        // Set current dot when page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDotIndicators(position)
            }
        })

        // Handle back button
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewPager.currentItem == 0) {
                        findNavController().popBackStack()
                    } else {
                        viewPager.currentItem -= 1
                    }
                }
            }
        )
    }

    private fun setupDotIndicators(count: Int) {
        dots = arrayOfNulls(count)

        for (i in 0 until count) {
            val dot = ImageView(requireContext())
            val params = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.dot_size),
                resources.getDimensionPixelSize(R.dimen.dot_size)
            )
            params.setMargins(
                resources.getDimensionPixelSize(R.dimen.dot_margin),
                0,
                resources.getDimensionPixelSize(R.dimen.dot_margin),
                0
            )
            dot.layoutParams = params

            dot.setImageResource(R.drawable.dot_inactive)
            dotIndicator.addView(dot)
            dots[i] = dot
        }

        // Set first dot as active
        if (dots.isNotEmpty()) {
            dots[0]?.setImageResource(R.drawable.dot_active)
        }
    }

    private fun updateDotIndicators(position: Int) {
        dots.forEachIndexed { index, dot ->
            dot?.setImageResource(
                if (index == position) R.drawable.dot_active
                else R.drawable.dot_inactive
            )
        }
    }
}