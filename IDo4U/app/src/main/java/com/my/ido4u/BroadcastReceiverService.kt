package com.my.ido4u

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.location.Location
import android.media.AudioManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson

/**
 * The service that runs in the foreground, listens to the relevant broadcasts (via
 * broadcastReceiver) and performs the actions of tasks whose conditions became fulfilled.
 */
class BroadcastReceiverService : Service() {
    private var mReceiver: BroadcastReceiver? = null
    private var actionsToListenTo : HashSet<String> = HashSet()
    private var taskList : MutableList<Task> = TaskManager.getTaskList()
    private var lastSSID : String? = null
    private val gson : Gson = Gson()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation : Location? = null
    private var locationTrackingStarted = false
    private var context : Context? = null
    private var locationCallback: LocationCallback? = null

    /**
     * These commands will be performed whenever StartService is called for this class
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        context = applicationContext
        createAndRegisterBroadcastReceiver()
        startForeground(FOREGROUND_ID, createStickyNotification())
        return START_STICKY
    }

    /**
     * Stops the service
     */
    fun stop(){
        stopSelf()
    }

    /**
     * Creates the sticky notification which lets the user know the service is running in foreground
     */
    private fun createStickyNotification(): Notification? {
        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0)
        val qIntent = Intent(QUIT)
        val quitPendingIntent = PendingIntent.getBroadcast(this, QUIT_ID, qIntent, 0)
        val action = NotificationCompat.Action(
            R.drawable.common_google_signin_btn_text_disabled,
            "Stop listening",
            quitPendingIntent
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_message))
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .addAction(action)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(118, 0, 255))
            .setOngoing(true)
            .build()
    }

    //////////////////////////////// location tracking methods /////////////////////////////////////

    @SuppressLint("MissingPermission") //todo - problematic for google play?
    /**
     * Initializes location tracking using fuseLocation.
     */
    private fun initializeLocationTracking() {
        if (!locationTrackingStarted) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (hasLocationPermissions(applicationContext) && isLocationEnabled(applicationContext)) {
                fusedLocationClient!!.lastLocation.addOnCompleteListener {
                    requestNewLocationData()
                }
            }
            locationTrackingStarted = true
        }
    }

    /**
     * Requests location updates periodically.
     */
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.interval = LOCATION_REQUEST_INTERVALS
        locationRequest.fastestInterval = LOCATION_REQUEST_INTERVALS // the fastest interval allowed
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult.lastLocation)
            }
        }
        fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Goes through all the tasks in taskList and for each task:
     * - Checks whether it's location-conditioned
     * - If so, checks if the distance between current location and the one in the condition is
     *   smaller or equal to the condition's radius.
     * - If so, it executes the task's actions.
     */
    private fun onLocationChanged(curLocation: Location){
        if(curLocation.accuracy < THRESHOLD_ACCURACY) {
            var firstLocationQuery = false
            if (lastLocation == null) {
                lastLocation = curLocation
                firstLocationQuery = true
            }
            for (task in taskList) { //todo - doesLocationConditionApply return false if oldLocation is null + cancel second condition
                if (task.isOn) {
                    if (task.condition.conditionType == Task.ConditionEnum.LOCATION) {
                        val newLocationSatisfiesCond = doesLocationConditionApply(task, curLocation)
                        val oldLocationSatisfiesCond =
                            doesLocationConditionApply(task, lastLocation!!)
                        if ((newLocationSatisfiesCond && !oldLocationSatisfiesCond) ||
                            (newLocationSatisfiesCond && firstLocationQuery)
                        ) {
                            handleActions(task)
                        }
                    }
                }
                lastLocation = curLocation
            }
        }
    }

    /**
     * Returns true if curLocation satisfies the location condition of task, false otherwise.
     */
    private fun doesLocationConditionApply(task: Task, curLocation: Location): Boolean {
        val curLon = curLocation.longitude
        val curLat = curLocation.latitude
        val rawData = task.condition.extraData
        val data = gson.fromJson(rawData, LocationConditionData::class.java)
        val results = FloatArray(1)
        Location.distanceBetween(curLat, curLon, data.latitude, data.longitude, results)
        return results[0] <= data.radius
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * The BroadcastReceiver of the Service, it listens to the relevant broadcasts.
     */
    inner class MyReceiver : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == null) {
                Log.e("error", "action is null!")
            } else {
                when (action) { // todo - add more cases?
                    QUIT -> stop()
                    WIFI_CHANGED_BROADCAST -> handleWifiCondition()
                    BLUETOOTH_CHANGED_BROADCAST -> handleBluetoothCondition(intent)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Handles Bluetooth conditions.
     */
    private fun handleBluetoothCondition(intent : Intent) {
        val device : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        for (task in taskList) {
            if (task.isOn) {
                if (task.condition.conditionType == Task.ConditionEnum.BLUETOOTH) {
                    val rawData = task.condition.extraData
                    val conditionData = gson.fromJson(rawData, BluetoothConditionData::class.java)
                    if (device != null) {
                        if (conditionData.hardwareAddress == device.address) {
                            handleActions(task)
                        }
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Handles WiFi conditions.
     */
    private fun handleWifiCondition() {
        var curSsid : String? = null
        for (task in taskList) {
            if (task.isOn) {
                if (task.condition.conditionType == Task.ConditionEnum.WIFI) {
                    val wifiMngr =
                        applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val wifiInfo: WifiInfo = wifiMngr.connectionInfo
                    val rawExtraData = task.condition.extraData
                    val extraData = gson.fromJson(rawExtraData, WifiConditionData::class.java)
                    val taskSsid: String? = extraData.ssid
                    curSsid = wifiInfo.ssid
                    if (taskSsid != null && "\"$taskSsid\"" != lastSSID) {
                        if (curSsid == "\"$taskSsid\"") {
                            handleActions(task)
                        }
                    }
                }
            }
        }
        lastSSID = curSsid
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Handles all the actions of a task.
     */
    private fun handleActions(task: Task) {
        for (action in task.actions) {
            when (action.actionType) {
                Task.ActionEnum.TOAST -> createAndShowToast(action)  // todo - delete
                Task.ActionEnum.VOLUME -> handleVolumeActions(action)
                Task.ActionEnum.BRIGHTNESS -> handleBrightnessActions(action)
                Task.ActionEnum.APPS -> handleAppOpeningAction(action)
                Task.ActionEnum.COMMUNICATION -> {} //todo
                Task.ActionEnum.DATA -> {} //todo
            }
        }
    }

    /**
     * Tries to open the relevant app.
     */
    private fun handleAppOpeningAction(action: Task.Action) {
        val rawData = action.extraData
        val actionData = gson.fromJson(rawData, OpenAppActionData::class.java)
        val packageName = actionData.packageName
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.let { startActivity(it) }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Handles screen brightness actions.
     */
    private fun handleBrightnessActions(action : Task.Action) {
        val rawData = gson.fromJson(action.extraData, BrightnessActionData::class.java)
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context?.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, rawData.brightness
            )
        }
    }

    /**
     * Handles volume actions.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleVolumeActions(action : Task.Action) {
        val audioMngr = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val actionData = gson.fromJson(action.extraData, VolumeActionData::class.java)
        checkActionsPermissions(Task.ActionEnum.BRIGHTNESS, applicationContext)
        when (actionData.volumeAction) {
            VolumeActionData.VolumeAction.SOUND -> changeRingerVolume(audioMngr, actionData)
            VolumeActionData.VolumeAction.VIBRATE ->
                                        silenceDevice(audioMngr, AudioManager.RINGER_MODE_VIBRATE)
            VolumeActionData.VolumeAction.MUTE ->
                                        silenceDevice(audioMngr, AudioManager.RINGER_MODE_SILENT)
        }
    }

    /**
     * Changes the ringer mode of the device to mode and silences all other sound streams (music,
     * notifications and system).
     */
    private fun silenceDevice(audioMngr: AudioManager, mode: Int){
        audioMngr.ringerMode = mode
        audioMngr.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        audioMngr.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
        audioMngr.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0)
    }

    /**
     * Changes the sound level of all 4 sound streams (ringer, music, notifications
     * and system) to the target sound stored in actionData.
     */
    private fun changeRingerVolume(audioMngr: AudioManager, actionData: VolumeActionData) {
        audioMngr.ringerMode = AudioManager.RINGER_MODE_NORMAL

        val ringerMax = audioMngr.getStreamMaxVolume(AudioManager.STREAM_RING)
        val musicMax = audioMngr.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val systemMax = audioMngr.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
        val notificationMAx = audioMngr.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val targetVolume = actionData.volumeLevel / 100

        audioMngr.setStreamVolume(
            AudioManager.STREAM_RING,
            (targetVolume * ringerMax).toInt(),
            0
        )
        audioMngr.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (targetVolume * musicMax).toInt(),
            0
        )
        audioMngr.setStreamVolume(
            AudioManager.STREAM_NOTIFICATION,
            (targetVolume * notificationMAx).toInt(),
            0
        )
        audioMngr.setStreamVolume(
            AudioManager.STREAM_SYSTEM,
            (targetVolume * systemMax).toInt(),
            0
        )
    }

    private fun createAndShowToast(action : Task.Action) { //todo - delete
        val actionData = gson.fromJson(action.extraData, ToastActionData::class.java)
        val text: CharSequence = actionData.text
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.show()
    }

    /**
     * Adds to a filter all the actions to whom the broadcastReceiver should listen and creates the
     * broadcastReceiver using the filter. Finally, registers the broadcastReceiver with the filter.
     */
    private fun createAndRegisterBroadcastReceiver() {
        for(task in taskList){ //todo - should we check here if task is on?
            when(task.condition.conditionType){
                Task.ConditionEnum.WIFI -> actionsToListenTo.add(WIFI_CHANGED_BROADCAST)
                Task.ConditionEnum.BLUETOOTH -> actionsToListenTo.add(BLUETOOTH_CHANGED_BROADCAST)
                Task.ConditionEnum.LOCATION -> initializeLocationTracking()
//                Task.ConditionEnum.TIME -> {} // todo
            }
        }
        val filter = IntentFilter()
        for (action in actionsToListenTo) {
            filter.addAction(action)
        }
        filter.addAction(QUIT)
        if(mReceiver != null){
            unregisterReceiver(mReceiver)
        }
        else {
            mReceiver = MyReceiver()
        }
        registerReceiver(mReceiver, filter)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
        if(locationCallback != null) {
            fusedLocationClient!!.removeLocationUpdates(locationCallback)
        }
    }
}