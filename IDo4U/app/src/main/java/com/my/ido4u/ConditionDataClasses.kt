package com.my.ido4u

import android.location.Location
import android.net.wifi.WifiInfo

data class WifiConditionData(var wifiInfo: WifiInfo)
data class BluetoothConditionData(var bluetoothName: String)
data class LocationConditionData(var location : Location, var radius: Float)