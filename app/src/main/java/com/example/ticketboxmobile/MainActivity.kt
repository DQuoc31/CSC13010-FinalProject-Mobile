package com.example.ticketboxmobile

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ticketboxmobile.data.TicketDatabase
import com.example.ticketboxmobile.p2p.P2PManager
import com.example.ticketboxmobile.ui.screens.HubScreen
import com.example.ticketboxmobile.ui.screens.RoleSelectionScreen
import com.example.ticketboxmobile.ui.screens.ScannerScreen
import com.example.ticketboxmobile.ui.screens.LoginScreen
import com.example.ticketboxmobile.ui.theme.TicketboxMobileTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request necessary permissions
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        requestPermissionLauncher.launch(permissions.toTypedArray())

        val p2pManager = P2PManager(this)
        val ticketDao = TicketDatabase.getDatabase(this).ticketDao()
        
        val sharedPref = getSharedPreferences("ticketbox_prefs", android.content.Context.MODE_PRIVATE)
        val savedToken = sharedPref.getString("token", null)
        val startDest = if (savedToken != null) "role_selection" else "login"

        setContent {
            TicketboxMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = startDest) {
                        composable("login") {
                            LoginScreen(
                                onLoginSuccess = { 
                                    navController.navigate("role_selection") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onSelectHub = { 
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        ticketDao.deleteAll()
                                        withContext(Dispatchers.Main) {
                                            navController.navigate("hub")
                                        }
                                    }
                                },
                                onSelectScanner = { 
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        ticketDao.deleteAll()
                                        withContext(Dispatchers.Main) {
                                            navController.navigate("scanner")
                                        }
                                    }
                                },
                                onLogout = {
                                    sharedPref.edit().remove("token").apply()
                                    navController.navigate("login") {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("hub") {
                            val token = sharedPref.getString("token", "") ?: ""
                            HubScreen(
                                p2pManager = p2pManager,
                                ticketDao = ticketDao,
                                token = token,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("scanner") {
                            ScannerScreen(
                                p2pManager = p2pManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}