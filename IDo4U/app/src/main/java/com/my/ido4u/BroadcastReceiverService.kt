package com.my.ido4u

import android.app.PendingIntent
import android.app.Service
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

class BroadcastReceiverService : Service() {

    var mReceiver: BroadcastReceiver? = null
    private var actionsToListenTo: HashSet<String> = HashSet<String>()
    private var taskList: MutableList<Task> = TaskManager.getTaskList() //todo - deep copy?
    private var lastRSSID: String? = null

    init {
        for (task in taskList) {
            when (task.condition.conditionType) {
                Task.ConditionEnum.WIFI -> actionsToListenTo
                    .add(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                //todo - fill in bluetooth's action, location, etc.
            }
        }
    }

    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == null) {
                Log.e("error", "action is null!")
            } else {
                when (action) {
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> { // todo - add more cases
                        for (task in taskList) {
                            if (task.condition.conditionType == Task.ConditionEnum.WIFI) {
                                val wifiMngr: WifiManager = applicationContext.applicationContext
                                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                                val wifiInfo: WifiInfo = wifiMngr.connectionInfo
                                val bssid: String? = wifiInfo.bssid

                                if (bssid != "02:00:00:00:00:00" && bssid != null) {
                                    val myBssid: String = ""//task.wifiNetworkSSID ?: "" //todo
                                    if (myBssid == bssid && lastRSSID != bssid) {
                                        lastRSSID = bssid
                                        for (taskAction in task.actions) {
                                            when (taskAction.actionType) {
                                                Task.ActionEnum.TOAST -> { // todo - add more cases
                                                    Log.e("found something", "wifi changed!")
                                                    val c = applicationContext
                                                    val text: CharSequence = "wifi changed!"
                                                    val duration = Toast.LENGTH_SHORT
                                                    val toast = Toast.makeText(c, text, duration)
                                                    toast.show()
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    lastRSSID = "bssid"
                                }
                            }
                        }

                    }
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val input = intent.getStringExtra("inputExtra")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, 0
        )
        val notification = NotificationCompat.Builder(this, Ido4uApp.CHANNEL_ID)
            .setContentTitle(input)
            .setContentText(input)
            .setSmallIcon(R.drawable.ic_baseline_hearing_24)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
        return START_STICKY

        //stopService();
    }

    override fun onBind(intent: Intent): IBinder? { //todo - needed?
        return null
    }
}