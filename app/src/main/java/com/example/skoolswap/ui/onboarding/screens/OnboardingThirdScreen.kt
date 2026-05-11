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
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
    ): View {
        val view = inflater.inflate(R.layout.fragment_onboarding_third_screen, container, false)
        val finishButton = view.findViewById<TextView>(R.id.finish)

        // Safe FAB hiding
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE

        finishButton.setOnClickListener {
            lifecycleScope.launch {
                appPreferences.setOnboardingFinished(true)

                // DON'T navigate to login - just go back in the stack
                // This returns to LoginFragment, which will see onboarding is done
                findNavController().popBackStack()
            }
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Safe FAB showing
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.VISIBLE
    }
}