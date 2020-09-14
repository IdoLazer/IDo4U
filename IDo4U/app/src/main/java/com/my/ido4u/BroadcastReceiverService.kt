package com.my.ido4u

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile.GATT
import android.bluetooth.BluetoothProfile.GATT_SERVER
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.gson.Gson

class BroadcastReceiverService : Service() {
    private var mReceiver: BroadcastReceiver? = null
    private var actionsToListenTo : HashSet<String> = HashSet<String>()
    private var taskList : MutableList<Task> = TaskManager.getTaskList() //todo - deep copy?
    private var lastRSSID : String? = null
    private val gson : Gson = Gson()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation : Location? = null
    private var locationTrackingStarted = false
    private var context : Context? = null
    private var locationCallback: LocationCallback? = null

    /**
     * These commands will be performed whenever StartService is called for this class
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        context = applicationContext
        createAndRegisterBroadcastReceiver()
        var checkedBrightnessPermissions = false
        for(task in taskList){
            val rawData = task.condition.extraData
            when(task.condition.conditionType){
                Task.ConditionEnum.BLUETOOTH -> initializeBluetoothTask(rawData, task) //todo - doesn't work!
                Task.ConditionEnum.LOCATION -> initializeLocationTracking()
//                Task.ConditionEnum.WIFI -> {} TODO()
//                Task.ConditionEnum.TIME -> {} TODO()
            }
            if(!checkedBrightnessPermissions) { //todo - delete and check permissions in task creation
                askBrightnessPermission(task, applicationContext)
                checkedBrightnessPermissions = true
            }
        }
        startForeground(FOREGROUND_ID,  createStickyNotification())
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
        var action = NotificationCompat.Action(
            R.drawable.common_google_signin_btn_text_disabled,
            "Stop listening",
            quitPendingIntent
        )
        val notification = NotificationCompat.Builder(this, Ido4uApp.CHANNEL_ID)
            .setContentTitle("Ido4u")
            .setContentText("Waiting for a condition to be filled...")
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .addAction(action)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(118, 0, 255))
            .setOngoing(true)
            .build()
        return notification
    }

    //////////////////////////////// location tracking methods /////////////////////////////////////

    @SuppressLint("MissingPermission")
    /**
     * todo
     */
    private fun initializeLocationTracking() { // todo - move to mainActivity
        if (!locationTrackingStarted) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (hasLocationPermissions(applicationContext)) {
                if (isLocationEnabled()) {
                    fusedLocationClient.lastLocation.addOnCompleteListener {
                        requestNewLocationData()
                    }
                }
                else{
                    noLocationDialog(applicationContext)
                }
            }
            else{
                //todo - show toast with explanation
            }
            locationTrackingStarted = true
        }
    }

    /**
     * Requests location updates periodically
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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Goes through all the tasks in taskList and for each task:
     * - Checks whether it's location-conditioned
     * - If so, checks if the distance between  current location and the one in the condition is
     *   smaller or equal to the condition's radius.
     * - If so, it executes the task's actions.
     */
    private fun onLocationChanged(curLocation: Location){
//        Log.e("accuracy", "${curLocation.accuracy}") //todo - remove
        if(curLocation.accuracy < THRESHOLD_ACCURACY) {
            var firstLocationQuery = false
            if (lastLocation == null){
                lastLocation = curLocation
                firstLocationQuery = true
            }
            for (task in taskList) {
                if (task.condition.conditionType == Task.ConditionEnum.LOCATION) {
                    val newLocationSatisfiesCond = doesLocationConditionApply(task, curLocation)
                    val oldLocationSatisfiesCond = doesLocationConditionApply(task, lastLocation!!)
//                    val closeToOldLocation =
//                        curLocation.distanceTo(lastLocation) <= MINIMAL_DISTANCE_TO_LAST_LOCATION
                    if ((newLocationSatisfiesCond && !oldLocationSatisfiesCond) ||
//                        (newLocationSatisfiesCond && !closeToOldLocation) ||
                        (newLocationSatisfiesCond && firstLocationQuery)) {
                        handleActions(task)
                    }
                }
            }
            lastLocation = curLocation
        }
    }

    /**
     * Returns true if curLocation satisfies the location condition of task, false otherwise
     */
    private fun doesLocationConditionApply(task: Task, curLocation: Location): Boolean {
        val curLon = curLocation.longitude
        val curLat = curLocation.latitude
        val rawData = task.condition.extraData
        val data = gson.fromJson(rawData, LocationConditionData::class.java)
        val results = FloatArray(1)
        Location.distanceBetween(curLat, curLon, data.latitude, data.longitude, results)
        if(results[0] > data.radius){ //todo - remove
            Log.e("delta", "${results[0]}")//todo - remove
        }//todo - remove
        return results[0] <= data.radius
    }

    /**
     * Checks whether or not the location services are enabled.
     * @return true if yes, false otherwise.
     */
    private fun isLocationEnabled(): Boolean {
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
                when (action) { // todo - add more cases
                    QUIT -> stop()
                    WIFI_CHANGED_BROADCAST -> handleWifiCondition()
                    BLUETOOTH_CHANGED_BROADCAST -> handleBluetoothCondition(intent)
                }
            }
        }
    }

    /**
     * Adds WifiManager.NETWORK_STATE_CHANGED_ACTION to the filter of the broadcastReceiver.
     * In addition, checks for every existing wifi conditioned task if the phone is connected
     * to its condition's wifi address and if so execute its action.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun initializeBluetoothTask(rawData: String, task: Task) { //todo - not working!
        actionsToListenTo.add(BLUETOOTH_CHANGED_BROADCAST)
        val condData = gson.fromJson(rawData, BluetoothConditionData::class.java)
        val deviceAddress = condData.hardwareAddress
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val connected: List<BluetoothDevice> = bluetoothManager.getConnectedDevices(GATT_SERVER) //todo - why is it empty?!
        for (connectedDevice in connected) {
            if (connectedDevice.address == deviceAddress) {
                handleActions(task)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleBluetoothCondition(intent : Intent) {
        var device : BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        for (task in taskList) {
            if (task.condition.conditionType == Task.ConditionEnum.BLUETOOTH) {
                val rawExtraData = task.condition.extraData
                var conditionData =
                    gson.fromJson(rawExtraData, BluetoothConditionData::class.java)
                if (device != null) {
                    if (conditionData.hardwareAddress == device.address) {
                        handleActions(task)
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleWifiCondition() {
        for (task in taskList) {
            if (task.condition.conditionType == Task.ConditionEnum.WIFI) {
                val wifiMngr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiMngr.connectionInfo
                val rawExtraData = task.condition.extraData
                val extraData = gson.fromJson(rawExtraData, WifiConditionData::class.java)
                val taskBssid: String? = extraData.bssid
                val curBssid = wifiInfo.bssid

                if (taskBssid != DEFAULT_BSSID && taskBssid != null) { //todo - only null?
                    if (curBssid == taskBssid ){
                        lastRSSID = taskBssid
                        handleActions(task)
                    }
                }
                lastRSSID = curBssid
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleActions(task: Task) {
        for (action in task.actions) {
            when (action.actionType) {
                Task.ActionEnum.TOAST -> createAndShowToast(action)  // todo - delete
                Task.ActionEnum.VOLUME -> handleVolumeActions(action)
                Task.ActionEnum.BRIGHTNESS -> handleBrightnessActions(action)
                Task.ActionEnum.APPS -> {
                    val rawData = action.extraData
                    val actionData = gson.fromJson(rawData, OpenAppActionData::class.java)
                    val packageName = actionData.packageName

                    val launchIntent =packageManager.getLaunchIntentForPackage(packageName)
                    launchIntent?.let { startActivity(it) }
                } //todo
                Task.ActionEnum.COMMUNICATION -> {} //todo
                Task.ActionEnum.DATA -> {} //todo
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleBrightnessActions(action : Task.Action) {
        val rawData = gson.fromJson(action.extraData, BrightnessActionData::class.java)
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(
                context?.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS, rawData.brightness
            )
        } else {
            //todo
        }
    }

    /**
     * todo
     */
    private fun handleVolumeActions(action : Task.Action) {
        val audioMngr = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val actionData = gson.fromJson(action.extraData, VolumeActionData::class.java)
        when (actionData.volumeAction) {
            VolumeActionData.VolumeAction.SOUND -> changeRingerModeToSound(audioMngr, actionData)
            VolumeActionData.VolumeAction.VIBRATE -> audioMngr.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            VolumeActionData.VolumeAction.MUTE -> silenceRinger(audioMngr)
        }
    }

    /**
     * Changes the cellPhones ringing mode to normal (sound) at the .
     */
    private fun changeRingerModeToSound(audioMngr: AudioManager, actionData: VolumeActionData) {
        val notificationMngr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationMngr.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            changeRingerVolume(audioMngr, actionData)
        } else {
            changeRingerVolume(audioMngr, actionData)
        }
    }

    /**
     * Changes the cellPhones ringing mode to silent.
     */
    private fun silenceRinger(audioMngr: AudioManager) {
        val notificationMngr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&!notificationMngr.isNotificationPolicyAccessGranted) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            audioMngr.ringerMode = AudioManager.RINGER_MODE_SILENT
        } else {
            audioMngr.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
    }

    /**
     * Changes the cellPhones ringing sound to the target sound stored in actionData
     */
    private fun changeRingerVolume(audioMngr: AudioManager, actionData: VolumeActionData) {
        audioMngr.ringerMode = AudioManager.RINGER_MODE_NORMAL
        val targetVolume = actionData.volumeLevel.toInt() //todo to int or not to int? that is the float
        audioMngr.setStreamVolume(AudioManager.STREAM_RING, targetVolume, 0)
    }

    private fun createAndShowToast(action : Task.Action) { //todo - delete
        val actionData = gson.fromJson(action.extraData, ToastActionData::class.java)
        val text: CharSequence = actionData.text
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(context, text, duration)
        toast.show()
    }

    /**
     * Adds to a filter all the actions to whom the broadcastReceiver should listen, creates the
     * broadcastReceiver using the filter. Finally, register the broadcastReceiver with the filter.
     */
    private fun createAndRegisterBroadcastReceiver() {
        for(task in taskList){
            when(task.condition.conditionType){
                Task.ConditionEnum.WIFI -> actionsToListenTo.add(WIFI_CHANGED_BROADCAST)
                Task.ConditionEnum.BLUETOOTH -> actionsToListenTo.add(BLUETOOTH_CHANGED_BROADCAST)
                Task.ConditionEnum.LOCATION -> {} //todo
                Task.ConditionEnum.TIME -> {} // todo
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
    }
}