package com.my.ido4u

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.shape.RoundedRectangle
import java.util.*


/**
 * A file common to all classes and activities, in which all the constants and methods that are
 * being used by several components.
 */

///////////////////////////////////////// Constants ////////////////////////////////////////////////
const val CHANNEL_ID = "stickyChannel"

const val QUIT = "quit"
const val QUIT_ID = 0
const val FOREGROUND_ID = 1
const val WIFI_CHANGED_BROADCAST = WifiManager.NETWORK_STATE_CHANGED_ACTION
const val BLUETOOTH_CHANGED_BROADCAST = BluetoothDevice.ACTION_ACL_CONNECTED
const val THRESHOLD_ACCURACY = 50
const val LOCATION_REQUEST_INTERVALS: Long = 5
const val WIFI_PERMISSION_REQUEST_CODE = 0
const val  BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1

const val MAP_PIN_LOCATION_REQUEST_CODE = 5
const val DEFAULT_RADIUS = 500f
const val RADIUS_MAX_IN_METERS = 5000
const val CENTER_MARKER = "centerMarker"
const val MAP_LOCATION_ACTION = "mapLocationAction"
const val CHOOSE_CONDITION_ACTION = "chooseConditionAction"
const val MARKER_LAT_LNG = "markerLatLng"
const val RADIUS = "radius"

const val CHOOSE_APP_REQUEST_CODE = 6 // todo - needed?

const val CONDITION = "condition"
const val ACTION = "action"

/** Condition Request Codes */
const val CHOOSE_CONDITION_REQUEST_CODE = 6
const val CHOOSE_LOCATION_CONDITION_REQUEST_CODE = 7
const val CHOOSE_WIFI_CONDITION_REQUEST_CODE = 8
const val CHOOSE_BLUETOOTH_CONDITION_REQUEST_CODE = 9

/** IMPORTANT: add all new condition request codes to this list*/
val CONDITION_REQUEST_CODES =
    listOf(
        CHOOSE_LOCATION_CONDITION_REQUEST_CODE,
        CHOOSE_WIFI_CONDITION_REQUEST_CODE,
        CHOOSE_BLUETOOTH_CONDITION_REQUEST_CODE
    )

/** Action Request Codes*/
const val CHOOSE_ACTION_REQUEST_CODE = 10

const val CHOOSE_APP_ACTION_REQUEST_CODE = 11

/** IMPORTANT: add all new action request codes to this list*/
val ACTION_REQUEST_CODES =
    listOf(
        CHOOSE_APP_ACTION_REQUEST_CODE
    )


/////////////////////////// Permission - related methods ///////////////////////////////////////////
fun checkConditionsPermissions(type: Task.ConditionEnum, activity: Activity): Boolean {
    when (type) {

        Task.ConditionEnum.WIFI -> checkSpecificPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            WIFI_PERMISSION_REQUEST_CODE, activity
        )

        Task.ConditionEnum.BLUETOOTH -> checkSpecificPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            BLUETOOTH_PERMISSIONS_REQUEST_CODE, activity
        )

        Task.ConditionEnum.TIME -> {} //todo

        Task.ConditionEnum.LOCATION -> checkSpecificPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            WIFI_PERMISSION_REQUEST_CODE, activity
        )
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

/**
 * Checks whether or not the location services are enabled.
 * @return true if yes, false otherwise.
 */
fun isLocationEnabled(context: Context): Boolean {
    val lm = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var gpsEnabled = false
    var networkEnabled = false
    try {
        gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
    } catch (ex: Exception) {
    }
    try {
        networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (ex: Exception) {
    }
    return gpsEnabled && networkEnabled
}

/**
 * Checks if all the permissions in permissionsList has been granted - if so returns true,
 * else - calls ActivityCompat.requestPermissions on all the un-granted permissions.
 */
private fun checkSpecificPermissions(
    permissionsList: MutableList<String>,
    requestCode: Int, activity: Activity
): Boolean {
    val unGrantedPermissionsList: MutableList<String> = ArrayList()
    for (permission in permissionsList) {
        if (checkPermission(permission, activity)) {
            unGrantedPermissionsList.add(permission)
        }
    }
    if (unGrantedPermissionsList.size > 0) {
        ActivityCompat.requestPermissions(
            activity,
            unGrantedPermissionsList.toTypedArray(),
            requestCode

        )
        return false
    }
    return true
}


/**
 * Return true if permission has been granted, false otherwise.
 */
private fun checkPermission(permission: String, context: Context): Boolean {
    val granted = PackageManager.PERMISSION_GRANTED
    return ContextCompat.checkSelfPermission(context, permission) != granted
}

@RequiresApi(Build.VERSION_CODES.M)
/**
 * Checks if all the relevant permissions for the action type "type" has been granted and
 * requests those who has'nt been granted yet.
 */

fun checkActionsPermissions(type: Task.ActionEnum, context: Context) : Boolean{
    when(type){
        Task.ActionEnum.VOLUME -> checkSoundPermissions(context)
        Task.ActionEnum.BRIGHTNESS -> checkBrightnessPermission(context)
    }
    return true

}

@RequiresApi(Build.VERSION_CODES.M)
/**
 * Checks if the application is allowed to change screen brightness.
 * If it is - the method returns true, else -  asks the permission from the user and sends him\her
 * to an activity in which it could be granted.
 */
fun checkBrightnessPermission(context: Context): Boolean {
    if (!Settings.System.canWrite(context)) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        return true
    }
    return false
}


//@RequiresApi(Build.VERSION_CODES.M)
//        /**
//         * todo
//         */
//fun askBrightnessPermission(
//    task: Task,
//    context: Context
//) { //todo - delete and check permissions in task creation
//    for (action in task.actions) {
//        if (action.actionType == Task.ActionEnum.BRIGHTNESS) {
//            if (!Settings.System.canWrite(context)) {
//                context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
//            }
//        }
//
//    }
//    return false
//}

/**
 * Checks if the application is allowed to change the device's volume mode and level.
 * If it is - the method returns true, else -  asks the permission from the user and sends him\her
 * to an activity in which it could be granted.
 */
fun checkSoundPermissions(context: Context): Boolean{
    val notificationMngr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
        !notificationMngr.isNotificationPolicyAccessGranted) {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        return false
    }
    return true
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
 * Creates a tutorial with all the vies in views as spotlight targets and all the strings in texts
 * as tutorial text explanations
 */
//todo - should only happen at first launch in every activity!
fun createTutorial(activity: Activity, texts: List<String>, vararg views: View) {
    val firstRoot = FrameLayout(activity)
    val layout = activity.layoutInflater.inflate(R.layout.layout_target, firstRoot)
    createSpotlightWhenViewIsInflated(layout, activity, texts, *views)
}

/**
 * Adds a listener which create a target and spotlight around a view when it is inflated
 */
fun createSpotlightWhenViewIsInflated(
    layout: View,
    activity: Activity,
    texts: List<String>,
    vararg views: View
) {
    views[0].viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val targets: MutableList<Target> = mutableListOf()
                for (view in views) {
                    targets.add(createTarget(view, layout))
                }
                val targetsArray = targets.toTypedArray()
                val spotlight = createSpotlight(activity, *targetsArray)
                val textIterator = texts.iterator()
                val targetText = layout.findViewById<TextView>(R.id.target_text)
                targetText.text = textIterator.next()
                val nextSpotlight = View.OnClickListener {
                    spotlight.next()
                    if (textIterator.hasNext()) {
                        targetText.text = textIterator.next()
                    }
                }
                val stopSpotlight = View.OnClickListener { spotlight.finish() }
                layout.findViewById<View>(R.id.close_spotlight)
                    .setOnClickListener(nextSpotlight)
                layout.findViewById<View>(R.id.next_button).setOnClickListener(stopSpotlight)
                spotlight.start()
                views[0].viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
}

/**
 * Creates a target for a tutorial spotlight. The target will be illuminated by a circle.
 */
private fun createTarget(view: View, layout: View): Target {
    return Target.Builder()
        .setAnchor(view)
        .setShape(RoundedRectangle(view.height.toFloat(), view.width.toFloat(), 100f))
        .setEffect(
            RectangleRippleEffect(
                view.height.toFloat(),
                view.width.toFloat(),
                100f,
                200f,
                Color.argb(30, 124, 255, 90))
        )
        .setOverlay(layout)
        .build()
}

/**
 * Create a tutorial spotlight around the target provided
 */

fun createSpotlight(activity: Activity, vararg targets: Target): Spotlight {
    return Spotlight.Builder(activity)
        .setTargets(*targets)
        .setBackgroundColor(R.color.spotlightBackground)
        .setDuration(1000L)
        .setAnimation(DecelerateInterpolator(2f))
        .build()
}


//////////////////////////// Wifi - related methods ////////////////////////////////////////////
/**
 * Scans the available wifi access points and registers a broadcastListener that listens to the
 * scan's results. This method also initializes wifiManager, so its' return value should be used as
 * one.
 */
fun scanWifi(
    activity: Activity,
    wifiManager: WifiManager?,
    onRecieveNetworks: (_: List<ScanResult>) -> Unit
): BroadcastReceiver? {
    if (checkConditionsPermissions(Task.ConditionEnum.WIFI, activity)) {
        val wifiScanReceiver = object : BroadcastReceiver() {

            @RequiresApi(api = Build.VERSION_CODES.M)
            override fun onReceive(c: Context, intent: Intent) {
                val success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED,
                    false
                )
                if (success) {
//                    scanSuccess(wifiManager)
                    onRecieveNetworks(wifiManager!!.scanResults)
                } else scanFailure()
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
    for (result in results) {
        Log.e("found_wifi_start", result.SSID)
    }
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
///**
// * Returns a list of all the bluetooth devices that were paired with the device.
// */
//fun getPairedBluetoothDevices(): Set<BluetoothDevice>? {
//    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
//    val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
//    return pairedDevices
//}

/**
 * Opens a pop-up that allows the user to choose an application from his\her device.
 * In order to get the chosen app's information the programmer should override the
 * onActivityResult in the calling activity and use data.getComponent().
 */
fun chooseApp(activity: Activity) {
    val mainIntent = Intent(Intent.ACTION_MAIN, null)
    mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
    val pickIntent = Intent(Intent.ACTION_PICK_ACTIVITY)
    pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent)
    activity.startActivityForResult(pickIntent, CHOOSE_APP_ACTION_REQUEST_CODE)
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

/**
 * Returns a list of all the bluetooth devices that were paired with the device.
 */
fun getBluetoothDevices(): Set<BluetoothDevice>? {
    val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    return bluetoothAdapter?.bondedDevices
}

/**
 * Starts the service that continuously checks for conditions.
 */
public fun startService(context: Context) {
    val serviceIntent = Intent(context, BroadcastReceiverService::class.java)
    serviceIntent.putExtra("inputExtra", "listening")
    context.startService(serviceIntent)
}



