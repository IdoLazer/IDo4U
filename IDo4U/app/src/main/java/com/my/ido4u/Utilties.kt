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
import android.content.res.Configuration
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
import android.widget.ScrollView
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

/** Shared constants */
const val CHANNEL_ID = "stickyChannel"
const val SHARED_PREFERENCES_NAME = "TaskManagerSharedPreferences"
const val TASK_LIST = "taskList"
const val CONDITION = "condition"
const val ACTION = "action"

/** Service related constants */
const val QUIT = "quit"
const val QUIT_ID = 0
const val FOREGROUND_ID = 1

/** Permission request codes */
const val WIFI_PERMISSION_REQUEST_CODE = 0
const val LOCATION_PERMISSIONS_REQUEST_CODE = 1

/** Names of actions in intents */
const val WIFI_CHANGED_BROADCAST = WifiManager.NETWORK_STATE_CHANGED_ACTION
const val BLUETOOTH_CHANGED_BROADCAST = BluetoothDevice.ACTION_ACL_CONNECTED

/** Wifi activity constant */
const val SCAN_RESULTS = "scan results"

/** Location activity constant */
const val BACKUP_CENTER_LOCATION = "backupCenterLocation"
const val DEFAULT_RADIUS = 50f
const val THRESHOLD_ACCURACY = 50
const val RADIUS_MAX_IN_METERS = 5000
const val CENTER_MARKER = "centerMarker"
const val MAP_LOCATION_ACTION = "mapLocationAction"
const val LOCATION_REQUEST_INTERVALS: Long = 5

/** SP tutorial constants */
const val SHOWED_MAIN_ACTIVITY_TUTORIAL = "showed mainActivity tutorial"
const val SHOWED_LOCATION_CHOICE_ACTIVITY_TUTORIAL = "showed location choice tutorial"
const val SHOWED_TASK_PROFILE_TUTORIAL = "showed_task_profile_tutorial"
const val SHOWED_WIFI_TUTORIAL = "showed wifi tutorial"

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
const val CHOOSE_VOLUME_ACTION_REQUEST_CODE = 12
const val CHOOSE_BRIGHTNESS_ACTION_REQUEST_CODE = 13

/** IMPORTANT: add all new action request codes to this list*/
val ACTION_REQUEST_CODES =
    listOf(
        CHOOSE_APP_ACTION_REQUEST_CODE,
        CHOOSE_VOLUME_ACTION_REQUEST_CODE,
        CHOOSE_BRIGHTNESS_ACTION_REQUEST_CODE
    )

/////////////////////////// Permission - related methods ///////////////////////////////////////////
/**
 * Returns true if all the permissions relevant to the condition type are granted.
 * Else, tries to request the user for the un-granted permissions.
 * At the end - return true if all permissions were granted, false otherwise.
 */
fun checkConditionsPermissions(type: Task.ConditionEnum, activity: Activity): Boolean {
    when (type) {

        Task.ConditionEnum.WIFI -> return checkSpecificPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            WIFI_PERMISSION_REQUEST_CODE, activity
        )

//        Task.ConditionEnum.TIME -> {
//        } //todo

        Task.ConditionEnum.LOCATION -> return checkSpecificPermissions(
            mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            LOCATION_PERMISSIONS_REQUEST_CODE, activity
        )

        else -> return false
    }
}


/**
 * Checks whether or not the location services are enabled.
 * @return true if yes, false otherwise.
 */
fun isLocationEnabled(context: Context): Boolean {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
 * Checks if all the permissions in permissionsList has been granted. If one of them hasn't,
 * the method presents an informative message explaining why we need the permission, after which
 * the permission is requested from the user.
 * Returns true if at the end of the process all permissions were granted, false otherwise
 */
private fun checkSpecificPermissions(
    permissionsList: MutableList<String>,
    requestCode: Int, activity: Activity
): Boolean {
    val unGrantedPermissionsList: MutableList<String> = ArrayList()
    for (permission in permissionsList) {

        if (!checkPermission(permission, activity)) {
            var text = ""
            when (permission) {
                Manifest.permission.ACCESS_COARSE_LOCATION ->
                    text = activity.getString(R.string.location_permission_explanation)

                Manifest.permission.ACCESS_FINE_LOCATION ->
                    text = activity.getString(R.string.location_permission_explanation)

                Manifest.permission.ACCESS_WIFI_STATE ->
                    text = activity.getString(R.string.wifi_access_permission_explanation)

                Manifest.permission.CHANGE_WIFI_STATE ->
                    text = activity.getString(R.string.wifi_change_permission_explanation)

            }
            showPermissionRationalDialog(activity, text, permission, requestCode)
        }

        if (!checkPermission(permission, activity)) {
            unGrantedPermissionsList.add(permission)
        }
    }
    return unGrantedPermissionsList.size <= 0
}

/**
 * Return true if permission has been granted, false otherwise.
 */
fun checkPermission(permission: String, activity: Activity): Boolean {
    val granted = PackageManager.PERMISSION_GRANTED
    return ContextCompat.checkSelfPermission(activity, permission) == granted
}

/**
 * Presents an informative message explaining why we need the permission, after which
 * the permission is requested from the user.
 */
@RequiresApi(Build.VERSION_CODES.M)  //todo
fun showPermissionRationalDialog(activity: Activity, text: String, permission: String, code: Int) {
    AlertDialog.Builder(activity)
        .setTitle("We need your permission")
        .setMessage(text)
        .setPositiveButton(android.R.string.yes
        ) { dialog, which ->
            activity.requestPermissions(
                arrayOf(permission),
                code
            )
        }
        .setIcon(R.drawable.ic_baseline_announcement_24)
        .show()
}

/**
 * Checks if all the relevant permissions for the action type "type" has been granted and
 * requests those who has'nt been granted yet.
 */
fun checkActionsPermissions(type: Task.ActionEnum, context: Context): Boolean {
    when (type) {
        Task.ActionEnum.VOLUME -> return checkSoundPermissions(context)
        Task.ActionEnum.BRIGHTNESS -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context)
        }
        else -> return false
    }
    return true
}


/**
 * Checks if the application is allowed to change the device's volume mode and level.
 * If it is - the method returns true, else -  asks the permission from the user and sends him\her
 * to an activity in which it could be granted.
 */
fun checkSoundPermissions(context: Context): Boolean {
    val notificationMngr = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    return !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            !notificationMngr.isNotificationPolicyAccessGranted)
}

/**
 * Shows a dialog in which we ask the user to give us permissions to modify notification do not
 * disturb policy in order to perform sound Actions.
 */
fun showVolumePermissionsDialog(activity: Activity) {
    AlertDialog.Builder(activity)
        .setTitle("Sound Permissions")
        .setMessage(activity.getString(R.string.volume_permissions_explanation))
        .setPositiveButton(android.R.string.yes, object : DialogInterface.OnClickListener {
            @RequiresApi(Build.VERSION_CODES.M) //todo
            override fun onClick(dialog: DialogInterface?, which: Int) {
                activity.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            }
        })
        .setNegativeButton(android.R.string.no, null)
        .setIcon(R.drawable.ic_baseline_volume_up_24)
        .show()
}

/**
 * Shows an informative dialog, explaining why the app needs permission to change screen brightness.
 * If the user clicks "OK", he\she is sent to an activity in which he\she can grant this permission.
 */
@RequiresApi(Build.VERSION_CODES.M)  //todo
fun showBrightnessPermissionsDialog(activity: Activity, brightness: Float) {
    AlertDialog.Builder(activity)
        .setTitle("Brightness Permissions")
        .setMessage(activity.getString(R.string.brightness_permissions_explanation))
        .setPositiveButton(
            android.R.string.yes
        ) { _, _ -> activity.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)) }
        .setNegativeButton(android.R.string.no, null)
        .setIcon(R.drawable.ic_baseline_brightness_6_24)
        .show()
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
 * Creates a tutorial with all the views in views as spotlight targets and all the strings in texts
 * as tutorial text explanations
 */
fun createTutorial(activity: Activity, texts: List<String>, toSP: String, vararg views: View) {
    val firstRoot = FrameLayout(activity)
    val layout = activity.layoutInflater.inflate(R.layout.layout_target, firstRoot)
    createSpotlightWhenViewIsInflated(layout, activity, texts, toSP, *views)
}

/**
 * Adds a listener which create a target and spotlight around a view when it is inflated
 */
fun createSpotlightWhenViewIsInflated(layout: View, activity: Activity, texts: List<String>,
    toSP: String, vararg views: View) {

    views[0].viewTreeObserver.addOnGlobalLayoutListener(
        object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val targets: MutableList<Target> = mutableListOf()
                for (view in views) { targets.add(createTarget(view, layout)) }
                val targetsArray = targets.toTypedArray()
                val spotlight = createSpotlight(activity, *targetsArray)
                val textIterator = texts.iterator()
                val targetText = layout.findViewById<TextView>(R.id.target_text)
                val nextButton = layout.findViewById<View>(R.id.close_spotlight)
                val skipTutorial = layout.findViewById<View>(R.id.next_button)
                val lowerText = layout.findViewById<TextView>(R.id.lower_text)
                val landscape = Configuration.ORIENTATION_LANDSCAPE ==
                        activity.resources.configuration.orientation
                val backDarker = ContextCompat.getColor(activity, R.color.colorBackGroundDarker)
                nextButton.setBackgroundColor(backDarker)
                skipTutorial.setBackgroundColor(backDarker)
                val sp = Ido4uApp.applicationContext()
                    .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                targetText.text = textIterator.next()
                targetText.setBackgroundColor(
                    ContextCompat.getColor(activity, R.color.spotlightTextBackground)
                )

                val nextSpotlight = View.OnClickListener(
                    nextSpot(textIterator, targetText, landscape, lowerText, sp, spotlight)
                )
                val stopSpotlight = View.OnClickListener {
                    sp.edit().putBoolean(toSP, true).apply()
                    spotlight.finish()
                }
                nextButton.setOnClickListener(nextSpotlight)
                skipTutorial.setOnClickListener(stopSpotlight)
                spotlight.start()
                views[0].viewTreeObserver.removeOnGlobalLayoutListener(this)
            }

            /**
             * Returns the callback for the "next" button in a tutorial
             */
            private fun nextSpot( textIterator: Iterator<String>,  targetText: TextView,
                landscape: Boolean, lowerText: TextView,  sp: SharedPreferences,
                                  spotlight: Spotlight ): (View) -> Unit {
                return {

                    if (textIterator.hasNext()) {
                        targetText.text = textIterator.next()
                        if (
                            (targetText.text ==
                            activity.getString(R.string.task_cond_info_tutorial) ||
                            targetText.text ==
                            activity.getString(R.string.task_add_condit_tutorial))
                            && !landscape
                        ) {
                            targetText.visibility = View.INVISIBLE
                            lowerText.visibility = View.VISIBLE
                            lowerText.text = targetText.text
                            lowerText.setBackgroundColor(
                                ContextCompat.getColor(activity, R.color.spotlightTextBackground)
                            )
                        }
                        if (
                            targetText.text == activity.getString(R.string.task_actions_info_tut)
                            && !landscape
                        ) {
                            targetText.visibility = View.VISIBLE
                            lowerText.visibility = View.INVISIBLE
                            lowerText.setBackgroundColor(Color.TRANSPARENT)
                        }

                    } else {
                        sp.edit().putBoolean(toSP, true).apply()
                    }

                    spotlight.next()
                }
            }
        })
}

/**
 * Creates a target for a tutorial spotlight. The target will be illuminated by a circle or a
 * rounded rectangle.
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
                Color.argb(60, 124, 255, 90)
            )
        )
        .setOverlay(layout)
        .build()
}

/**
 * Creates a tutorial spotlight around the target provided
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

            @RequiresApi(api = Build.VERSION_CODES.M) //todo
            override fun onReceive(c: Context, intent: Intent) {
                val success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED,
                    false
                )
                if (success) {
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
 * Handles failure: new scan did NOT succeed
 */
private fun scanFailure() {
    Log.e("problem_in_wifi_scan", "wifi scan problem!")
}

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
fun startService(context: Context) {
    val serviceIntent = Intent(context, BroadcastReceiverService::class.java)
    serviceIntent.putExtra("inputExtra", "listening")
    context.startService(serviceIntent)
}



