package com.example.skoolswap.ui.main

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
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
import com.example.skoolswap.common.constants.AppConstants.LogTags
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
import com.example.skoolswap.workers.WorkerManager
import jakarta.inject.Inject
import timber.log.Timber
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.net.Uri

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
    private var lastForegroundRefreshAt = 0L
    private val MIN_REFRESH_INTERVAL_MS = 30_000L
    @Inject
    lateinit var workerManager: WorkerManager

    companion object {
        private const val TAG = "MainActivity"
        private const val STATUS_BAR_LIGHT = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        private const val MIN_SEARCH_QUERY_LENGTH = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        val nightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        updateStatusBar()

        window.statusBarColor = if (isDark) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isDark) {
            0
        } else {
            STATUS_BAR_LIGHT
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = findNavController(R.id.nav_host_fragment_content_main)

        setSupportActionBar(binding.appBarMain.toolbar)

        setupNavigationDrawer()
        setupNavigationHeader()
        setupNavigationListener()
        observeNavigation()
        observeAuthState()
        setupPolicyFooter()
        binding.appBarMain.fab.visibility = View.GONE
        supportActionBar?.hide()
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        if (savedInstanceState == null) {
            when (intent.getStringExtra("destination")) {
                "home" -> {
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
        refreshToolbarVisibility()
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

        Timber.tag(LogTags.UI).d("usernameTextView: ${usernameTextView != null}")
        Timber.tag(LogTags.UI).d("userEmailTextView: ${userEmailTextView != null}")
        Timber.tag(LogTags.UI).d("profileImageView: ${profileImageView != null}")

        loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                navHeaderViewModel.userState.collectLatest { userState ->
                    Timber.tag(LogTags.UI).d("userState: $userState")

                    when (userState) {
                        is UserState.Loading -> {
                            Timber.tag(LogTags.UI).d("State: LOADING")
                            loadingOverlay.visibility = View.VISIBLE
                            usernameTextView.text = "Loading..."
                            userEmailTextView.text = ""
                            profileImageView.setImageResource(R.drawable.ic_user)
                            profileImageView.setColorFilter(null)
                        }
                        is UserState.Success -> {
                            Timber.tag(LogTags.UI).d("State: SUCCESS")
                            Timber.tag(LogTags.UI).d("Name: ${userState.name}")
                            Timber.tag(LogTags.UI).d("Email: ${userState.email}")
                            Timber.tag(LogTags.UI).d("ProfileImageUrl: ${userState.profileImageUrl}")

                            loadingOverlay.visibility = View.GONE
                            usernameTextView.text = userState.name ?: AppConstants.DEFAULT_USER_NAME
                            userEmailTextView.text = userState.email ?: "Sign in to continue"

                            if (!userState.profileImageUrl.isNullOrEmpty()) {
                                Timber.tag(LogTags.UI).d("Loading image from URL: ${userState.profileImageUrl}")

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
                                            Timber.tag(LogTags.UI).e(e, "Glide load failed")
                                            return false
                                        }

                                        override fun onResourceReady(
                                            resource: android.graphics.drawable.Drawable?,
                                            model: Any?,
                                            target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?,
                                            dataSource: com.bumptech.glide.load.DataSource?,
                                            isFirstResource: Boolean
                                        ): Boolean {
                                            Timber.tag(LogTags.UI).d("Glide load success")
                                            return false
                                        }
                                    })
                                    .into(profileImageView)
                                profileImageView.setColorFilter(null)
                            } else {
                                Timber.tag(LogTags.UI).d("No profile image URL, using placeholder")
                                profileImageView.setImageResource(R.drawable.ic_user)
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
                            Timber.tag(LogTags.UI).e("State: ERROR - ${userState.message}")
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

    private fun showMissingContactDialog() {
        DialogHelper.showConfirmationDialog(
            context = this,
            action = DialogAction.MissingContactNumber,
            onConfirm = {
                navController.navigate(R.id.nav_profile)
            },
            onCancel = {
                Toast.makeText(this, "Please add a contact number to list items", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun setupNavigationListener() {
        val navView: NavigationView = binding.navView
        binding.appBarMain.fab.setOnClickListener {
            lifecycleScope.launch {
                authRepository.refreshUserProfile()
                val hasContactNumber = viewModel.hasContactNumber()
                if (hasContactNumber) {
                    navController.navigate(R.id.createItemFragment)
                } else {
                    showMissingContactDialog()
                }
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    showLogoutConfirmationDialog()
                    true
                }
                R.id.nav_help -> {
                    // Navigate to help fragment
                    navController.navigate(R.id.nav_help)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
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
                        Timber.tag(LogTags.UI).e(e, "Navigation error")
                        false
                    }
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.schoolOnboardingFragment,
                R.id.viewPagerFragment,
                R.id.introFragment,
                R.id.loginFragment -> {
                    supportActionBar?.hide()
                    binding.appBarMain.fab.visibility = View.GONE
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    window.statusBarColor = android.graphics.Color.BLACK
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = 0
                }
                R.id.signUpFragment -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    binding.appBarMain.fab.visibility = View.GONE
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = false
                }
                R.id.nav_profile,
                R.id.nav_favorites,
                R.id.createItemFragment,
                R.id.editItemFragment,
                R.id.nav_shop,
                R.id.nav_help -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.GONE  // ← HIDE FAB
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = false
                }
                else -> {
                    supportActionBar?.show()
                    binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                    binding.appBarMain.fab.visibility = View.VISIBLE
                    binding.appBarMain.toolbar.menu.findItem(R.id.action_search)?.isVisible = true

                    val nightMode = resources.configuration.uiMode and
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK
                    val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
                    updateStatusBar()
                    window.statusBarColor = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                    @Suppress("DEPRECATION")
                    window.decorView.systemUiVisibility = if (isDark) 0 else STATUS_BAR_LIGHT
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
    private fun setupPolicyFooter() {
        val navView = binding.navView

        // Inflate the footer layout
        val footerLayout = layoutInflater.inflate(R.layout.nav_footer_policies, navView, false)

        // Add at the END of the NavigationView (after header and menu)
        // Get the current child count and add at the end
        navView.addView(footerLayout, navView.childCount)

        // Set click listeners
        footerLayout.findViewById<TextView>(R.id.tv_privacy)?.setOnClickListener {
            openPolicyUrl("https://skoolswap.co.za/privacy")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        footerLayout.findViewById<TextView>(R.id.tv_terms)?.setOnClickListener {
            openPolicyUrl("https://skoolswap.co.za/terms")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        footerLayout.findViewById<TextView>(R.id.tv_guidelines)?.setOnClickListener {
            openPolicyUrl("https://skoolswap.co.za/guidelines")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
        footerLayout.findViewById<TextView>(R.id.tv_prohibited)?.setOnClickListener {
            openPolicyUrl("https://skoolswap.co.za/Prohibited")
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val destinationId = savedInstanceState.getInt("currentDestinationId", R.id.nav_home)
        if (navController.currentDestination?.id != destinationId) {
            navController.navigate(destinationId)
        }
    }
    private fun openPolicyUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Timber.e(e, "Failed to open URL: $url")
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }
    private fun performLogout() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)

        lifecycleScope.launch {
            try {
                viewModel.logout()
                viewHomeModel.clearHomeData()
                appPreferences.clearUserData()
                workerManager.cancelTokenRefresh()

                val navController = findNavController(R.id.nav_host_fragment_content_main)

                try {
                    navController.navigate(R.id.action_global_logout)
                } catch (e: Exception) {
                    Timber.tag(LogTags.UI).w(e, "Global logout action failed, using fallback")
                    navController.popBackStack(R.id.mobile_navigation, true)
                    navController.navigate(R.id.loginFragment)
                }

                Snackbar.make(binding.root, "Logged out successfully", Snackbar.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Timber.tag(LogTags.UI).e(e, "Logout failed")
                Snackbar.make(binding.root, "Logout failed. Please try again.", Snackbar.LENGTH_LONG).show()
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
                    } else if (newText.length >= MIN_SEARCH_QUERY_LENGTH) {
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
        if (query.length < MIN_SEARCH_QUERY_LENGTH) return

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
            R.id.signUpFragment -> {
                navController.navigate(R.id.action_signUpFragment_to_loginFragment)
                return true
            }
            else -> {
                return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
            }
        }
    }

    private fun updateStatusBar() {
        val nightMode = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES

        window.statusBarColor = if (isDark) {
            android.graphics.Color.BLACK
        } else {
            android.graphics.Color.WHITE
        }

        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = if (isDark) {
            0
        } else {
            STATUS_BAR_LIGHT
        }
    }

    private fun observeNavigation() {
        Timber.tag(LogTags.UI).d("👀 observeNavigation called at ${System.currentTimeMillis()}")

        viewModel.navigationDestination.observe(this) { destination ->
            Timber.tag(LogTags.UI).d("📡 navigationDestination observed: $destination at ${System.currentTimeMillis()}")

            val currentDest = navController.currentDestination?.id
            if (currentDest == R.id.introFragment) {
                Timber.tag(LogTags.UI).d("⏸️ SKIPPING navigation - IntroFragment is handling routing")
                return@observe
            }

            if (destination != null) {
                navigateToDestination(destination)
                viewModel.clearNavigationDestination()
            } else {
                Timber.tag(LogTags.UI).d("⏭️ Null destination - no navigation")
            }
        }

        viewModel.forceNavigation.observe(this) { destination ->
            Timber.tag(LogTags.UI).d("📡 forceNavigation observed: $destination at ${System.currentTimeMillis()}")

            val currentDest = navController.currentDestination?.id
            if (currentDest == R.id.introFragment) {
                Timber.tag(LogTags.UI).d("⏸️ SKIPPING force navigation - IntroFragment is handling routing")
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

        if (currentDestId == R.id.createItemFragment) {
            Timber.tag(LogTags.UI).d("🛑 BLOCKING navigation - createItemFragment is active (camera return)")
            return
        }

        if (currentDestId == R.id.nav_profile) {
            Timber.tag(LogTags.UI).d("🛑 BLOCKING navigation - ProfileFragment is active")
            return
        }

        when (destination) {
            NavigationDestination.HOME -> {
                Timber.tag(LogTags.UI).d("🏠 Navigating to HOME")

                if (currentDestId == R.id.nav_home) {
                    Timber.tag(LogTags.UI).d("✅ Already on home, skipping")
                    return
                }

                val popped = navController.popBackStack(R.id.nav_home, false)
                if (!popped) {
                    navController.navigate(R.id.action_introFragment_to_nav_home)
                }

                Timber.tag(LogTags.UI).d("✅ Navigated to HOME")
            }

            NavigationDestination.LOGIN -> {
                Timber.tag(LogTags.UI).d("🔐 Navigating to LOGIN")

                if (currentDestId == R.id.loginFragment) {
                    Timber.tag(LogTags.UI).d("✅ Already on login, skipping")
                    return
                }

                navController.navigate(R.id.action_global_logout)
                Timber.tag(LogTags.UI).d("✅ Navigated to LOGIN")
            }

            NavigationDestination.ONBOARDING -> {
                Timber.tag(LogTags.UI).d("📋 Navigating to ONBOARDING")

                if (currentDestId == R.id.viewPagerFragment) {
                    Timber.tag(LogTags.UI).d("✅ Already on onboarding, skipping")
                    return
                }

                val popped = navController.popBackStack(R.id.viewPagerFragment, false)
                if (!popped) {
                    navController.navigate(R.id.action_introFragment_to_onboarding)
                }

                Timber.tag(LogTags.UI).d("✅ Navigated to ONBOARDING")
            }

            else -> {
                Timber.tag(LogTags.UI).d("⚠️ Unknown destination: $destination - falling back to LOGIN")
                navController.navigate(R.id.action_global_logout)
            }
        }
    }

    private fun refreshToolbarVisibility() {
        val currentDestination = navController.currentDestination?.id
        Timber.tag(LogTags.UI).d("🔄 Refreshing toolbar for destination: $currentDestination")

        when (currentDestination) {
            R.id.schoolOnboardingFragment,
            R.id.viewPagerFragment,
            R.id.introFragment,
            R.id.loginFragment -> {
                supportActionBar?.hide()
                binding.appBarMain.fab.visibility = View.GONE
            }
            else -> {
                supportActionBar?.show()
                binding.appBarMain.fab.visibility = View.VISIBLE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Timber.tag(LogTags.UI).d("🔄 onResume called")

        // ✅ Only handle the camera/gallery return edge case
        if (viewModel.suppressNextResumeRefresh) {
            viewModel.suppressNextResumeRefresh = false
            Timber.tag(LogTags.UI).d("⏭️ Skipping toolbar refresh - returning from camera/gallery")
            refreshToolbarVisibility()
            updateStatusBar()
            return
        }

        // ✅ Minimal: just ensure UI state is correct
        refreshToolbarVisibility()
        updateStatusBar()

        // ❌ REMOVED: navHeaderViewModel.refresh() - This should be event-driven
        // ❌ REMOVED: All workerManager sync calls - Moved to ProcessLifecycleOwner

        // ✅ Note: Nav header will refresh when user state changes
        // via the collector in setupNavigationHeader()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateStatusBar()
    }

    override fun onPause() {
        super.onPause()
        Timber.tag(LogTags.UI).d("🔥 onPause at ${System.currentTimeMillis()}")
    }
}