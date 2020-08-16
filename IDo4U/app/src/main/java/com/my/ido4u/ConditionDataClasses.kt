package com.my.ido4u

import android.location.Location
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo

data class WifiConditionData(var bssid: String)

data class BluetoothConditionData(var bluetoothName: String, var hardwareAddress : String)
data class LocationConditionData(var location : Location, var radius: Float)

