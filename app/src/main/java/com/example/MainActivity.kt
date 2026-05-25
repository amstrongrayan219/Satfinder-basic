package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.data.AppDatabase
import com.example.data.SatelliteRepository
import com.example.ui.MainScreen
import com.example.ui.SatFinderViewModel
import com.example.ui.SatFinderViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: SatFinderViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database and Repository under lifecycle coroutine scope
        val database = AppDatabase.getDatabase(applicationContext, lifecycleScope)
        val repository = SatelliteRepository(database.satelliteDao())
        
        // Instantiate the ViewModel
        val factory = SatFinderViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[SatFinderViewModel::class.java]

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume tracking when returning to the app
        viewModel.startTracking()
    }

    override fun onPause() {
        super.onPause()
        // Stop tracking when app is in background to preserve system battery
        viewModel.stopTracking()
    }
}
