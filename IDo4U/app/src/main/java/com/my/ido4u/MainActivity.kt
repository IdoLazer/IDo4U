package com.my.ido4u

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var gson: Gson = Gson()
    private var recycler: RecyclerView? = null
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

        if(TaskManager.getSize() != 0){ //todo - is it too many activations?
            startService(this)
        }
        initializeViews()
        val sp = Ido4uApp.applicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        if(!sp.getBoolean(SHOWED_MAIN_ACTIVITY_TUTORIAL, false)) {
            createMainActivityTutorial()
            sp.edit().putBoolean(SHOWED_MAIN_ACTIVITY_TUTORIAL, true).apply()
        }
    }

    /**
     * Creates tutorial for MainActivity.
     */
    private fun createMainActivityTutorial() {
        recycler!!.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val viewItem: View? = recycler!!.layoutManager?.findViewByPosition(0)
                    val firstTaskLayout = viewItem?.findViewById<View>(R.id.task_layout)
                    val firstTaskSwitch = viewItem?.findViewById<View>(R.id.task_switch)
                    val texts: List<String> = listOf(
                        getString(R.string.add_task_explanation),
                        getString(R.string.task_recycler_explanation),
                        getString(R.string.task_switch_explanation)
                    )
                    val arr = if (firstTaskLayout != null && firstTaskSwitch != null) {
                        arrayOf(
                            findViewById(R.id.add_task_button),
                            firstTaskLayout,
                            firstTaskSwitch
                        )
                    } else {
                        arrayOf(
                            findViewById(R.id.add_task_button),
                            findViewById(R.id.task_recycler)
                        )
                    }

                    createTutorial(this@MainActivity, texts, *arr)
                    recycler!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
    }

    /**
     * Initializes the MainActivities' views.
     */
    private fun initializeViews() {
        recycler = findViewById(R.id.task_recycler)
        recycler!!.adapter = adapter
        recycler!!.layoutManager =
                        LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val addButton: FloatingActionButton = findViewById(R.id.add_task_button)

        addButton.setOnClickListener {
            val intent = Intent(this@MainActivity, TaskProfileActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Opens an activity which presents the profile of the task with the id given.
     */
    private fun openTaskProfile(id: Int) {
        val intent = Intent(this, TaskProfileActivity::class.java)
        intent.putExtra("id", id)
        startActivity(intent)
    }

    /**
     * Adds task to the task list of TaskManager and starts the service again.
     */
    private fun addNewTask(newTask: Task) {
        TaskManager.addTask(newTask)
        adapter.notifyDataSetChanged()
        startService()
    }

    /**
     * Starts the service that continuously listens to relevant broadcasts and checks if any
     * task condition became fulfilled.
     */
    private fun startService() {
        val serviceIntent = Intent(this, BroadcastReceiverService::class.java)
        startService(serviceIntent)
    }

    //////////////////////////// permission related code ///////////////////////////////////////////
    override fun onRequestPermissionsResult( // todo - needed?
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            //todo - add cases!
        }
    }

    /////////////////////////// override of activity methods s///////////////////////////////////////

    override fun onDestroy() { //todo make sure all relevant broadcastReceivers are unregistered here
        super.onDestroy()
    }

    ////////////////////////////////////// todo change /////////////////////////////////////////////

    @RequiresApi(Build.VERSION_CODES.M)
    private fun createMockTasks() { // todo - remove
//        mockWifi()
//        mockBluetooth()
//        mockLocation()
    }

    private fun mockBluetooth(){
        checkConditionsPermissions(Task.ConditionEnum.LOCATION, this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

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
        val conData : WifiConditionData = WifiConditionData("", "Ido")//"10:be:f5:3c:48:e6") //todo
        val cond : Task.Condition = Task.Condition(
            Task.ConditionEnum.WIFI,
            gson.toJson(conData),
            conData.toString()
        )

        /////////////////////////////////////// volume action //////////////////////////////////////
        val actData = VolumeActionData(VolumeActionData.VolumeAction.SOUND, 30f)
        val action : Task.Action = Task.Action(
            Task.ActionEnum.VOLUME,
            gson.toJson(actData),
            actData.toString()
        )
        val newTask : Task = Task("wifi task1", true, cond, arrayOf(action))
        addNewTask(newTask)

        /////////////////////////////////// brightness action //////////////////////////////////////
        checkActionsPermissions(Task.ActionEnum.BRIGHTNESS,this)
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
