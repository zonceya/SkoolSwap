package com.example.skoolswap.ui.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.skoolswap.R
import com.example.skoolswap.ui.main.MainViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OnboardingThirdScreen : Fragment() {

    // Use the shared MainViewModel (activity-scoped)
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_third_screen, container, false)
        val finishButton = view.findViewById<TextView>(R.id.finish)
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE

        finishButton.setOnClickListener {
            // Tell MainViewModel to finish onboarding
            mainViewModel.finishOnboarding()
        }

        return view
    }
}
