package com.my.ido4u


/**
 * A class that contains all the data needed for a WiFi condition.
 * ssid: the name of the WiFi connection.
 */

data class WifiConditionData(var bssid: String, var ssid: String) {

    override fun toString(): String {
        return "Wifi network connected: $ssid"
    }
}

/**
 * A class that contains all the data needed for a Bluetooth condition.
 * bluetoothName: the name of the bluetooth device.
 * hardwareAddress: the unique hardware address of the bluetooth device.
 */
data class BluetoothConditionData(var bluetoothName: String, var hardwareAddress : String) {
    override fun toString(): String {
        return "Bluetooth connected: $bluetoothName"
    }
}

/**
 * A class that contains all the data needed for a Location condition.
 * longitude: the longitude of the center of the area in which the conditioned is fulfilled.
 * latitude: the latitude of the center of the area in which the conditioned is fulfilled.
 * radius: the radius of the circle in which the conditioned is fulfilled.
 */
data class LocationConditionData(var longitude: Double, var latitude: Double, var radius: Float) {
    override fun toString(): String {
        return "Location is within radius of $radius from the coordinates: ($latitude, $longitude)"
    }
}

