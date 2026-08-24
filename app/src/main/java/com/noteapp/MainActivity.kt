package com.noteapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.noteapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "MainActivity onCreate started")
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                ?: run {
                    Log.e(TAG, "NavHostFragment not found")
                    finish()
                    return
                }

            val navController = navHostFragment.navController

            binding.bottomNav.setupWithNavController(navController)

            navController.addOnDestinationChangedListener { _, destination, _ ->
                when (destination.id) {
                    R.id.homeFragment, R.id.categoryFragment, R.id.settingsFragment -> {
                        binding.bottomNav.visibility = android.view.View.VISIBLE
                    }
                    else -> {
                        binding.bottomNav.visibility = android.view.View.GONE
                    }
                }
            }
            Log.d(TAG, "MainActivity setup completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error in MainActivity onCreate", e)
            finish()
        }
    }
}
