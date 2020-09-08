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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.gson.Gson

class BroadcastReceiverService : Service() {
    private var mReceiver: BroadcastReceiver? = null
    private var actionsToListenTo : HashSet<String> = HashSet<String>()
    private var taskList : MutableList<Task> = TaskManager.getTaskList() //todo - deep copy?
    private var lastRSSID : String? = null
    private val gson : Gson = Gson()
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // todo - needed?
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
        createAndRegisterBroadcastReceiver() // todo - here or in the constructor?
        for(task in taskList){
            val rawData = task.condition.extraData
            when(task.condition.conditionType){
                Task.ConditionEnum.BLUETOOTH -> initializeBluetoothTask(rawData, task) //todo - doesn't work!
                Task.ConditionEnum.LOCATION -> initializeLocationTracking()

                //todo - fill in bluetooth's action, location, etc.
            }
            askBrightnessPermission(task) //todo - delete and check permissions in task creation
        }
        startForeground(FOREGROUND_ID,  createStickyNotification())
        return START_STICKY
        //todo - should add stopService() at some point (maybe "stop listening" button)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * todo
     */
    private fun askBrightnessPermission(task: Task) { //todo - delete and check permissions in task creation
        for (action in task.actions) {
            if (action.actionType == Task.ActionEnum.BRIGHTNESS) {
                if (!Settings.System.canWrite(context)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                }
            }
        }
    }

    /**
     * Creates the sticky notification which lets the user know the service is running in foreground
     */
    private fun createStickyNotification(): Notification? {
        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0)
        val notification = NotificationCompat.Builder(this, Ido4uApp.CHANNEL_ID)
            .setContentTitle("Ido4u")
            .setContentText("Waiting for a condition to be filled...")
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(118, 0, 255))
            .setOngoing(true)
            .build()
        return notification
    }

    //////////////////////////////// location tracking methods /////////////////////////////////////

    @SuppressLint("MissingPermission") // todo - can't happen, leave this notation?
    /**
     * todo
     */
    private fun initializeLocationTracking() {
        if (!locationTrackingStarted) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (hasLocationPermissions()) {
                if (isLocationEnabled()) {
                    fusedLocationClient.lastLocation.addOnCompleteListener {
                        requestNewLocationData()
                    }
                }
                else{
                    noLocationDialog(context)
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
    @SuppressLint("MissingPermission") // todo - can't happen, leave this notation?
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.interval = 5
        locationRequest.fastestInterval = 5 // the fastest update interval allowed
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onLocationResult(locationResult: LocationResult) {
                onLocationChanged(locationResult.lastLocation)
            }
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
                                                                                Looper.myLooper())
    }

    @RequiresApi(Build.VERSION_CODES.M)
    /**
     * Goes through all the tasks in taskList and for each task:
     * - Checks whether it's location-conditioned
     * - If so, checks if the distance between  current location and the one in the condition is
     *   smaller or equal to the condition's radius.
     * - If so, it executes the task's actions.
     */
    //todo - should add some lastLocation mechanism in order to save battery
    private fun onLocationChanged(curLocation: Location){
        if(curLocation.accuracy < MINIMAL_ACCURACY) { // todo - needed? If so, is 50m a good limitation?
            if(lastLocation == null){
                lastLocation = curLocation
                activateLoactionConditionedTasks(curLocation)
            }
            if(curLocation.distanceTo(lastLocation) > MINIMAL_DISTANCE_TO_LAST_LOCATION) {
                activateLoactionConditionedTasks(curLocation)
            }
            else{
                Log.e("too close", "curLocation is too close to to the last one")
            }
            lastLocation = curLocation
        }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun activateLoactionConditionedTasks(curLocation: Location) {
        Log.e("action activated", "action activated due to location change")
        val curLon = curLocation.longitude
        val curLat = curLocation.latitude
        for (task in taskList) {
            if (task.condition.conditionType == Task.ConditionEnum.LOCATION) {
                val rawData = task.condition.extraData
                val data = gson.fromJson(rawData, LocationConditionData::class.java)
                val results = FloatArray(1)
                Location.distanceBetween(curLat, curLon, data.latitude, data.longitude, results)
                Log.e("distance", "${results[0]}") // todo - remove
                if (results[0] <= data.radius) {
                    handleActions(task)
                }
            }
        }
    }

    /**
     * Checks whether or not the location services are enabled.
     * @return true if yes, false otherwise.
     */
    private fun isLocationEnabled(): Boolean {
        val lm = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var gps_enabled = false
        var network_enabled = false
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
        }
        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (ex: Exception) {
        }
        return gps_enabled && network_enabled
    }

    /**
     * Returns true if the app has the necessary permissions for location tracking, false otherwise
     */
    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
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
    private fun initializeBluetoothTask(rawData: String, task: Task) { //todo = not working!
        actionsToListenTo.add(BLUETOOTH_CHANGED_BROADCAST)
        val condData = gson.fromJson(rawData, BluetoothConditionData::class.java)
        val deviceAddress = condData.hardwareAddress
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

//        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
//        var connectionState = bluetoothManager.getConnectionState(device, GATT)
//        if(connectionState == BluetoothProfile.STATE_CONNECTED){
//            handleActions(task)
//        }
        val connected: List<BluetoothDevice> = bluetoothManager.getConnectedDevices(GATT) //todo - why is it empty?!
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
                val wifiMngr = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val wifiInfo: WifiInfo = wifiMngr.connectionInfo
                val rawExtraData = task.condition.extraData
                val extraData = gson.fromJson(rawExtraData, WifiConditionData::class.java)
                val taskBssid: String? = extraData.bssid
                val curBssid = wifiInfo.bssid

                if (taskBssid != DEFAULT_BSSID && taskBssid != null) { //todo - only null?
                    if (curBssid == taskBssid ){//&& curBssid != lastRSSID) {
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
        if(mReceiver != null){
            unregisterReceiver(mReceiver)
        }
        else {
            mReceiver = MyReceiver()
        }
        registerReceiver(mReceiver, filter)
    }



    override fun onBind(intent: Intent): IBinder? { //todo - needed?
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }
}