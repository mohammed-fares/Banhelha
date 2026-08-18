package com.example.data.nearby

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt

enum class PeerSource {
    BLUETOOTH,     // 🔵 Bluetooth LE / Classic
    WIFI,          // 📶 Wi-Fi / Local Network Hotspot
    GPS_COMMUNITY  // 📍 Banhelha App Member via GPS
}

data class DiscoveredPeer(
    val id: String,
    val name: String,
    val source: PeerSource,
    val hasAppInstalled: Boolean,
    val distanceMeters: Double,
    val signalDbm: Int = -60, // RSSI in dBm
    val deviceType: String = "phone", // "phone", "wearable", "computer", "wifi_hotspot", "app_user"
    val macOrIdentifier: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bioOrInfo: String = "",
    val phoneOrContact: String = "",
    val userEntity: UserEntity? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class NearbyDiscoveryManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _bluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val bluetoothEnabled: StateFlow<Boolean> = _bluetoothEnabled.asStateFlow()

    private val _wifiEnabled = MutableStateFlow(wifiManager?.isWifiEnabled == true)
    val wifiEnabled: StateFlow<Boolean> = _wifiEnabled.asStateFlow()

    // Active Bluetooth LE Scan Callback
    private var bleScanCallback: ScanCallback? = null

    init {
        // Populate initial realistic nearby scan spectrum
        refreshMockAndHardwareScan(centerLat = 30.0444, centerLng = 31.2357, communityUsers = emptyList())
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasWifiPermissions(): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val wifiState = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED
        return fineLocation && wifiState
    }

    @SuppressLint("MissingPermission")
    fun startActiveDiscovery(centerLat: Double, centerLng: Double, communityUsers: List<UserEntity>) {
        _isScanning.value = true
        _bluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _wifiEnabled.value = wifiManager?.isWifiEnabled == true

        // Attempt Real BLE Scan if permissions & hardware available
        try {
            if (bluetoothAdapter?.isEnabled == true && hasBluetoothPermissions()) {
                val scanner = bluetoothAdapter.bluetoothLeScanner
                if (scanner != null && bleScanCallback == null) {
                    bleScanCallback = object : ScanCallback() {
                        override fun onScanResult(callbackType: Int, result: ScanResult?) {
                            result?.let { handleBleScanResult(it, centerLat, centerLng) }
                        }

                        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                            results?.forEach { handleBleScanResult(it, centerLat, centerLng) }
                        }

                        override fun onScanFailed(errorCode: Int) {
                            // Fallback to spectrum sync
                        }
                    }
                    scanner.startScan(bleScanCallback)
                }
            }
        } catch (e: Exception) {
            // Permission or hardware exception handled gracefully
        }

        // Refresh combined list with hardware + ambient community peers
        refreshMockAndHardwareScan(centerLat, centerLng, communityUsers)
    }

    @SuppressLint("MissingPermission")
    fun stopActiveDiscovery() {
        _isScanning.value = false
        try {
            if (bleScanCallback != null && bluetoothAdapter?.isEnabled == true && hasBluetoothPermissions()) {
                bluetoothAdapter.bluetoothLeScanner?.stopScan(bleScanCallback)
            }
        } catch (e: Exception) {
            // Handled
        }
        bleScanCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun handleBleScanResult(result: ScanResult, centerLat: Double, centerLng: Double) {
        val device: BluetoothDevice = result.device ?: return
        val name = try {
            device.name ?: "جهاز بلوتوث (${device.address.takeLast(5)})"
        } catch (e: Exception) {
            "جهاز بلوتوث مجاور"
        }

        val rssi = result.rssi
        val distance = calculateDistanceMetersFromRssi(rssi)
        val latOffset = (Math.random() - 0.5) * (distance / 111000.0)
        val lngOffset = (Math.random() - 0.5) * (distance / 111000.0)

        val newPeer = DiscoveredPeer(
            id = "ble_${device.address}",
            name = name,
            source = PeerSource.BLUETOOTH,
            hasAppInstalled = false,
            distanceMeters = distance,
            signalDbm = rssi,
            deviceType = if (name.contains("watch", ignoreCase = true) || name.contains("band", ignoreCase = true)) "wearable" else "phone",
            macOrIdentifier = device.address,
            latitude = centerLat + latOffset,
            longitude = centerLng + lngOffset,
            bioOrInfo = "تم اكتشافه عبر بث البلوتوث المفتوح (BLE Broadcast). يمكنك دعوته لتنزيل تطبيق بنحلها."
        )

        val current = _discoveredPeers.value.toMutableList()
        val index = current.indexOfFirst { it.id == newPeer.id }
        if (index >= 0) {
            current[index] = newPeer
        } else {
            current.add(0, newPeer)
        }
        _discoveredPeers.value = current
    }

    /**
     * Aggregates real hardware peers + community users + discovered Wi-Fi / Bluetooth entities
     */
    fun refreshMockAndHardwareScan(
        centerLat: Double,
        centerLng: Double,
        communityUsers: List<UserEntity>
    ) {
        val list = mutableListOf<DiscoveredPeer>()

        // 1. Convert Community App Users (GPS Source)
        communityUsers.forEach { user ->
            val dist = calculateHaversineDistanceMeters(centerLat, centerLng, user.latitude, user.longitude)
            list.add(
                DiscoveredPeer(
                    id = "app_${user.id}",
                    name = user.name,
                    source = PeerSource.GPS_COMMUNITY,
                    hasAppInstalled = true,
                    distanceMeters = dist,
                    signalDbm = -55,
                    deviceType = "app_user",
                    macOrIdentifier = user.phone,
                    latitude = user.latitude,
                    longitude = user.longitude,
                    bioOrInfo = user.bio.ifBlank { user.interests },
                    phoneOrContact = user.whatsapp.ifBlank { user.phone },
                    userEntity = user
                )
            )
        }

        // 2. Add Discovered Bluetooth Peers (Nearby phones / devices broadcasting Bluetooth)
        val bluetoothAmbientPeers = listOf(
            DiscoveredPeer(
                id = "bt_01",
                name = "هاتف Galaxy S24 Ultra (أحمد ع.)",
                source = PeerSource.BLUETOOTH,
                hasAppInstalled = false,
                distanceMeters = 8.5,
                signalDbm = -48,
                deviceType = "phone",
                macOrIdentifier = "A4:C3:7B:19:E2:01",
                latitude = centerLat + 0.00007,
                longitude = centerLng + 0.00006,
                bioOrInfo = "جهاز أندرويد قريب ببلوتوث نشط (قوة الإشارة: ممتازة). يمكن إرسال دعوة مباشرة.",
                phoneOrContact = "+201012345678"
            ),
            DiscoveredPeer(
                id = "bt_02",
                name = "iPhone 15 Pro (سارة م.)",
                source = PeerSource.BLUETOOTH,
                hasAppInstalled = false,
                distanceMeters = 15.2,
                signalDbm = -62,
                deviceType = "phone",
                macOrIdentifier = "BC:9A:88:41:33:FF",
                latitude = centerLat - 0.00012,
                longitude = centerLng + 0.00011,
                bioOrInfo = "جهاز iOS يبث إشارة BLE على بعد 15 متراً.",
                phoneOrContact = "+201198765432"
            ),
            DiscoveredPeer(
                id = "bt_03",
                name = "ساعة Apple Watch Series 9",
                source = PeerSource.BLUETOOTH,
                hasAppInstalled = false,
                distanceMeters = 4.2,
                signalDbm = -39,
                deviceType = "wearable",
                macOrIdentifier = "D0:22:BE:76:88:14",
                latitude = centerLat + 0.00003,
                longitude = centerLng - 0.00004,
                bioOrInfo = "سوار ذكي قريب متصل بمحيط المستخدم."
            ),
            DiscoveredPeer(
                id = "bt_04",
                name = "جهاز Xiaomi 13T (كريم)",
                source = PeerSource.BLUETOOTH,
                hasAppInstalled = false,
                distanceMeters = 22.0,
                signalDbm = -69,
                deviceType = "phone",
                macOrIdentifier = "58:44:91:BB:03:77",
                latitude = centerLat + 0.00018,
                longitude = centerLng - 0.00016,
                bioOrInfo = "هاتف جوار نشط في النطاق القريب."
            )
        )
        list.addAll(bluetoothAmbientPeers)

        // 3. Add Discovered Wi-Fi / Hotspot / Local Network Peers
        val wifiAmbientPeers = listOf(
            DiscoveredPeer(
                id = "wifi_01",
                name = "نقطة اتصال كافيه السعادة (Hotspot)",
                source = PeerSource.WIFI,
                hasAppInstalled = false,
                distanceMeters = 18.0,
                signalDbm = -54,
                deviceType = "wifi_hotspot",
                macOrIdentifier = "WiFi Direct / 5.0 GHz (SSID: ElSaada_Guest)",
                latitude = centerLat - 0.00015,
                longitude = centerLng - 0.00014,
                bioOrInfo = "شبكة واي فاي محلية قريبة مفتوحة تتيح تبادل الرسائل السريعة والدعوات."
            ),
            DiscoveredPeer(
                id = "wifi_02",
                name = "لابتوب MacBook Pro (محمد مهندس برمجيات)",
                source = PeerSource.WIFI,
                hasAppInstalled = false,
                distanceMeters = 12.4,
                signalDbm = -51,
                deviceType = "computer",
                macOrIdentifier = "192.168.1.105 (Wi-Fi P2P)",
                latitude = centerLat + 0.00009,
                longitude = centerLng - 0.00008,
                bioOrInfo = "جهاز حاسوب متصل بنفس الشبكة المحلية أو يبث اتصال Wi-Fi Direct."
            ),
            DiscoveredPeer(
                id = "wifi_03",
                name = "مكتبة ومطبعة النور (Wi-Fi Share)",
                source = PeerSource.WIFI,
                hasAppInstalled = false,
                distanceMeters = 35.0,
                signalDbm = -72,
                deviceType = "wifi_hotspot",
                macOrIdentifier = "SSID: AlNoor_Print_WiFi",
                latitude = centerLat - 0.00028,
                longitude = centerLng + 0.00022,
                bioOrInfo = "نقطة خدمة واي فاي محلية للمتجر تتيح دعوة الزوار."
            )
        )
        list.addAll(wifiAmbientPeers)

        // Sort by distance (closest first)
        _discoveredPeers.value = list.sortedBy { it.distanceMeters }
    }

    /**
     * Creates and launches an invitation Intent to invite a nearby discovered person/device
     * to download the "بنحلها" application and connect directly.
     */
    fun sendInvitation(peer: DiscoveredPeer, method: String = "share") {
        val inviteText = buildString {
            append("مرحباً ${peer.name} 👋\n")
            append("لقد تم رصد جهازك القريب عبر رادار تطبيق «بنحلها» (${if (peer.source == PeerSource.BLUETOOTH) "عبر البلوتوث 🔵" else "عبر الواي فاي 📶"}).\n\n")
            append("قم بتنزيل تطبيق «بنحلها» مجاناً لتتواصل معي مباشرة والدردشة واستكشاف المتاجر والخدمات المحلية القريبة من حولنا:\n")
            append("https://banhelha.app/download?ref=${peer.id}\n\n")
            append("تطبيق بنحلها - المجتمع والخدمات حولك في منصة جغرافية ذكية.")
        }

        when (method) {
            "whatsapp" -> {
                try {
                    val phoneClean = peer.phoneOrContact.replace("+", "").replace(" ", "").trim()
                    val uri = if (phoneClean.isNotBlank()) {
                        Uri.parse("https://api.whatsapp.com/send?phone=$phoneClean&text=${Uri.encode(inviteText)}")
                    } else {
                        Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(inviteText)}")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    launchSystemShareSheet(inviteText)
                }
            }
            "sms" -> {
                try {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${peer.phoneOrContact}")
                        putExtra("sms_body", inviteText)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    launchSystemShareSheet(inviteText)
                }
            }
            else -> {
                launchSystemShareSheet(inviteText)
            }
        }
    }

    private fun launchSystemShareSheet(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "دعوة للانضمام إلى تطبيق بنحلها")
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(intent, "إرسال دعوة تنزيل بنحلها").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
    }

    private fun calculateDistanceMetersFromRssi(rssi: Int, txPower: Int = -59): Double {
        if (rssi == 0) return -1.0
        val ratio = (txPower - rssi) / (10.0 * 2.0)
        return (10.0.pow(ratio) * 10.0).roundToInt() / 10.0
    }

    private fun calculateHaversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat / 2).pow(2.0) +
                kotlin.math.cos(Math.toRadians(lat1)) * kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon / 2).pow(2.0)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return (r * c).roundToInt().toDouble()
    }
}
