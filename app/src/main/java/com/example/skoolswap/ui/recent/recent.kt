package com.example.skoolswap.ui.recent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.skoolswap.R

class RecentFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Navigate immediately to ProductsFragment with recent filter
        val bundle = Bundle().apply {
            putString("SECTION_TYPE", "recent")
            putString("SECTION_TITLE", "Recently Added")
            putString("PERIOD", arguments?.getString("PERIOD") ?: "all")
        }
        findNavController().navigate(R.id.productsFragment, bundle)

        // Return empty view since we're navigating away
        return View(requireContext())
    }
}