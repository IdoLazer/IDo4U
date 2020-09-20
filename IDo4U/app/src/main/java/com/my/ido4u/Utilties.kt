package com.my.ido4u

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.*
import android.content.Context.NOTIFICATION_SERVICE
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.effet.RippleEffect
import com.takusemba.spotlight.shape.RoundedRectangle
import java.util.ArrayList

///////////////////////////////////////// Constants ////////////////////////////////////////////////
const val DEFAULT_BSSID = "02:00:00:00:00:00"
const val QUIT = "quit"
const val QUIT_ID = 0
const val FOREGROUND_ID = 1
const val WIFI_CHANGED_BROADCAST = WifiManager.NETWORK_STATE_CHANGED_ACTION
const val BLUETOOTH_CHANGED_BROADCAST = BluetoothDevice.ACTION_ACL_CONNECTED
const val THRESHOLD_ACCURACY = 50
const val LOCATION_REQUEST_INTERVALS: Long = 5

const val WIFI_PERMISSION_REQUEST_CODE = 0
const val  BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1
const val LOCATION_PERMISSION_REQUEST_CODE = 3

const val MAP_PIN_LOCATION_REQUEST_CODE = 5
const val DEFAULT_RADIUS = 500f
const val RADIUS_MAX_IN_METERS = 5000
const val CENTER_MARKER = "centerMarker"
const val MAP_LOCATION_ACTION = "mapLocationAction"
const val MARKER_LAT_LNG = "markerLatLng"
const val RADIUS = "radius"

const val CHOOSE_APP_REQUEST_CODE = 6

/////////////////////////// Permission - related methods ///////////////////////////////////////////
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
 * Checks a if all permissions in permissionsList are granted - if so returns true, else - calls
 * ActivityCompat.requestPermissions on all the ungranted permissions.
 */
private fun checkSpecificPermissions(permissionsList: MutableList<String>,
                             requestCode : Int, activity : Activity): Boolean {
    val unGrantedPermissionsList: MutableList<String> = ArrayList()
    for(permission in permissionsList){
        if (checkPermission(permission, activity)) {
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

private fun checkPermission(permission : String, context: Context) : Boolean{
    val granted = PackageManager.PERMISSION_GRANTED
    return ContextCompat.checkSelfPermission(context, permission)!= granted
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
fun askBrightnessPermission(context: Context) { //todo - delete and check permissions in task creation
    if (!Settings.System.canWrite(context)) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
    }
}

fun checkSoundPermissions(context: Context){
    val notificationMngr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationMngr.isNotificationPolicyAccessGranted) { //todo - uneccesry
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
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

//////////////////////// Tutorial - related methods ////////////////////////////////////////////////
/**
 * Create a tutorial spotlight around the target provided
 */
fun createSpotlight(target: Target, activity: Activity): Spotlight {
    return Spotlight.Builder(activity)
        .setTargets(target)
        .setBackgroundColor(R.color.spotlightBackground)
        .setDuration(1000L)
        .setAnimation(DecelerateInterpolator(2f))
        .build()
}

/**
 * Adds a listener which create a target and spotlight around a view when it is inflated
 */
fun createSpotlightWhenViewIsInflated(button: View, layout: View, activity: Activity) {
    button.viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val target = createTarget(button, layout)
                val spotlight = createSpotlight(target, activity)
                val closeSpotlight = View.OnClickListener { spotlight.finish() }
                layout.findViewById<View>(R.id.close_spotlight)
                    .setOnClickListener(closeSpotlight)
                spotlight.start()
                button.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
}

/**
 * Creates a target for a tutorial spotlight. The target will be illuminated by a circle.
 */
private fun createTarget(button: View, layout: View): Target {
    return Target.Builder()
        .setAnchor(button)//findViewById<View>(R.id.add_task_button))
        .setShape(RoundedRectangle(button.height.toFloat(), button.width.toFloat(), 100f))
        .setEffect(
            RippleEffect(100f, 200f, Color.argb(30, 124, 255, 90))
        )
        .setOverlay(layout)
        .build()
}

/**
 * Creates a tutorial
 */
//todo - add to parameters a list of strings to be presented and change activity to a list of
// activities to be spotlighted one after the other
fun createTutorial(activity: Activity, viewId: Int) {
    val firstRoot = FrameLayout(activity)
    val layout = activity.layoutInflater.inflate(R.layout.layout_target, firstRoot)
    val button = activity.findViewById<View>(viewId)
    createSpotlightWhenViewIsInflated(button, layout, activity) //todo - should only happen at first launch!
}

//////////////////////////// Wifi - related methods ////////////////////////////////////////////
/**
 * Scans the available wifi access points and registers a broadcastListener that listens to the
 * scan's results. This method also initializes wifiManager, so its' return value should be used as
 * one.
 */
fun scanWifi(activity: Activity, wifiManager: WifiManager?): BroadcastReceiver?{
    if (checkConditionsPermissions(Task.ConditionEnum.WIFI, activity)){
        val wifiScanReceiver = object : BroadcastReceiver() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            override fun onReceive(c: Context, intent: Intent) {
                val success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED,
                    false
                )
                if (success) scanSuccess(wifiManager) else scanFailure()
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        activity.registerReceiver(wifiScanReceiver, intentFilter)
        val success = wifiManager!!.startScan()
        if (!success) scanFailure()
        return wifiScanReceiver
    }
    return null
}

/**
 * Handles wifi scan success
 */
private fun scanSuccess(wifiManager: WifiManager?) { //todo - present results to user (Lazer)
    val results = wifiManager!!.scanResults
    Log.e("found_wifi_start", results.toString())
}

/**
 * Handles failure: new scan did NOT succeed
 */
private fun scanFailure() {
    //todo consider using old scan results: these are the OLD results:
    // val results = wifiManager!!.scanResults
    Log.e("found_wifi_start", "wifi scan problem!")
}

///////////////////////////// Bluetooth related methods ////////////////////////////////////////////
fun getPairedBluetoothDevices(): Set<BluetoothDevice>? {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
    return pairedDevices
}

fun chooseApp(activity: Activity){
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    val pickIntent = Intent(Intent.ACTION_PICK_ACTIVITY)
    pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent)
    activity.startActivityForResult(pickIntent, CHOOSE_APP_REQUEST_CODE)
    // todo - add this code to the calling app:
    //    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    //        super.onActivityResult(requestCode, resultCode, data)
    //        if (requestCode == CHOOSE_APP_REQUEST_CODE &&
    //              resultCode == Activity.RESULT_OK && data != null) {
    //            val componentName: ComponentName? = data.getComponent()
    //            if(componentName != null) {
    //                val packageName = componentName.packageName
    //                val activityName = componentName.className
    //            }
    //        }
    //    }


}



