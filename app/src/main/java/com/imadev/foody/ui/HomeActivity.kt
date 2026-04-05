package com.imadev.foody.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.imadev.foody.R
import com.imadev.foody.databinding.ActivityMainBinding
import com.imadev.foody.model.Client
import com.imadev.foody.ui.auth.LoginActivity
import com.imadev.foody.utils.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), View.OnClickListener {

    private var mMotionProgress: Float = 0f
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var isDrawerActive = false
    private var userRole: String? = null

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.applyFullscreen()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController

        // AJOUT DES FRAGMENTS ADMIN ET LIVREUR COMME TOP-LEVEL POUR ENLEVER LA FLECHE RETOUR
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.historyFragment,
                R.id.favoritesFragment,
                R.id.userFragment,
                R.id.adminDashboardFragment,
                R.id.adminOrdersFragment,
                R.id.adminDriversFragment,
                R.id.adminUsersFragment,
                R.id.deliveryDashboardFragment,
                R.id.driverOrdersFragment,
                R.id.driverHistoryFragment
            )
        )

        setSupportActionBar(binding.toolbar)
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentDestinationId = destination.id
            configToolbar(currentDestinationId)
            updateBottomNavigationMenu(currentDestinationId)

            // CACHE L'ICONE MENU POUR ADMIN ET LIVREUR
            val isStaff = userRole == Client.ROLE_ADMIN || userRole == Client.ROLE_DELIVERY
            
            val showMenuIcon = (currentDestinationId in setOf(
                R.id.homeFragment, R.id.favoritesFragment, R.id.historyFragment,
                R.id.userFragment
            )) && !isStaff
            
            if (showMenuIcon) binding.menuIc.show() else binding.menuIc.hide()
        }

        binding.motionLayout.addTransitionListener(object : MotionLayout.TransitionListener {
            override fun onTransitionStarted(motionLayout: MotionLayout?, startId: Int, endId: Int) {}
            override fun onTransitionChange(motionLayout: MotionLayout?, startId: Int, endId: Int, progress: Float) {
                mMotionProgress = progress
                isDrawerActive = progress > 0
            }
            override fun onTransitionCompleted(motionLayout: MotionLayout?, currentId: Int) {
                isDrawerActive = currentId == R.id.end
            }
            override fun onTransitionTrigger(motionLayout: MotionLayout?, triggerId: Int, positive: Boolean, progress: Float) {}
        })

        subscribeClickListeners()

        binding.bottomNav.setOnItemSelectedListener { item ->
            if (isDrawerActive) binding.motionLayout.transitionToStart()
            NavigationUI.onNavDestinationSelected(item, navController)
            return@setOnItemSelectedListener true
        }

        handleRoleRedirection()
    }

    private fun updateBottomNavigationMenu(destinationId: Int) {
        when {
            userRole == Client.ROLE_ADMIN -> {
                if (binding.bottomNav.menu.findItem(R.id.adminDashboardFragment) == null) {
                    binding.bottomNav.menu.clear()
                    binding.bottomNav.inflateMenu(R.menu.menu_admin_navigation)
                }
                binding.bottomNav.visibility = View.VISIBLE
            }
            userRole == Client.ROLE_DELIVERY -> {
                if (binding.bottomNav.menu.findItem(R.id.deliveryDashboardFragment) == null) {
                    binding.bottomNav.menu.clear()
                    binding.bottomNav.inflateMenu(R.menu.menu_delivery_navigation)
                }
                binding.bottomNav.visibility = View.VISIBLE
            }
            else -> {
                if (binding.bottomNav.menu.findItem(R.id.homeFragment) == null) {
                    binding.bottomNav.menu.clear()
                    binding.bottomNav.inflateMenu(R.menu.menu_bottom_navigation)
                }
                binding.bottomNav.visibility = View.VISIBLE
            }
        }
    }

    private fun handleRoleRedirection() {
        val uid = Firebase.auth.uid ?: return
        val databaseUrl = "https://foody-app-a1b12-default-rtdb.firebaseio.com/"
        FirebaseDatabase.getInstance(databaseUrl).reference
            .child("clients").child(uid).child("role").get().addOnSuccessListener {
                val dbRole = it.value as? String
                this.userRole = dbRole
                dbRole?.let { r -> applyRoleRedirection(r) }
            }
    }

    private fun applyRoleRedirection(role: String) {
        when (role) {
            Client.ROLE_ADMIN -> {
                navController.navigate(R.id.adminDashboardFragment)
            }
            Client.ROLE_DELIVERY -> {
                navController.navigate(R.id.deliveryDashboardFragment)
            }
            else -> {
                navController.navigate(R.id.homeFragment)
            }
        }
        updateBottomNavigationMenu(navController.currentDestination?.id ?: 0)
    }

    private fun subscribeClickListeners() {
        binding.signOut.setOnClickListener(this)
        binding.favorite.setOnClickListener(this)
        binding.home.setOnClickListener(this)
        binding.history.setOnClickListener(this)
        binding.profile.setOnClickListener(this)
    }

    private fun configToolbar(currentDestinationId: Int?) {
        when (currentDestinationId) {
            R.id.homeFragment -> {
                setToolbarTitle(R.string.home)
                setHomeToolbarIcon()
            }
            R.id.foodDetailsFragment -> setFavoriteToolbarIcon()
            R.id.userFragment -> {
                val title = if (userRole == Client.ROLE_DELIVERY) "Profil Livreur" else if (userRole == Client.ROLE_ADMIN) "Profil Admin" else "User"
                binding.toolbarTitle.text = title
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.adminDashboardFragment -> {
                binding.toolbarTitle.text = "Admin Dashboard"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.deliveryDashboardFragment -> {
                binding.toolbarTitle.text = "Driver Dashboard"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.driverOrdersFragment -> {
                binding.toolbarTitle.text = "Commandes"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.adminOrdersFragment -> {
                binding.toolbarTitle.text = "Orders"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.adminDriversFragment -> {
                binding.toolbarTitle.text = "Manage Drivers"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.adminUsersFragment -> {
                binding.toolbarTitle.text = "Manage Users"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.mapsFragment -> {
                binding.toolbarTitle.text = "Choisir une adresse"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            R.id.deliveryTrackingFragment -> {
                binding.toolbarTitle.text = "Suivi de livraison"
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
            else -> {
                binding.favoriteIcon.hide()
                binding.bubbleCart.hide()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun setToolbarTitle(@StringRes title: Int) {
        binding.toolbarTitle.text = getString(title)
    }

    fun setToolbarTitleString(title: String) {
        binding.toolbarTitle.text = title
    }

    private fun setHomeToolbarIcon() {
        binding.favoriteIcon.hide()
        binding.bubbleCart.show()
    }

    private fun setFavoriteToolbarIcon() {
        binding.bubbleCart.hide()
        binding.favoriteIcon.show()
    }

    fun getBubbleCart() = binding.bubbleCart
    fun getHomeToolbarIcon() = binding.bubbleCart.toolbarIcon
    fun getFavoriteToolbarIcon() = binding.favoriteIcon
    fun getToolbar() = binding.toolbar
    fun getBottomNav() = binding.bottomNav

    fun showProgressBar() {
        binding.progressBar.show()
    }

    fun hideProgressBar() {
        binding.progressBar.hide()
    }

    override fun onBackPressed() {
        if (isDrawerActive) {
            binding.motionLayout.transitionToStart()
            return
        }
        super.onBackPressed()
    }

    override fun onClick(v: View?) {
        if (isDrawerActive) binding.motionLayout.transitionToStart()

        when (v?.id) {
            R.id.sign_out -> signOut()
            R.id.favorite -> binding.bottomNav.selectedItemId = R.id.favoritesFragment
            R.id.history -> {
                if (userRole == Client.ROLE_DELIVERY) {
                    binding.bottomNav.selectedItemId = R.id.driverOrdersFragment
                } else if (userRole == Client.ROLE_ADMIN) {
                    binding.bottomNav.selectedItemId = R.id.adminOrdersFragment
                } else {
                    binding.bottomNav.selectedItemId = R.id.historyFragment
                }
            }
            R.id.home -> {
                if (userRole == Client.ROLE_ADMIN) {
                    binding.bottomNav.selectedItemId = R.id.adminDashboardFragment
                } else if (userRole == Client.ROLE_DELIVERY) {
                    binding.bottomNav.selectedItemId = R.id.deliveryDashboardFragment
                } else {
                    binding.bottomNav.selectedItemId = R.id.homeFragment
                }
            }
            R.id.profile -> binding.bottomNav.selectedItemId = R.id.userFragment
        }
    }

    private fun signOut() {
        Firebase.auth.signOut()
        getSharedPreferences(Constants.FCM_TOKEN_PREF, MODE_PRIVATE).edit().clear().apply()
        moveTo(LoginActivity::class.java)
    }
}