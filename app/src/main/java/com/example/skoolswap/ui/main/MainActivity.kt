package com.example.skoolswap.ui.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import com.example.skoolswap.ui.home.HomeFragment
import com.example.skoolswap.ui.navigationheader.NavigationHeaderViewModel
import com.example.skoolswap.ui.navigationheader.UserState
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import androidx.navigation.fragment.NavHostFragment
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import jakarta.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val navHeaderViewModel: NavigationHeaderViewModel by viewModels()

    private lateinit var navController: NavController
    @Inject
    lateinit var authRepository: AuthRepositoryInterface
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.tag("MainActivity").e("🔥 onCreate at ${System.currentTimeMillis()}")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener()
        observeNavigation()
        observeAuthState()
        lifecycleScope.launch {
            delay(100) // Small delay to ensure UI is ready
            val restored = authRepository.restoreSession()
            Timber.tag("MainActivity").e("🔐 Session restored: $restored")

            if (!restored) {
                Timber.tag("MainActivity")
                    .e("⚠️ No session, but we're on a content screen - this shouldn't happen")
                // Only navigate to login if we're not already there
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    navController.navigate(R.id.loginFragment)
                }
            } else {
                Timber.tag("MainActivity").e("✅ Session restored successfully!")
                // Optionally refresh user data
                authRepository.refreshUserProfile()
            }
        }
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

        loadingOverlay.visibility = View.VISIBLE

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
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentDestinationId", navController.currentDestination?.id ?: R.id.nav_home)
    }

    private fun setupNavigationListener() {
        val navView: NavigationView = binding.navView
        binding.appBarMain.fab.setOnClickListener {
            navController.navigate(R.id.createItemFragment)
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                else -> {
                    try {
                        if (navController.currentDestination?.id != menuItem.itemId) {
                            navController.navigate(menuItem.itemId)
                        }
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                        true
                    } catch (e: Exception) {
                        Timber.tag("MainActivity").e("Navigation error: ${e.message}")
                        false
                    }
                }
            }
        }

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
        val destinationId = savedInstanceState.getInt("currentDestinationId", R.id.nav_home)
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }

    private fun performLogout() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                viewModel.logout()
                navController.navigate(R.id.loginFragment) {
                    popUpTo(R.id.nav_home) { inclusive = true }
                }
                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Logout failed: ${e.message}", Snackbar.LENGTH_LONG).show()
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                delay(AppConstants.TIMEOUT)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.checkAuthState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? androidx.appcompat.widget.SearchView

        searchView?.apply {
            queryHint = "Search..."
            setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { performSearch(it) }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (newText.isNullOrEmpty()) {
                        clearHomeSearch()
                    } else if (newText.length >= 2) {
                        performSearch(newText)
                    }
                    return true
                }
            })

            setOnCloseListener {
                clearHomeSearch()
                true
            }
        }
        return true
    }

    private fun performSearch(query: String) {
        if (query.length < 2) return

        // Get the current visible fragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        if (currentFragment is HomeFragment) {
            // Call HomeFragment's search
            currentFragment.performLiveSearch(query)
        } else {
            Toast.makeText(this, "Search is only available on the Home screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearHomeSearch() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is HomeFragment) {
            currentFragment.exitSearchMode()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun observeNavigation() {
        Timber.tag("MainActivity").e("👀 observeNavigation called at ${System.currentTimeMillis()}")
        viewModel.navigationDestination.observe(this) { destination ->
            Timber.tag("MainActivity")
                .e("📡 navigationDestination observed: $destination at ${System.currentTimeMillis()}")

            if (destination != null) {
                navigateToDestination(destination)
                viewModel.clearNavigationDestination()
            } else {
                Timber.tag("MainActivity").e("⏭️ Null destination - no navigation")
            }
        }

        viewModel.forceNavigation.observe(this) { destination ->
            Timber.tag("MainActivity")
                .e("📡 forceNavigation observed: $destination at ${System.currentTimeMillis()}")
            destination?.let {
                navigateToDestination(it)
                viewModel.clearForceNavigation()
            }
        }
    }

    private fun navigateToDestination(destination: NavigationDestination) {
        val currentDestId = navController.currentDestination?.id
        val currentDestName = navController.currentDestination?.displayName

        Timber.tag("MainActivity")
            .e("🎯 navigateToDestination: $destination, current: $currentDestName")

        if (currentDestId == R.id.nav_profile) {
            Timber.tag("MainActivity")
                .e("🛑 BLOCKING navigation to $destination - ProfileFragment is active")
            return
        }

        if (currentDestId == R.id.productsFragment ||
            currentDestId == R.id.uniformFragment ||
            currentDestId == R.id.sportFragment) {
            Timber.tag("MainActivity").e("🛑 BLOCKED - Already on a content screen")
            return
        }

        when (destination) {
            NavigationDestination.ONBOARDING -> {
                if (currentDestId != R.id.viewPagerFragment) {
                    navController.navigate(R.id.viewPagerFragment)
                }
            }
            NavigationDestination.HOME -> {
                if (currentDestId != R.id.nav_home &&
                    currentDestId != R.id.productsFragment &&
                    currentDestId != R.id.uniformFragment &&
                    currentDestId != R.id.sportFragment &&
                    currentDestId != R.id.recentFragment) {
                    navController.navigate(R.id.nav_home)
                } else {
                    Timber.tag("MainActivity")
                        .e("🛑 Already on a valid screen, skipping HOME navigation")
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
        Timber.tag("MainActivity").e("🔥 onResume at ${System.currentTimeMillis()}")
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").e("🔥 onPause at ${System.currentTimeMillis()}")
    }
}