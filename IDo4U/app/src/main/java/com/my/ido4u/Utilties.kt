package com.my.ido4u

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import java.util.ArrayList


const val DEFAULT_BSSID = "02:00:00:00:00:00"
const val QUIT = "quit"
const val QUIT_ID = 0
const val FOREGROUND_ID = 1
const val WIFI_CHANGED_BROADCAST = WifiManager.NETWORK_STATE_CHANGED_ACTION
const val BLUETOOTH_CHANGED_BROADCAST = BluetoothDevice.ACTION_ACL_CONNECTED
const val THRESHOLD_ACCURACY = 50 //todo - is it a good choice
const val MINIMAL_DISTANCE_TO_LAST_LOCATION = 30 //todo - is it a good choice
const val LOCATION_REQUEST_INTERVALS: Long = 5

/**
 * Checks a if all permissions in permissionsList are granted - if so returns true, else - calls
 * ActivityCompat.requestPermissions on all the ungranted permissions.
 */
fun checkSpecificPermissions(permissionsList: MutableList<String>,
                                     requestCode : Int, activity : Activity): Boolean {
    val unGrantedPermissionsList: MutableList<String> = ArrayList()
    for(permission in permissionsList){
        if (checkWiFiPermissions(permission, activity)) {
            unGrantedPermissionsList.add(permission)
        }
    }
    if (unGrantedPermissionsList.size > 0) {
        ActivityCompat.requestPermissions(activity,
            unGrantedPermissionsList.toTypedArray(), requestCode)
        return false
    }
    return true
}

fun checkWiFiPermissions(permission : String, context: Context) : Boolean{
    val granted = PackageManager.PERMISSION_GRANTED
    return ContextCompat.checkSelfPermission(context, permission)!= granted
}

fun checkConditionsPermissions(type : Task.ConditionEnum, activity: Activity) : Boolean{
    when(type){
        Task.ConditionEnum.WIFI -> checkSpecificPermissions(mutableListOf(
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION),
            WIFI_PERMISSION_REQUEST_CODE, activity)

        Task.ConditionEnum.BLUETOOTH -> checkSpecificPermissions(mutableListOf(
            Manifest.permission.ACCESS_COARSE_LOCATION),
            BLUETOOTH_PERMISSIONS_REQUEST_CODE, activity)

        Task.ConditionEnum.TIME -> {} //todo

        Task.ConditionEnum.LOCATION -> checkSpecificPermissions(mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION),
            WIFI_PERMISSION_REQUEST_CODE, activity)
    }
    return true // todo
}

/**
 * Shows a dialog that says the location service is unavailable
 * @param context a context
 */
 fun noLocationDialog(context: Context?) {
    AlertDialog.Builder(context).setMessage(R.string.gps_network_not_enabled)
        .setPositiveButton(
            R.string.open_location_settings
        ) { _, _ ->
            context?.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
        .show()
}

@RequiresApi(Build.VERSION_CODES.M)
/**
 * todo
 */
fun askBrightnessPermission(task: Task, context: Context) { //todo - delete and check permissions in task creation
    for (action in task.actions) {
        if (action.actionType == Task.ActionEnum.BRIGHTNESS) {
            if (!Settings.System.canWrite(context)) {
                context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
            }
        }
    }
}

/**
 * Returns true if the app has the necessary permissions for location tracking, false otherwise
 */
fun hasLocationPermissions(context: Context): Boolean {
    return ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}




