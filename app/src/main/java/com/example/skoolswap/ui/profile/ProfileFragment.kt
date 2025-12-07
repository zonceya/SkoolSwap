package com.example.sekeni.ui.profile

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.sekeni.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFragment : Fragment() {
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Hide the FAB
        val fab = activity?.findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = View.GONE

        // Set up the toolbar with back button and title
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.title = "User Profile"  // Set the title
        (activity as AppCompatActivity).setSupportActionBar(toolbar)

        //(activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Show back button

        // Override back button press to handle going back to previous fragment
        toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()  // Navigate back to the previous fragment
        }

        // Hide the navigation drawer
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        // Initialize views
        profileImage = view.findViewById(R.id.profileImage)
        profileName = view.findViewById(R.id.profile_name)

        //loadingIndicator = view.findViewById(R.id.loadingIndicator)

        // Update UI with fetched data
        updateUI(profileViewModel.userName, profileViewModel.userProfilePicUrl)
        return view
    }
    private fun updateUI(name: String?, profilePicUrl: String?) {
        // Check if name and profilePicUrl are valid
        if (name.isNullOrEmpty() || profilePicUrl.isNullOrEmpty()) {
            Log.e("HomeFragment", "Name or Profile Picture is missing")
            // Handle the error (e.g., show a default image or prompt the user)
            return
        }

        //showLoadingIndicator()

        profileName.text =  getString(R.string.profileUsername, name)
        profileName.visibility = View.VISIBLE
        loadProfileImage(profilePicUrl)
        profileImage.visibility = View.VISIBLE


        // hideLoadingIndicator()
    }
    private fun loadProfileImage(profilePicUrl: String) {
        val requestOptions = RequestOptions()
            .override(300, 300)
            .fitCenter()
            .diskCacheStrategy(DiskCacheStrategy.ALL)

        Glide.with(this)
            .load(profilePicUrl)
            .apply(requestOptions)
            .transform(CircleCrop())
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.rectangular)
            .into(profileImage)
    }
    override fun onDestroyView() {
        super.onDestroyView()

        // Unlock the navigation drawer when leaving the fragment
        val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

        // Remove the back button and restore toolbar functionality
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }
    override fun onResume() {
        super.onResume()
        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        (activity as AppCompatActivity).supportActionBar?.apply {
           // setDisplayHomeAsUpEnabled(true)
           // setHomeAsUpIndicator(com.google.android.material.R.drawable.ic_arrow_back_black_24) // Optional custom back icon
            title = "User Profile"
        }
        toolbar?.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
    }
}
