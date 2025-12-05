package com.example.skoolswap.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.skoolswap.R
import com.example.skoolswap.data.local.AppPreferences
import com.example.skoolswap.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        val preferences = AppPreferences(this)
        viewModel = ViewModelProvider(this, MainViewModelFactory(preferences))[MainViewModel::class.java]

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationListener()
        checkInitialNavigation()
        observeNavigation()
    }

    private fun setupNavigationDrawer() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun observeNavigation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe regular navigation
                viewModel.navigationDestination.observe(this@MainActivity) { destination ->
                    navigateToDestination(destination)
                }

                // Observe force navigation (for immediate actions)
                viewModel.forceNavigation.observe(this@MainActivity) { destination ->
                    destination?.let {
                        navigateToDestination(it)
                        viewModel.clearForceNavigation()
                    }
                }
            }
        }
    }
    private fun setupNavigationListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.viewPagerFragment, R.id.loginFragment -> {
                    // Hide UI for onboarding and login
                    supportActionBar?.hide()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.fab.visibility = View.GONE
                }
                else -> {
                    // Show UI for main app screens
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.VISIBLE
                }
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
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Get the initial destination from ViewModel
                viewModel.navigationDestination.observe(this@MainActivity) { destination ->
                    navigateToDestination(destination)
                }
            }
        }
    }


}
