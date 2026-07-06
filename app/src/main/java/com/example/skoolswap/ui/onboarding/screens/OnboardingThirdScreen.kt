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
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.common.constants.AppConstants.LogTags
import com.example.skoolswap.common.constants.ErrorConstantsHelper
import com.example.skoolswap.data.local.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
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
                    // Save onboarding as completed using AppConstants
                    appPreferences.setOnboardingFinished(true)

                    // Safe navigation
                    findNavController().navigate(
                        R.id.action_viewPagerFragment_to_loginFragment
                    )

                } catch (e: Exception) {
                    // Log with consistent tag
                    Timber.tag(LogTags.UI).e(e, "Error during onboarding completion")

                    // Show user-friendly error message
                    val errorMessage = ErrorConstantsHelper.getErrorMessage(e)
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()

                    // Fallback navigation
                    try {
                        findNavController().navigate(R.id.loginFragment)
                    } catch (ex: Exception) {
                        Timber.tag(LogTags.UI).e(ex, "Fallback navigation failed")
                    }
                }
            }
        }

        return view
    }
}