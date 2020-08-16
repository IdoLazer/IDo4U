package com.my.ido4u

import android.Manifest
import android.bluetooth.BluetoothAdapter
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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.util.ArrayList

const val WIFI_PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var myNetwork: ScanResult? = null
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var gson: Gson = Gson()
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
            var intent = Intent(this@MainActivity, TaskProfileActivity::class.java)
            startActivity(intent)
        }

        mockWifi()
        mockBluetooth()
    }

    private fun openTaskProfile(id: Int) {
        var intent = Intent(this, TaskProfileActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    ////////////////////////////////////// todo change /////////////////////////////////////////////
    private fun mockBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(checkPermissions(Task.ConditionEnum.BLUETOOTH)){
            val conData = BluetoothConditionData("LE-Ido's Bose QC35 II", "4C:87:5D:CB:9B:CD")
            val conDataStr = gson.toJson(conData)
            var cond = Task.Condition(Task.ConditionEnum.BLUETOOTH, conDataStr)

            val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG,
                "found Ido's bluetooth!")
            val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData))

            var newTask = Task("find earphone", true, cond, action)

            TaskManager.addTask(newTask)
            adapter.notifyDataSetChanged()
            startService()
        }
    }

    private fun mockWifi(){ //todo delete!
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        checkPermissions(Task.ConditionEnum.WIFI)
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
                    scanFailure()
                }
            }
        }
        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
        val success = wifiManager!!.startScan()
        if (!success) {
            scanFailure()
        }
    }

    private fun scanSuccess() {
        val results = wifiManager!!.scanResults
        myNetwork = results[0]

        val conData : WifiConditionData = WifiConditionData(results[0].BSSID)
        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.WIFI, gson.toJson(conData))
        val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG,
                                                                        "found Ido's wifi!")
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
        Log.e("scan failure", "failed to scan wifi")
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

    //////////////////////////// permission related code ///////////////////////////////////////////
    private fun checkPermission(permission : String) : Boolean{
        val granted = PackageManager.PERMISSION_GRANTED
        return ContextCompat.checkSelfPermission(this, permission)!= granted
    }

    private fun checkSpecificPermissions(permissionsList: MutableList<String>): Boolean {
        val unGrantedPermissionsList: MutableList<String> = ArrayList()
        for(permission in permissionsList){
            if (checkPermission(permission)) {
                unGrantedPermissionsList.add(permission)
            }
        }
        if (unGrantedPermissionsList.size > 0) {
            ActivityCompat.requestPermissions(this,
                unGrantedPermissionsList.toTypedArray(), 1) // todo - request code
            return false
        }
        return true
    }

    private fun checkPermissions(type : Task.ConditionEnum) : Boolean{
        when(type){
            Task.ConditionEnum.WIFI -> checkSpecificPermissions(mutableListOf(
                                            Manifest.permission.ACCESS_WIFI_STATE,
                                            Manifest.permission.CHANGE_WIFI_STATE,
                                            Manifest.permission.ACCESS_COARSE_LOCATION))

            Task.ConditionEnum.BLUETOOTH -> checkSpecificPermissions(mutableListOf(
                                            Manifest.permission.ACCESS_COARSE_LOCATION))

            Task.ConditionEnum.TIME -> {} //todo

            Task.ConditionEnum.LOCATION -> {}//todo
        }
        return true // todo
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver);
    }
}
