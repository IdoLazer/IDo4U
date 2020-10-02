package com.my.ido4u

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

    @RequiresApi(Build.VERSION_CODES.M) //todo
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(TaskManager.getSize() != 0 && savedInstanceState == null){
            startService(this)
        }
        initializeViews()
        performFirsLaunchOperations()
    }

    /**
     * Creates a mock-task if needed and starts a tutorial if needed.
     */
    private fun performFirsLaunchOperations() {
        val sp = Ido4uApp.applicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        if (!sp.getBoolean(SHOWED_MAIN_ACTIVITY_TUTORIAL, false)) {
            if (TaskManager.getSize() == 0) {
                mockBluetooth()
            }
            createMainActivityTutorial()
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

                    createTutorial(
                        this@MainActivity, texts,SHOWED_MAIN_ACTIVITY_TUTORIAL, *arr
                    )
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

    /**
     * Creates a bluetooth - conditioned task for tutorial - purposes
     */
    private fun mockBluetooth(){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val conData = BluetoothConditionData(
                "LE-Ido's Bose QC35 II", "4C:87:5D:CB:9B:CD" //todo
            )
            val conDataStr = gson.toJson(conData)
            val cond = Task.Condition(Task.ConditionEnum.BLUETOOTH, conDataStr, conData.toString())
            val actData = OpenAppActionData("com.waze")
            val action : Task.Action = Task.Action(
                Task.ActionEnum.APPS,
                gson.toJson(actData),
                actData.toString()
            )
            val newTask = Task("find earphone", true, cond, arrayOf(action))
            addNewTask(newTask)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }
}
