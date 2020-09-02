package com.my.ido4u

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.ArrayList

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

        Task.ConditionEnum.LOCATION -> {}//todo
    }
    return true // todo
}

