package com.my.ido4u

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson


/**
 * Activity in which thw user can choose a bluetooth device as a condition for a task.
 */
class ChooseBluetoothActivity : AppCompatActivity() {

    private lateinit var scanResultsLinearLayout: LinearLayout
    private lateinit var chooseBluetoothEditText: EditText
    private lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_bluetooth)

        initializeViews()
        createAndRegisterBroadcastReceiver()
    }

    /**
     * Creates and registers a broadcastReceiver that listens to changes in bluetooth state.
     */
    private fun createAndRegisterBroadcastReceiver() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent!!.action

                if (action == BLUETOOTH_STATE_CHANGED) {
                    val bluetoothState = intent.getIntExtra(
                        BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR
                    )
                    when (bluetoothState) {
                        BluetoothAdapter.STATE_ON -> {
                            populateList(getBluetoothDevices())
                        }
                    }
                }
            }
        }
        val filter = IntentFilter()
        filter.addAction(BLUETOOTH_STATE_CHANGED)
        registerReceiver(broadcastReceiver, filter)
    }

    /**
     * Initializes all the views of the activity.
     */
    private fun initializeViews() {
        scanResultsLinearLayout = findViewById(R.id.choose_bluetooth_linearLayout)
        chooseBluetoothEditText = findViewById(R.id.choose_bluetooth_edit_text)
        val confirmWifiButton: MaterialButton = findViewById(R.id.confirm_bluetooth_network_button)
        confirmWifiButton.setOnClickListener {
            if (chooseBluetoothEditText.text.isNotEmpty()) {
                onDeviceChosen(chooseBluetoothEditText.text.toString(), "")
            } else {
                AlertDialog.Builder(this)
                    .setTitle("No Device Name Entered")
                    .setMessage("Please manually enter the name of the bluetooth device")
                    .setNeutralButton(
                        android.R.string.ok
                    ) { _, _ -> }
                    .show()
            }
        }
        if (!BluetoothAdapter.getDefaultAdapter().isEnabled) {
            AlertDialog.Builder(this)
                .setTitle("Bluetooth is Off")
                .setMessage("Please enable bluetooth if you wish to see a list of paired devices")
                .setNeutralButton(
                    android.R.string.ok
                ) { _, _ -> }
                .show()
        }
        populateList(getBluetoothDevices())
    }

    /**
     * todo
     */
    private fun populateList(set: Set<BluetoothDevice>?) {
        if (set == null || set.isEmpty()) return

        scanResultsLinearLayout.removeAllViewsInLayout()
        for (item in set) {
            val scanResultLayout =
                LayoutInflater.from(this)
                    .inflate(R.layout.item_menu, scanResultsLinearLayout, false)
            scanResultLayout.findViewById<TextView>(R.id.menu_item_name).text = item.name
            scanResultLayout.findViewById<ImageView>(R.id.menu_item_icon)
                .setImageResource(R.drawable.ic_baseline_bluetooth_24)
            scanResultLayout.setOnClickListener { onDeviceChosen(item.name, item.address) }
            scanResultsLinearLayout.addView(scanResultLayout)
        }
    }

    /**
     * todo
     */
    private fun onDeviceChosen(name: String, address: String) {
        val resultIntent = Intent()
        val bluetoothConditionData =
            BluetoothConditionData(name, address)
        val condition = Task.Condition(
            Task.ConditionEnum.BLUETOOTH,
            Gson().toJson(bluetoothConditionData),
            bluetoothConditionData.toString()
        )
        resultIntent.putExtra(CONDITION, Gson().toJson(condition))
        setResult(FragmentActivity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(broadcastReceiver)
    }
}