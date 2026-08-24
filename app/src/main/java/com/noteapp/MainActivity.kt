package com.noteapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.noteapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            val navHostFragment = (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)
                ?: (supportFragmentManager.fragments.firstOrNull { it is NavHostFragment } as? NavHostFragment)

            val navController = navHostFragment?.navController
            if (navController != null) {
                binding.bottomNav.setupWithNavController(navController)

                navController.addOnDestinationChangedListener { _, destination, _ ->
                    when (destination.id) {
                        R.id.homeFragment, R.id.categoryFragment, R.id.settingsFragment -> {
                            binding.bottomNav.visibility = View.VISIBLE
                        }
                        else -> {
                            binding.bottomNav.visibility = View.GONE
                        }
                    }
                }
            } else {
                Log.w("MainActivity", "NavController is null on startup")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error during onCreate initialization", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
