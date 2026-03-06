package com.example.skoolswap.ui.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.skoolswap.R
import com.example.skoolswap.common.constants.AppConstants
import com.example.skoolswap.databinding.ActivityMainBinding
import com.example.skoolswap.ui.navigationheader.NavigationHeaderViewModel
import com.example.skoolswap.ui.navigationheader.UserState
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val navHeaderViewModel: NavigationHeaderViewModel by viewModels()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("MainActivity", "🔥 onCreate at ${System.currentTimeMillis()}")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener()
        observeNavigation()
        observeAuthState()
    }

    private fun setupNavigationDrawer() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun setupNavigationHeader() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)
        val loadingOverlay = binding.loadingOverlay

        val usernameTextView = headerView.findViewById<TextView>(R.id.usernameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profileImageView)

        // Show loading overlay initially
        loadingOverlay.visibility = View.VISIBLE

        // Observe navigation header view model
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navHeaderViewModel.userState.collectLatest { userState ->
                    when (userState) {
                        is UserState.Loading -> {
                            loadingOverlay.visibility = View.VISIBLE
                            usernameTextView.text = "Loading..."
                            userEmailTextView.text = ""
                            profileImageView.setImageResource(R.drawable.ic_user)
                        }
                        is UserState.Success -> {
                            loadingOverlay.visibility = View.GONE

                            usernameTextView.text = userState.name ?: "Welcome"
                            userEmailTextView.text = userState.email ?: "Sign in to continue"

                            if (!userState.profileImageUrl.isNullOrEmpty()) {
                                Glide.with(this@MainActivity)
                                    .load(userState.profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Force fresh load
                                    .skipMemoryCache(true)
                                    .into(profileImageView)
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_user)
                            }
                        }
                        is UserState.Error -> {
                            loadingOverlay.visibility = View.GONE

                            usernameTextView.text = "Error"
                            userEmailTextView.text = userState.message
                            profileImageView.setImageResource(R.drawable.ic_user)

                            headerView.setOnClickListener {
                                loadingOverlay.visibility = View.VISIBLE
                                navHeaderViewModel.refresh()
                            }
                        }
                    }
                }
            }
        }

        // NO NEED to call loadProfile() - ViewModel observes automatically
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current destination
        outState.putInt("currentDestinationId", navController.currentDestination?.id ?: R.id.nav_home)
    }
    private fun setupNavigationListener() {
        val navView: NavigationView = binding.navView
        binding.appBarMain.fab.setOnClickListener {
            navController.navigate(R.id.createItemFragment)
        }

        // Setup navigation item selection
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> {
                    try {
                        // Check if we're already on this destination
                        if (navController.currentDestination?.id != menuItem.itemId) {
                            navController.navigate(menuItem.itemId)
                        }
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Navigation error: ${e.message}")
                        false
                    }
                }
            }
        }

        // Setup destination changed listener
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.viewPagerFragment, R.id.loginFragment -> {
                    supportActionBar?.hide()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.fab.visibility = View.GONE
                }
                else -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore destination if needed
        val destinationId = savedInstanceState.getInt("currentDestinationId", R.id.nav_home)
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }
    private fun performLogout() {
        // Close the drawer (optional if you're using a navigation drawer)
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        // Get the ProgressBar and show it
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE  // Show ProgressBar

        lifecycleScope.launch {
            try {
                // Sign out using ViewModel
                viewModel.logout()

                // Navigate to login screen
                navController.navigate(R.id.loginFragment) {
                    popUpTo(R.id.nav_home) { inclusive = true }
                }

                // Show success message
                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Handle logout failure
                Snackbar.make(binding.root, "Logout failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                // Hide ProgressBar after operation is complete
                progressBar.visibility = View.GONE  // Hide ProgressBar
            }
        }
    }


    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                // This triggers profile refresh when activity resumes
                delay(AppConstants.TIMEOUT)
               // navHeaderViewModel.refresh() // ← This calls the profile API
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Check if user is already logged in
        viewModel.checkAuthState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun observeNavigation() {
        Log.e("MainActivity", "👀 observeNavigation called at ${System.currentTimeMillis()}")
        viewModel.navigationDestination.observe(this) { destination ->
            Log.e("MainActivity", "📡 navigationDestination observed: $destination at ${System.currentTimeMillis()}")

            // 🔥 Only navigate if destination is not null
            if (destination != null) {
                navigateToDestination(destination)
            } else {
                Log.e("MainActivity", "⏭️ Null destination - no navigation (user needs to complete profile)")
            }
        }

        viewModel.forceNavigation.observe(this) { destination ->
            Log.e("MainActivity", "📡 forceNavigation observed: $destination at ${System.currentTimeMillis()}")
            destination?.let {
                navigateToDestination(it)
                viewModel.clearForceNavigation()
            }
        }
    }
    private fun navigateToDestination(destination: NavigationDestination) {
        val currentDestId = navController.currentDestination?.id
        val currentDestName = navController.currentDestination?.displayName

        Log.e("MainActivity", "🎯 navigateToDestination: $destination, current: $currentDestName")

        // BLOCK ALL navigation when ProfileFragment is visible
        if (currentDestId == R.id.nav_profile) {
            Log.e("MainActivity", "🛑 BLOCKING navigation to $destination - ProfileFragment is active")
            return
        }

        // Also block if we're already at the destination
        when (destination) {
            NavigationDestination.ONBOARDING -> {
                if (currentDestId != R.id.viewPagerFragment) {
                    navController.navigate(R.id.viewPagerFragment)
                }
            }
            NavigationDestination.HOME -> {
                if (currentDestId != R.id.nav_home) {
                    navController.navigate(R.id.nav_home)
                }
            }
            NavigationDestination.LOGIN -> {
                if (currentDestId != R.id.loginFragment) {
                    navController.navigate(R.id.loginFragment)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.e("MainActivity", "🔥 onResume at ${System.currentTimeMillis()}")
    }

    override fun onPause() {
        super.onPause()
        Log.e("MainActivity", "🔥 onPause at ${System.currentTimeMillis()}")
    }
}