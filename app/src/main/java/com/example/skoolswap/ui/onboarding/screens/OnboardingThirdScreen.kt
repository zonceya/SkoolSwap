package com.example.skoolswap.ui.onboarding.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding_third_screen, container, false)
        val finishButton = view.findViewById<TextView>(R.id.finish)

        // Debug logging
        android.util.Log.d("OnboardingThird", "onCreateView called")

        if (finishButton == null) {
            android.util.Log.e("OnboardingThird", "Finish button not found! Check ID in layout")
            Toast.makeText(requireContext(), "Error: Finish button not found", Toast.LENGTH_LONG).show()
        } else {
            android.util.Log.d("OnboardingThird", "Finish button found, setting click listener")

            finishButton.setOnClickListener {
                android.util.Log.d("OnboardingThird", "Finish button clicked!")

                lifecycleScope.launch {
                    try {
                        // Save that onboarding is finished
                        appPreferences.setOnboardingFinished(true)
                        android.util.Log.d("OnboardingThird", "Onboarding saved as finished")

                        // Use the existing navigation action from nav_graph
                        // This will navigate to loginFragment
                        findNavController().navigate(
                            R.id.action_viewPagerFragment_to_loginFragment
                        )

                        android.util.Log.d("OnboardingThird", "Navigation to login executed")

                    } catch (e: Exception) {
                        android.util.Log.e("OnboardingThird", "Error during navigation", e)
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("OnboardingThird", "onResume called - Fragment is visible")
    }
}