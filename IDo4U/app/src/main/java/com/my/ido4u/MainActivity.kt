package com.my.ido4u

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color.argb
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.takusemba.spotlight.OnSpotlightListener
import com.takusemba.spotlight.Spotlight
import com.takusemba.spotlight.Target
import com.takusemba.spotlight.effet.RippleEffect
import com.takusemba.spotlight.shape.Circle

class MainActivity : AppCompatActivity() {

    private var wifiManager: WifiManager? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var wifiScanReceiver: BroadcastReceiver? = null
    private var gson: Gson = Gson()
    private var adapter = TaskAdapter(object : TaskAdapter.TaskClickListener {

        override fun onTaskClicked(id: Int) {
            openTaskProfile(id)
        }

        override fun onSwitchClicked(id: Int, isChecked: Boolean) {
            TaskManager.switchTask(id, isChecked)
        }
    })

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
//        wifiScanReceiver = scanWifi(this@MainActivity, wifiManager) //todo -remove
        createMockTasks() //todo - remove
        createTutorial(this@MainActivity, R.id.add_task_button)
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
            val intent = Intent(this@MainActivity, TaskProfileActivity::class.java) //todo
            startActivity(intent) // todo
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) { //todo - remove
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 0){
                if (data != null) {
                    Log.e("location selected: ", "${data.getParcelableExtra<LatLng>(MARKER_LAT_LNG)}")
                    Log.e("radius", "${data.getFloatExtra(RADIUS, 0f)}")
                }
            }
        }
    }

    /**
     * Opens an activity which presents the profile of the task with the id given
     */
    private fun openTaskProfile(id: Int) {
        val intent = Intent(this, TaskProfileActivity::class.java)
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

    /**
     * Starts the service that continuously checks for conditions.
     */
    private fun startService() {
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        serviceIntent.putExtra("inputExtra", "listening")
        startService(serviceIntent)
    }

    //////////////////////////// permission related code ///////////////////////////////////////////
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            WIFI_PERMISSION_REQUEST_CODE -> scanWifi(this@MainActivity, wifiManager) //todo
        }
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
        mockWifi()
        mockBluetooth()
        mockLocation()
    }

    private fun mockBluetooth(){
        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        val pairedDevices: Set<BluetoothDevice>? = getPairedBluetoothDevices()
        pairedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            Log.e("paired bluetooth", "$deviceName is paired with MAC address $deviceHardwareAddress")
        }

        if(checkConditionsPermissions(Task.ConditionEnum.BLUETOOTH, this)){
            val conData = BluetoothConditionData("LE-Ido's Bose QC35 II", "4C:87:5D:CB:9B:CD")
            val conDataStr = gson.toJson(conData)
            var cond = Task.Condition(Task.ConditionEnum.BLUETOOTH, conDataStr, conData.toString())

            val actData : ToastActionData = ToastActionData(
                ToastActionData.ToastAction.LONG,
                "found Ido's bluetooth!"
            )
            val action : Task.Action = Task.Action(
                Task.ActionEnum.TOAST,
                gson.toJson(actData),
                actData.toString()
            )

            var newTask = Task("find earphone", true, cond, arrayOf(action))

            addNewTask(newTask)
        }
    }

    private fun mockLocation(){
        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this)
        val condData = LocationConditionData(35.192712, 31.7770856, 50f)
        val cond : Task.Condition = Task.Condition(
            Task.ConditionEnum.LOCATION,
            gson.toJson(condData),
            condData.toString()
        )
        val actData = OpenAppActionData("com.waze")
        val action : Task.Action = Task.Action(
            Task.ActionEnum.APPS,
            gson.toJson(actData),
            actData.toString()
        )
        val newTask : Task = Task("location task", true, cond, arrayOf(action))
        addNewTask(newTask)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun mockWifi(){ //todo delete!

        if (!Settings.System.canWrite(applicationContext)) startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        val conData : WifiConditionData = WifiConditionData("Ido")//"10:be:f5:3c:48:e6")
        val cond : Task.Condition = Task.Condition(
            Task.ConditionEnum.WIFI,
            gson.toJson(conData),
            conData.toString()
        )

        /////////////////////////////////////// volume action //////////////////////////////////////
        val actData = VolumeActionData(VolumeActionData.VolumeAction.SOUND, 3.0f)
        val action : Task.Action = Task.Action(
            Task.ActionEnum.VOLUME,
            gson.toJson(actData),
            actData.toString()
        )
        val newTask : Task = Task("wifi task1", true, cond, arrayOf(action))
        addNewTask(newTask)

        /////////////////////////////////// brightness action //////////////////////////////////////
        askBrightnessPermission(this)
        val actData2 = BrightnessActionData(170)
        val action2 : Task.Action = Task.Action(
            Task.ActionEnum.BRIGHTNESS,
            gson.toJson(actData2),
            actData2.toString()
        )
        val newTask2 = Task("wifi task2", true, cond, arrayOf(action2))
        addNewTask(newTask2)
    }
}
