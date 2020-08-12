package com.example.servicetest

import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class ExampleService : Service() {
    var mReceiver: BroadcastReceiver? = null

    // use this as an inner class like here or as a top-level class
    inner class MyReceiver  // constructor
        : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == null) {
                Log.e("error", "action is null!")
            } else {
                when (action) {
                    WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
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
    }

    override fun onCreate() {
        // get an instance of the receiver in your service
        val filter = IntentFilter()
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        //        filter.addAction("anotherAction");
        mReceiver = MyReceiver()
        registerReceiver(mReceiver, filter)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val input = intent.getStringExtra("inputExtra")
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)
        val notification = NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setContentTitle("Example Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_android)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(1, notification)
        return START_STICKY

        //stopService();
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}