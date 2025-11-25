package com.example.skoolswap.ui.onboarding.screens

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.skoolswap.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OnboardingSecondScreen : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view =  inflater.inflate(R.layout.fragment_onboarding_second_screen, container, false)

        val viewPager =  activity?.findViewById<ViewPager2>(R.id.viewPager)
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)

        // Hide the FAB
        fab?.visibility = View.GONE


        return view
    }

}