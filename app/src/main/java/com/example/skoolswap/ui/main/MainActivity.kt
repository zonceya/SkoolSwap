package com.example.skoolswap.ui.main

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
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
import com.bumptech.glide.Glide
import com.example.skoolswap.R
import com.example.skoolswap.data.local.AppPreferences
import com.example.skoolswap.data.repository.AuthRepository
import com.example.skoolswap.databinding.ActivityMainBinding
import com.example.skoolswap.ui.navigationheader.NavigationHeaderViewModel
import com.example.skoolswap.ui.navigationheader.NavigationHeaderViewModelFactory
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var navController: NavController
    private lateinit var navHeaderViewModel: NavigationHeaderViewModel
    private lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        val preferences = AppPreferences(this)

        // ✅ CRITICAL: Initialize authRepository FIRST
        authRepository = AuthRepository(this)
        authRepository.checkCurrentUser()

        // ✅ Now initialize ViewModels
        viewModel = ViewModelProvider(this, MainViewModelFactory(preferences))[MainViewModel::class.java]

        val navHeaderViewModelFactory = NavigationHeaderViewModelFactory(authRepository)
        navHeaderViewModel = ViewModelProvider(this, navHeaderViewModelFactory)[NavigationHeaderViewModel::class.java]

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener()
        checkInitialNavigation()
        observeNavigation()
        observeAuthState()
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

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navHeaderViewModel.refresh()
            }
        }
    }

    // ... rest of your methods remain the same ...
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
                viewModel.navigationDestination.observe(this@MainActivity) { destination ->
                    navigateToDestination(destination)
                }

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
                viewModel.navigationDestination.observe(this@MainActivity) { destination ->
                    navigateToDestination(destination)
                }
            }
        }
    }
}