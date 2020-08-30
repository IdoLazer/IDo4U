package com.my.ido4u

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import java.util.*


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

        override fun onSwitchClicked(id: Int, isChecked : Boolean) {
            TaskManager.switchTask(id, isChecked)
        }
    })

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recycler : RecyclerView = findViewById(R.id.task_recycler)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)

       // val m = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager //todo
//        m.isDataEnabled = false //todo

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
        if(checkConditionsPermissions(Task.ConditionEnum.BLUETOOTH)){
            val conData = BluetoothConditionData("LE-Ido's Bose QC35 II", "4C:87:5D:CB:9B:CD")
            val conDataStr = gson.toJson(conData)
            var cond = Task.Condition(Task.ConditionEnum.BLUETOOTH, conDataStr, conData.toString())

            val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG,
                "found Ido's bluetooth!")
            val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData), actData.toString())

            var newTask = Task("find earphone", true, cond, arrayOf(action))

            TaskManager.addTask(newTask)
            adapter.notifyDataSetChanged()
            startService()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun mockWifi(){ //todo delete!

        if(!Settings.System.canWrite(applicationContext)){
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        }
        val conData : WifiConditionData = WifiConditionData("10:5a:f7:07:6f:88")//"10:be:f5:3c:48:e6")
        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.WIFI, gson.toJson(conData), conData.toString())
//        val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG,
//                                                                           "found Ido's wifi!")
//        val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData))

        /////////////////////////////////////// volume action //////////////////////////////////////
        val actData = VolumeActionData(VolumeActionData.VolumeAction.SOUND, 3.0f)
        val action : Task.Action = Task.Action(Task.ActionEnum.VOLUME, gson.toJson(actData), actData.toString())
        val newTask : Task = Task("wifi task1", true, cond, arrayOf(action))

        TaskManager.addTask(newTask)
        adapter.notifyDataSetChanged()
        startService()

        /////////////////////////////////// brightness action //////////////////////////////////////

                val actData2 = BrightnessActionData(170)
        val action2 : Task.Action = Task.Action(Task.ActionEnum.BRIGHTNESS, gson.toJson(actData2),actData2.toString())
        val newTask2 = Task("wifi task2", true, cond, arrayOf(action2))

        TaskManager.addTask(newTask2)
        adapter.notifyDataSetChanged()
        startService()

        /////////////////////////////////////// app action //////////////////////////////////////
        val actData3 = OpenAppActionData("com.waze")
        val action3 : Task.Action = Task.Action(Task.ActionEnum.APPS, gson.toJson(actData3), actData3.toString())
        val newTask3 : Task = Task("wifi task3", true, cond, arrayOf(action3))

        TaskManager.addTask(newTask3)
        adapter.notifyDataSetChanged()
        startService()

        Log.e("found_wifi_start", myNetwork.toString())
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
//    private fun mockWifi(){ //todo delete!
//        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        checkPermission()
//        handleWifi()
//    }
//
//    private fun handleWifi() {
//        wifiScanReceiver = object : BroadcastReceiver() {
//            @RequiresApi(api = Build.VERSION_CODES.M)
//            override fun onReceive(c: Context, intent: Intent) {
//                val success = intent.getBooleanExtra(
//                    WifiManager.EXTRA_RESULTS_UPDATED, false)
//                if (success) {
//                    scanSuccess()
//                } else {
//                    // scan failure handling
//                    scanFailure()
//                }
//            }
//        }
//        val intentFilter = IntentFilter()
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//        registerReceiver(wifiScanReceiver, intentFilter)
//        val success = wifiManager!!.startScan()
//        if (!success) {
//            // scan failure handling
//            scanFailure()
//        }
//    }
//
//    private fun scanSuccess() {
//        val results = wifiManager!!.scanResults
//        myNetwork = results[0]
//
//        val conData : WifiConditionData = WifiConditionData(results[0].BSSID)
//        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.WIFI, gson.toJson(conData))
//        val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG, "found Ido's wifi!")
//        val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData))
//        val newTask : Task = Task("wifi task", true, cond, action)
//
//
//        TaskManager.addTask(newTask)
////        TaskManager.addTask(Task("Stupid Task", false, cond, action))
////        TaskManager.addTask(Task("Shut up bitch Im tryin to talk", false, cond, action))
//        adapter.notifyDataSetChanged()
//
//        startService()
//
//        Log.e("found_wifi_start", myNetwork.toString())
//    }
//
//    private fun scanFailure() {
//        // handle failure: new scan did NOT succeed
//        // consider using old scan results: these are the OLD results!
//        val results = wifiManager!!.scanResults
//    }
//
//
//    fun startService() {
//        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
//        serviceIntent.putExtra("inputExtra", "listening")
//
//        startService(serviceIntent)
//    }
//
//    fun stopService() {
//        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
//        stopService(serviceIntent)
//    }
//
//
//    private fun checkPermission(): Boolean {
//        val permissionsList: MutableList<String> = ArrayList()
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
//            != PackageManager.PERMISSION_GRANTED) {
//            permissionsList.add(Manifest.permission.ACCESS_WIFI_STATE)
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)
//            != PackageManager.PERMISSION_GRANTED) {
//            permissionsList.add(Manifest.permission.CHANGE_WIFI_STATE)
//        }
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) {
//            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//        }
//        if (permissionsList.size > 0) {
//            ActivityCompat.requestPermissions(this,
//                permissionsList.toTypedArray(), 1)
//            return false
//        }
//        return true
//    }

    private fun checkConditionsPermissions(type : Task.ConditionEnum) : Boolean{
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

//    private fun checkActionsPermissions(type : Task.ActionEnum) : Boolean{
//        when(type){
//            Task.ActionEnum.VOLUME ->{
//
//            }
//        }
//        return true // todo
//    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
//            val componentName: ComponentName? = data.getComponent()
//            if(componentName != null) {
//                val packageName = componentName.packageName
//                val activityName = componentName.className
//            }
//        }
//    }
    override fun onDestroy() {
        super.onDestroy()
    }
}
