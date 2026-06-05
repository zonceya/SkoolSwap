package com.example.skoolswap.ui.main

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
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
import com.bumptech.glide.load.engine.GlideException
import com.example.skoolswap.data.local.datastore.AppPreferences
import com.example.skoolswap.domain.repository.AuthRepositoryInterface
import com.example.skoolswap.ui.home.HomeViewModel
import com.example.skoolswap.ui.products.ProductsFragment
import com.example.skoolswap.utils.DialogAction
import com.example.skoolswap.utils.DialogHelper
import jakarta.inject.Inject
import timber.log.Timber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private val viewHomeModel: HomeViewModel by viewModels()
    private val navHeaderViewModel: NavigationHeaderViewModel by viewModels()
    @Inject
    lateinit var appPreferences: AppPreferences
    private lateinit var navController: NavController
    @Inject
    lateinit var authRepository: AuthRepositoryInterface

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)

        val nightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        // Set status bar color explicitly based on mode
        window.statusBarColor = if (isDark) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }

        // Set icon tint — light icons for dark, dark icons for light
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isDark) {
            0  // dark background = light icons, clear the flag
        } else {
            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR  // light background = dark icons
        }

        // Only inflate once!
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Debug theme colors
        val typedValue = android.util.TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        Timber.tag("THEME").e("colorSurface = #${Integer.toHexString(typedValue.data)}")

        theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
        Timber.tag("THEME").e("colorBackground = #${Integer.toHexString(typedValue.data)}")

        Timber.tag("THEME").e("Night mode = ${if (isDark) "DARK" else "LIGHT"}")

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener()
        observeNavigation()
        observeAuthState()

        // Initial FAB/toolbar state
        binding.appBarMain.fab.visibility = View.GONE
        supportActionBar?.hide()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        // ✅ FIXED: Route based on SplashActivity decision - NO runBlocking!
        if (savedInstanceState == null) {
            when (intent.getStringExtra("destination")) {
                "home" -> {
                    // Move blocking call off main thread
                    lifecycleScope.launch {
                        val schoolMapped = appPreferences.hasSchoolMapped()
                        if (schoolMapped) {
                            navController.navigate(R.id.action_loginFragment_to_nav_home)
                        } else {
                            navController.navigate(R.id.action_loginFragment_to_profileFragment)
                        }
                        authRepository.refreshUserProfile()
                    }
                }
                "onboarding" -> navController.navigate(R.id.viewPagerFragment)
                else -> { /* stay on loginFragment */ }
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
    // Add this function to debug the divider color
    private fun debugDividerColor() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)

        // Find the divider view
        val divider = headerView.findViewById<View>(R.id.divider)

        if (divider != null) {
            // Get the divider background color
            val background = divider.background
            if (background != null) {
                // For color drawable
                if (background is android.graphics.drawable.ColorDrawable) {
                    val colorInt = background.color
                    val hexColor = String.format("#%08X", colorInt)
                    val alpha = (colorInt shr 24) and 0xFF
                    val alphaPercent = (alpha / 255.0) * 100

                    Timber.tag("DIVIDER_DEBUG").e("=== DIVIDER COLOR DEBUG ===")
                    Timber.tag("DIVIDER_DEBUG").e("Divider exists: YES")
                    Timber.tag("DIVIDER_DEBUG").e("Color: $hexColor")
                    Timber.tag("DIVIDER_DEBUG").e("Alpha: $alpha (${String.format("%.1f", alphaPercent)}%)")
                    Timber.tag("DIVIDER_DEBUG").e("Visible: ${divider.visibility == View.VISIBLE}")
                    Timber.tag("DIVIDER_DEBUG").e("Height: ${divider.layoutParams?.height}px")
                    Timber.tag("DIVIDER_DEBUG").e("Alpha property: ${divider.alpha}")
                }
            } else {
                Timber.tag("DIVIDER_DEBUG").e("Divider has no background drawable")
            }

            // Temporarily force the divider to be very visible for testing
            // Uncomment to force visibility:
            // forceDividerVisible(divider)
        } else {
            Timber.tag("DIVIDER_DEBUG").e("Divider not found in header layout!")

            // List all views in header for debugging
            listAllViews(headerView)
        }
    }

    // Force divider to be very visible for testing
    private fun forceDividerVisible(divider: View) {
        Timber.tag("DIVIDER_DEBUG").e("=== FORCING DIVIDER VISIBLE FOR TEST ===")

        // Make divider bright red for testing
        divider.setBackgroundColor(android.graphics.Color.RED)
        divider.alpha = 1.0f  // Fully opaque

        // Increase height
        val params = divider.layoutParams
        params.height = 4  // 4dp for testing
        divider.layoutParams = params
    }

    // Helper to list all views in header (for debugging)
    private fun listAllViews(view: View, level: Int = 0) {
        val indent = "  ".repeat(level)
        Timber.tag("DIVIDER_DEBUG").e("$indent- ${view::class.java.simpleName} (id: ${view.id})")

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                listAllViews(view.getChildAt(i), level + 1)
            }
        }
    }

    // Call this function after setupNavigationHeader()
    private fun setupNavigationHeader() {
        val navView: NavigationView = binding.navView
        val headerView = navView.getHeaderView(0)
        val loadingOverlay = binding.loadingOverlay

        val usernameTextView = headerView.findViewById<TextView>(R.id.usernameTextView)
        val userEmailTextView = headerView.findViewById<TextView>(R.id.userEmailTextView)
        val profileImageView = headerView.findViewById<ImageView>(R.id.profileImageView)

        // Log to check if views are found
        Timber.tag("NavHeader").d("usernameTextView: ${usernameTextView != null}")
        Timber.tag("NavHeader").d("userEmailTextView: ${userEmailTextView != null}")
        Timber.tag("NavHeader").d("profileImageView: ${profileImageView != null}")

        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navHeaderViewModel.userState.collectLatest { userState ->
                    Timber.tag("NavHeader").d("userState: $userState")

                    when (userState) {
                        is UserState.Loading -> {
                            Timber.tag("NavHeader").d("State: LOADING")
                            loadingOverlay.visibility = View.VISIBLE
                            usernameTextView.text = "Loading..."
                            userEmailTextView.text = ""
                            profileImageView.setImageResource(R.drawable.ic_user)
                            profileImageView.setColorFilter(null)
                        }
                        is UserState.Success -> {
                            Timber.tag("NavHeader").d("State: SUCCESS")
                            Timber.tag("NavHeader").d("Name: ${userState.name}")
                            Timber.tag("NavHeader").d("Email: ${userState.email}")
                            Timber.tag("NavHeader")
                                .d("ProfileImageUrl: ${userState.profileImageUrl}")

                            loadingOverlay.visibility = View.GONE
                            usernameTextView.text = userState.name ?: "Welcome"
                            userEmailTextView.text = userState.email ?: "Sign in to continue"

                            if (!userState.profileImageUrl.isNullOrEmpty()) {
                                Timber.tag("NavHeader")
                                    .d("Loading image from URL: ${userState.profileImageUrl}")

                                Glide.with(this@MainActivity)
                                    .load(userState.profileImageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .override(72, 72)
                                    .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                                        override fun onLoadFailed(
                                            e: GlideException?,
                                            model: Any?,
                                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            Timber.tag("NavHeader").e(e, "Glide load failed")
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: android.graphics.drawable.Drawable?,
                                            model: Any?,
                                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                            dataSource: com.bumptech.glide.load.DataSource?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            Timber.tag("NavHeader").d("Glide load success")
                                            return false
                                        }
                                    })
                                    .into(profileImageView)
                                profileImageView.setColorFilter(null)
                            } else {
                                Timber.tag("NavHeader").d("No profile image URL, using placeholder")
                                profileImageView.setImageResource(R.drawable.ic_user)
                                // Set tint based on theme
                                val isDarkMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                                val tintColor = if (isDarkMode) {
                                    ContextCompat.getColor(this@MainActivity, R.color.white)
                                } else {
                                    ContextCompat.getColor(this@MainActivity, R.color.black)
                                }
                                profileImageView.setColorFilter(tintColor)
                            }
                        }
                        is UserState.Error -> {
                            Timber.tag("NavHeader").e("State: ERROR - ${userState.message}")
                            loadingOverlay.visibility = View.GONE
                            usernameTextView.text = "Error"
                            userEmailTextView.text = userState.message
                            profileImageView.setImageResource(R.drawable.ic_user)
                            profileImageView.setColorFilter(null)
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
                R.id.viewPagerFragment,
                R.id.loginFragment -> {
                    supportActionBar?.hide()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.fab.visibility = View.GONE
                    window.statusBarColor = android.graphics.Color.BLACK
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0 // light icons
                }

                R.id.signUpFragment,
                R.id.otpFragment -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.fab.visibility = View.GONE
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = false
                }

                R.id.nav_profile,
                R.id.nav_favorites,
                R.id.createItemFragment,
                R.id.editItemFragment,
                R.id.nav_shop -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.VISIBLE
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = false
                }

                else -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.VISIBLE
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = true

                    // Restore theme-based status bar for all other screens
                    val nightMode = resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    window.statusBarColor = if (isDark) android.graphics.Color.BLACK
                    else android.graphics.Color.WHITE
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = if (isDark) 0
                    else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }

    }

    private fun showLogoutConfirmationDialog() {
        DialogHelper.showConfirmationDialog(
            context = this,
            action = DialogAction.Logout,
            onConfirm = {
                performLogout()
            }
        )
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
        lifecycleScope.launch {
            try {
                viewModel.logout()
                viewHomeModel.clearHomeData()
                appPreferences.clearUserData()

                // Use the action from loginFragment
                navController.navigate(R.id.action_global_logout)

                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Logout failed: ${e.message}", Snackbar.LENGTH_LONG).show()
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

        val currentFragment = getCurrentFragment()

        when (currentFragment) {
            is HomeFragment -> currentFragment.performLiveSearch(query)
            is ProductsFragment -> {
                currentFragment.performLiveSearch(query)
            }
            else -> Toast.makeText(this, "Search not available here", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentFragment(): Fragment? {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        return navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
    }

    private fun clearHomeSearch() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        if (currentFragment is HomeFragment) {
            currentFragment.exitSearchMode()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val currentDestId = navController.currentDestination?.id

        when (currentDestId) {
            R.id.otpFragment -> {
                val purpose = navController.currentBackStackEntry
                    ?.arguments?.getString("purpose") ?: "LOGIN"

                if (purpose == "SIGNUP") {
                    navController.navigate(R.id.action_otpFragment_to_signUpFragment)
                } else {
                    navController.navigate(R.id.action_otpFragment_to_loginFragment)
                }
                return true
            }
            R.id.signUpFragment -> {
                navController.navigate(R.id.action_signUpFragment_to_loginFragment)
                return true
            }
            else -> {
                return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
            }
        }
    }

    private fun observeNavigation() {
        Timber.tag("MainActivity").e("👀 observeNavigation called at ${System.currentTimeMillis()}")

        viewModel.navigationDestination.observe(this) { destination ->
            Timber.tag("MainActivity")
                .e("📡 navigationDestination observed: $destination at ${System.currentTimeMillis()}")

            // CRITICAL FIX: Don't navigate while IntroFragment is active
            val currentDest = navController.currentDestination?.id
            if (currentDest == R.id.introFragment) {
                Timber.tag("MainActivity").e("⏸️ SKIPPING navigation - IntroFragment is handling routing")
                return@observe
            }

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

            // CRITICAL FIX: Don't navigate while IntroFragment is active
            val currentDest = navController.currentDestination?.id
            if (currentDest == R.id.introFragment) {
                Timber.tag("MainActivity").e("⏸️ SKIPPING force navigation - IntroFragment is handling routing")
                return@observe
            }

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
        navHeaderViewModel.refresh()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag("MainActivity").e("🔥 onPause at ${System.currentTimeMillis()}")
    }
}