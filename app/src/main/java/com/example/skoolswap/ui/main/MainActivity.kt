package com.example.skoolswap.ui.main

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.ImageView
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
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.databinding.ActivityMainBinding
import com.example.skoolswap.ui.navigationheader.NavigationHeaderViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val navHeaderViewModel: NavigationHeaderViewModel by viewModels()

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener() // Combined method
        checkInitialNavigation()
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

        val usernameTextView = headerView.findViewById<TextView>(R.id.usernameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profileImageView)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    navHeaderViewModel.userName,
                    navHeaderViewModel.userEmail,
                    navHeaderViewModel.userProfileImage
                ) { name, email, imageUrl ->
                    Triple(name, email, imageUrl)
                }.collect { (name, email, imageUrl) ->
                    usernameTextView.text = name ?: "Welcome"
                    userEmailTextView.text = email ?: "Sign in to continue"

                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@MainActivity)
                            .load(imageUrl)
                            .circleCrop()
                            .placeholder(R.drawable.ic_user)
                            .error(R.drawable.ic_user)
                            .into(profileImageView)
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_user)
                    }
                }
            }
        }
    }

    private fun setupNavigationListener() {
        val navView: NavigationView = binding.navView

        // Setup navigation with NavController
        navView.setupWithNavController(navController)

        // Add custom navigation item selection handling
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> {
                    try {
                        // Navigate using NavController
                        navController.navigate(menuItem.itemId)
                        // Close drawer
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Navigation error: ${e.message}")
                        false
                    }
                }
            }
        }

        // Add destination changed listener for UI changes
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

    private fun performLogout() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        lifecycleScope.launch {
            // Show loading
            val progressDialog = ProgressDialog(this@MainActivity).apply {
                setMessage("Logging out...")
                setCancelable(false)
                show()
            }

            try {
                // Sign out using viewModel
                viewModel.logout()

                // Navigate to login
                navController.navigate(R.id.loginFragment) {
                    popUpTo(R.id.nav_home) { inclusive = true }
                }

                // Show success message
                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Logout failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navHeaderViewModel.refresh()
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
        viewModel.navigationDestination.observe(this) { destination ->
            navigateToDestination(destination)
        }

        viewModel.forceNavigation.observe(this) { destination ->
            destination?.let {
                navigateToDestination(it)
                viewModel.clearForceNavigation()
            }
        }
    }

    private fun navigateToDestination(destination: NavigationDestination) {
        when (destination) {
            NavigationDestination.ONBOARDING -> {
                if (navController.currentDestination?.id != R.id.viewPagerFragment)
                    navController.navigate(R.id.viewPagerFragment)
            }
            NavigationDestination.HOME -> {
                if (navController.currentDestination?.id != R.id.nav_home)
                    navController.navigate(R.id.nav_home)
            }
            NavigationDestination.LOGIN -> {
                if (navController.currentDestination?.id != R.id.loginFragment)
                    navController.navigate(R.id.loginFragment)
            }
        }
    }

    private fun checkInitialNavigation() {
        viewModel.navigationDestination.observe(this) { destination ->
            navigateToDestination(destination)
        }
    }
}