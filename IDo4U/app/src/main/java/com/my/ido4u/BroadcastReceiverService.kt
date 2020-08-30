package com.my.ido4u

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
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Color.BLUE
import android.media.AudioManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.gson.Gson


const val DEFAULT_BSSID = "02:00:00:00:00:00"
const val FOREGROUND_ID = 1
const val WIFI_CHANGED_BROADCAST = WifiManager.NETWORK_STATE_CHANGED_ACTION
const val BLUETOOTH_CHANGED_BROADCAST = BluetoothDevice.ACTION_ACL_CONNECTED

class BroadcastReceiverService : Service() {

    private var mReceiver: BroadcastReceiver? = null
    private var actionsToListenTo : HashSet<String> = HashSet<String>()
    private var taskList : MutableList<Task> = TaskManager.getTaskList() //todo - deep copy?
    private var lastRSSID : String? = null
    private val gson : Gson = Gson()
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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
                val wifiMngr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
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
        if (Settings.System.canWrite(applicationContext)) {
            Settings.System.putInt(
                applicationContext.contentResolver,
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
        val c = applicationContext
        val actionData = gson.fromJson(action.extraData, ToastActionData::class.java)
        val text: CharSequence = actionData.text
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(c, text, duration)
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

    /**
     * These commands will be performed whenever StartService is called for this class
     */
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createAndRegisterBroadcastReceiver() // todo - here or in the constructor?
        for(task in taskList){
            val rawData = task.condition.extraData
            when(task.condition.conditionType){
                Task.ConditionEnum.BLUETOOTH -> initializeBluetoothTask(rawData, task) //todo - doesn't work!
                //todo - fill in bluetooth's action, location, etc.
            }
            for (action in task.actions) {
                if (action.actionType == Task.ActionEnum.BRIGHTNESS) {
                    if (!Settings.System.canWrite(applicationContext)) {
                        startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
                    }
                }
            }
        }
        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,intent1, 0)
        val notification = NotificationCompat.Builder(this, Ido4uApp.CHANNEL_ID)
            .setContentTitle("Ido4u")
            .setContentText("Waiting for a condition to be filled...")
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(118,0,255))
            .setOngoing(true)
            .build()
        startForeground(FOREGROUND_ID, notification)
        return START_STICKY
        //todo - should add stopService() at some point (maybe "stop listening" button)
    }

    override fun onBind(intent: Intent): IBinder? { //todo - needed?
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mReceiver)
    }
}