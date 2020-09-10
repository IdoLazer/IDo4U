package com.my.ido4u

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson


const val WIFI_PERMISSION_REQUEST_CODE = 0
const val  BLUETOOTH_PERMISSIONS_REQUEST_CODE = 1
const val LOCATION_PERMISSION_REQUEST_CODE = 3

class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var myNetwork: ScanResult? = null
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var gson: Gson = Gson()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
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

        initializeViews()
//        noLocationDialog(this)
        TaskManager.emptySP() //todo - remove
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        createMockTasks() //todo - remove
    }


//    fun setMobileDataState(mobileDataEnabled: Boolean) { //todo - No Carrier Privilege
//        try {
//            val telephonyService = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//            val setMobileDataEnabledMethod = telephonyService.javaClass.getDeclaredMethod(
//                    "setDataEnabled",
//                    Boolean::class.javaPrimitiveType
//                )
//            setMobileDataEnabledMethod.invoke(telephonyService, mobileDataEnabled)
//        } catch (ex: Exception) {
//            Log.e("FragmentActivity.TAG", ex.cause.toString(), ex)
//        }
//    }


    /**
     * Initializes the MainActivities' views
     */
    private fun initializeViews() {
        val recycler: RecyclerView = findViewById(R.id.task_recycler)
        recycler.adapter = adapter
        recycler.layoutManager =
                        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val addButton: FloatingActionButton = findViewById(R.id.add_task_button)
        addButton.setOnClickListener {
            var intent = Intent(this@MainActivity, TaskProfileActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * todo
     */
    private fun openTaskProfile(id: Int) {
        var intent = Intent(this, TaskProfileActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    /**
     * Adds task to the task list of TaskManager and starts the service again
     */
    private fun addNewTask(newTask: Task) {
        TaskManager.addTask(newTask)
        adapter.notifyDataSetChanged()
        startService()
    }



    ////////////////////////////// Service - related code //////////////////////////////////////////
    private fun startService() {
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        serviceIntent.putExtra("inputExtra", "listening")
        startService(serviceIntent)
    }

    fun stopService() { //todo - add a "stop" button
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        stopService(serviceIntent)
    }

    ////////////////////////////////// Wifi Scan methods ///////////////////////////////////////////
    private fun scanWifi(){ //todo move to the relevant Activity when it will be created
        if (checkConditionsPermissions(Task.ConditionEnum.WIFI, this)){
            wifiScanReceiver = object : BroadcastReceiver() {

                @RequiresApi(api = Build.VERSION_CODES.M)
                override fun onReceive(c: Context, intent: Intent) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED,
                                                        false)
                    if (success) scanSuccess() else scanFailure()
                }
            }
            val intentFilter = IntentFilter()
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            registerReceiver(wifiScanReceiver, intentFilter)
            val success = wifiManager!!.startScan()
            if (!success) scanFailure()
        }
    }

    /**
     * Handles wifi scan success
     */
    private fun scanSuccess() { //todo - present results to user (Lazer)
        val results = wifiManager!!.scanResults
        Log.e("found_wifi_start", results.toString())
    }

    /**
     * Handles failure: new scan did NOT succeed
     */
    private fun scanFailure() {
        //todo consider using old scan results: these are the OLD results:
        // val results = wifiManager!!.scanResults
        Log.e("found_wifi_start", "wifi scan problem!")
    }

    //////////////////////////// permission related code ///////////////////////////////////////////
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            WIFI_PERMISSION_REQUEST_CODE ->{scanWifi()} //todo
        }
    }

    private fun checkActionsPermissions(type : Task.ActionEnum) : Boolean{ //todo - needed?
        when(type){
            Task.ActionEnum.VOLUME ->{}
        }
        return true // todo
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////

    override fun onDestroy() { //todo make sure all relevant broadcastReceivers are unregistered here
        super.onDestroy()
        if(wifiScanReceiver != null){
            unregisterReceiver(wifiScanReceiver)
        }
    }

    ////////////////////////////////////// todo change /////////////////////////////////////////////
    @RequiresApi(Build.VERSION_CODES.M)
    private fun createMockTasks() { // todo - remove
//        mockWifi()
        mockBluetooth()
//        mockLocation()
    }

    private fun mockBluetooth(){
        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(checkConditionsPermissions(Task.ConditionEnum.BLUETOOTH, this)){
            val conData = BluetoothConditionData("LE-Ido's Bose QC35 II", "4C:87:5D:CB:9B:CD")
            val conDataStr = gson.toJson(conData)
            var cond = Task.Condition(Task.ConditionEnum.BLUETOOTH, conDataStr, conData.toString())

            val actData : ToastActionData = ToastActionData(ToastActionData.ToastAction.LONG,
                "found Ido's bluetooth!")
            val action : Task.Action = Task.Action(Task.ActionEnum.TOAST, gson.toJson(actData), actData.toString())

            var newTask = Task("find earphone", true, cond, arrayOf(action))

            addNewTask(newTask)
        }
    }

    private fun mockLocation(){
        val condData = LocationConditionData(35.192712,31.7770856,  50f)
        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.LOCATION, gson.toJson(condData), condData.toString())
        val actData = OpenAppActionData("com.waze")
        val action : Task.Action = Task.Action(Task.ActionEnum.APPS, gson.toJson(actData), actData.toString())
        val newTask : Task = Task("location task", true, cond, arrayOf(action))
        addNewTask(newTask)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun mockWifi(){ //todo delete!

        if (!Settings.System.canWrite(applicationContext)) startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        val conData : WifiConditionData = WifiConditionData("10:be:f5:3c:48:e6") //"10:5a:f7:07:6f:88")
        val cond : Task.Condition = Task.Condition(Task.ConditionEnum.WIFI, gson.toJson(conData), conData.toString())

        /////////////////////////////////////// volume action //////////////////////////////////////
        val actData = VolumeActionData(VolumeActionData.VolumeAction.SOUND, 3.0f)
        val action : Task.Action = Task.Action(Task.ActionEnum.VOLUME, gson.toJson(actData), actData.toString())
        val newTask : Task = Task("wifi task1", true, cond, arrayOf(action))
        addNewTask(newTask)

        /////////////////////////////////// brightness action //////////////////////////////////////
        val actData2 = BrightnessActionData(170)
        val action2 : Task.Action = Task.Action(Task.ActionEnum.BRIGHTNESS, gson.toJson(actData2),actData2.toString())
        val newTask2 = Task("wifi task2", true, cond, arrayOf(action2))
        addNewTask(newTask2)
    }
}
