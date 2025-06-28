// MainActivity.kt
package com.example.roboticsgenius

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.roboticsgenius.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the toolbar
        setSupportActionBar(binding.toolbar)

        // Set up NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.main_nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Set up AppBarConfiguration with the DrawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.timeTrackerRootFragment,
                R.id.addDataFragment,
                R.id.myDataFragment,
                R.id.settingsFragment
            ),
            binding.drawerLayout
        )

        // Connect the Toolbar with the NavController. This will automatically
        // manage the hamburger icon and back arrow.
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Connect the NavigationView with the NavController
        binding.navigationView.setupWithNavController(navController)

        // Set a listener for navigation changes to update the toolbar title
        navController.addOnDestinationChangedListener { _, destination, _ ->
            supportActionBar?.title = destination.label
        }
    }

    // This is required for the hamburger icon and back arrow to work correctly.
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }

    // Handle back press to close the drawer if it's open
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}