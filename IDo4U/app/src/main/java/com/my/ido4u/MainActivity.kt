package com.my.ido4u

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var myNetwork: ScanResult? = null
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var gson : Gson = Gson()
    private var adapter = TaskAdapter(object : TaskAdapter.TaskClickListener {
        override fun onTaskClicked(id: Int) {
            openTaskProfile(id)
        }
    })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val adapter = TaskAdapter(object : TaskAdapter.TaskClickListener {
//            override fun onTaskClicked(id: Int) {
//                openTaskProfile(id)
//            }
//        })
        val recycler : RecyclerView = findViewById(R.id.task_recycler)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)


        val addButton : FloatingActionButton = findViewById(R.id.add_task_button)
        addButton.setOnClickListener {
            var intent = Intent(this@MainActivity, TaskProfileActivity::class.java) //todo restore
            startActivity(intent) //todo restore
        }

        mockWifi()
    }

    private fun mockWifi(){ //todo delete!
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        checkPermission()
        handleWifi()
    }

    private fun handleWifi() {
        wifiScanReceiver = object : BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            override fun onReceive(c: Context, intent: Intent) {
                val success = intent.getBooleanExtra(
                    WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    scanSuccess()
                } else {
                    // scan failure handling
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
        val success = wifiManager!!.startScan()
        if (!success) {
            // scan failure handling
            scanFailure()
        }
    }

    private fun scanSuccess() {
        val results = wifiManager!!.scanResults
        myNetwork = results[0]

        val conData : WifiConditionData = WifiConditionData(results[0].BSSID)
        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.WIFI, gson.toJson(conData))
        val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG, "found Ido's wifi!")
        val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData))
        val newTask : Task = Task("wifi task", true, cond, action)


        TaskManager.addTask(newTask)
//        TaskManager.addTask(Task("Stupid Task", false, cond, action))
//        TaskManager.addTask(Task("Shut up bitch Im tryin to talk", false, cond, action))
        adapter.notifyDataSetChanged()

        startService()

        Log.e("found_wifi_start", myNetwork.toString())
    }

    private fun scanFailure() {
        // handle failure: new scan did NOT succeed
        // consider using old scan results: these are the OLD results!
        val results = wifiManager!!.scanResults
    }

    private fun openTaskProfile(id: Int) {
        var intent = Intent(this, TaskProfileActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    fun startService() {
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        serviceIntent.putExtra("inputExtra", "listening")

        startService(serviceIntent)
    }

    fun stopService() {
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        stopService(serviceIntent)
    }


    private fun checkPermission(): Boolean {
        val permissionsList: MutableList<String> = ArrayList()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_WIFI_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.CHANGE_WIFI_STATE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissionsList.size > 0) {
            ActivityCompat.requestPermissions(this,
                permissionsList.toTypedArray(), 1)
            return false
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver);
    }
}
