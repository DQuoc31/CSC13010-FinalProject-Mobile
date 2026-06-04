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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ticketboxmobile.data.TicketDatabase
import com.example.ticketboxmobile.p2p.P2PManager
import com.example.ticketboxmobile.ui.screens.HubScreen
import com.example.ticketboxmobile.ui.screens.RoleSelectionScreen
import com.example.ticketboxmobile.ui.screens.ScannerScreen
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

        setContent {
            TicketboxMobileTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "role_selection") {
                        composable("role_selection") {
                            RoleSelectionScreen(
                                onSelectHub = { navController.navigate("hub") },
                                onSelectScanner = { navController.navigate("scanner") }
                            )
                        }
                        composable("hub") {
                            HubScreen(
                                p2pManager = p2pManager,
                                ticketDao = ticketDao,
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