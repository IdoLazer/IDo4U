package com.my.ido4u

import android.location.Location

data class WifiConditionData(var bssid: String) {
    override fun toString(): String {
        return "Wifi network connected: $bssid"
    }
}


data class BluetoothConditionData(var bluetoothName: String, var hardwareAddress : String) {
    override fun toString(): String {
        return "Bluetooth connected: $bluetoothName"
    }
}

data class LocationConditionData(var longitude: Double, var latitude: Double, var radius: Float) {
    override fun toString(): String {
        return "Location is within radius of $radius from location ($latitude, $longitude"
    }
}

