package com.example.skoolswap.ui.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R
import com.example.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingThirdScreen : Fragment() {

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_third_screen, container, false)
        val finishButton = view.findViewById<TextView>(R.id.finish)

        finishButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    // Save onboarding as completed
                    appPreferences.setOnboardingFinished(true)

                    // Safe navigation
                    findNavController().navigate(
                        R.id.action_viewPagerFragment_to_loginFragment
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fallback navigation
                    try {
                        findNavController().navigate(R.id.loginFragment)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
            }
        }

        return view
    }
}