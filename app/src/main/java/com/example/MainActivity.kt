package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.MainScreen
import com.example.ui.theme.GeoConnectTheme
import com.example.ui.theme.GeoDarkBackground
import com.example.viewmodel.GeoConnectViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: GeoConnectViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      GeoConnectTheme {
        val permissionsToRequest = buildList {
          add(Manifest.permission.ACCESS_FINE_LOCATION)
          add(Manifest.permission.ACCESS_COARSE_LOCATION)
          add(Manifest.permission.RECORD_AUDIO)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
          }
        }.toTypedArray()

        val multiplePermissionsLauncher = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsMap ->
          val locationGranted = permissionsMap[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                  permissionsMap[Manifest.permission.ACCESS_COARSE_LOCATION] == true
          viewModel.onLocationPermissionResult(locationGranted)
          if (locationGranted) {
            viewModel.refreshLocationFromGps()
          }
        }

        LaunchedEffect(Unit) {
          val needsRequest = permissionsToRequest.any { perm ->
            ContextCompat.checkSelfPermission(this@MainActivity, perm) != PackageManager.PERMISSION_GRANTED
          }
          if (needsRequest) {
            multiplePermissionsLauncher.launch(permissionsToRequest)
          } else {
            viewModel.onLocationPermissionResult(true)
            viewModel.refreshLocationFromGps()
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = GeoDarkBackground
        ) {
          MainScreen(viewModel = viewModel)
        }
      }
    }
  }
}


