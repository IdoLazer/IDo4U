package com.my.ido4u

import android.location.Location

data class WifiConditionData(var bssid: String) {
    override fun toString(): String {
        return "Wifi network connected: $bssid"
    }
}

data class BluetoothConditionData(var bluetoothName: String) {
    override fun toString(): String {
        return "Bluetooth connected: $bluetoothName"
    }
}

data class LocationConditionData(var location: Location, var radius: Float) {
    override fun toString(): String {
        return "Location is within radius of $radius from location (${location.latitude}, ${location.longitude}"
    }
}