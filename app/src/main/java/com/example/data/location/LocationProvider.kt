package com.example.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationProvider(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return suspendCancellableCoroutine { continuation ->
            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    // Fallback to lastLocation if current location returned null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        continuation.resume(lastLoc)
                    }.addOnFailureListener {
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener {
                // On failure, try lastLocation
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                    continuation.resume(lastLoc)
                }.addOnFailureListener {
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                cts.cancel()
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 10000L): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
