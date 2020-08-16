package com.my.ido4u

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothProfile.GATT
import android.bluetooth.BluetoothProfile.GATT_SERVER
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
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
     * Adds WifiManager.NETWORK_STATE_CHANGED_ACTION to the filter of the broadcastReceiver.
     * In addition, checks for every existing wifi conditioned task if the phone is connected
     * to its condition's wifi address and if so execute its action.
     */
    private fun initializeBluetoothTask(rawData: String, task: Task) {
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

    /**
     * Adds BluetoothDevice.ACTION_ACL_CONNECTED to the filter of the broadcastReceiver.
     * In addition, checks for every existing bluetooth conditioned task if the phone is connected
     * to its condition's bluetooth device and if so execute its action.
     */
    private fun initializeWifiTask(rawData: String, task: Task) {
        actionsToListenTo.add(WIFI_CHANGED_BROADCAST)
        val condData = gson.fromJson(rawData, WifiConditionData::class.java)
        val taskBssid = condData.bssid
        val wifiMngr = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val curBssid = wifiMngr.connectionInfo.bssid
        if (taskBssid == curBssid) {
            handleActions(task)
        }
    }

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
                    if (curBssid == taskBssid && curBssid != lastRSSID) {
                        lastRSSID = taskBssid
                        handleActions(task)
                    }
                }
                lastRSSID = curBssid
            }
        }
    }

    private fun handleActions(task: Task) {
        when (task.action.actionType) {
            Task.ActionEnum.TOAST -> createAndShowToast(task)  // todo - add more cases
        }
    }

    private fun createAndShowToast(task : Task ) {
        val c = applicationContext
        val actionData = gson.fromJson(task.action.extraData, ToastActionData::class.java)
        val text: CharSequence = actionData.text
        val duration = Toast.LENGTH_SHORT
        val toast = Toast.makeText(c, text, duration)
        toast.show()
    }

    inner class MyReceiver : BroadcastReceiver() {
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

    override fun onCreate() {
        createAndRegisterBroadcastReceiver() // todo - here or in the constructor?
    }

    /**
     * Adds to a filter all the actions to whom the broadcastReceiver should listen, creates the
     * broadcastReceiver using the filter. Finally, register the broadcastReceiver with the filter.
     */
    private fun createAndRegisterBroadcastReceiver() {
        val filter = IntentFilter()
        for (action in actionsToListenTo) {
            filter.addAction(action)
        }
        mReceiver = MyReceiver()
        registerReceiver(mReceiver, filter)
    }

    /**
     * These commands will be performed whenever StartService is called for this class
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        for(task in taskList){
            val rawData = task.condition.extraData
            when(task.condition.conditionType){
                Task.ConditionEnum.WIFI -> initializeWifiTask(rawData, task)
                Task.ConditionEnum.BLUETOOTH -> initializeBluetoothTask(rawData, task)

                //todo - fill in bluetooth's action, location, etc.
            }
        }

        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,intent1, 0)
        val notification = NotificationCompat.Builder(this, Ido4uApp.CHANNEL_ID)
            .setContentTitle("Ido4u")
            .setContentText("Waiting for a condition to fill...")
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(FOREGROUND_ID, notification)
        return START_STICKY
        //todo - should add stopService() at some point (maybe "stop listening" button)
    }

    override fun onBind(intent: Intent): IBinder? { //todo - needed?
        return null
    }
}