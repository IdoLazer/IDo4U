package com.my.ido4u

import android.bluetooth.BluetoothDevice
import android.content.Intent
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

class ChooseBluetoothActivity : AppCompatActivity() {

    lateinit var scanResultsLinearLayout: LinearLayout
    lateinit var chooseBluetoothEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_bluetooth)
        initializeViews()
    }

    private fun initializeViews() {
        scanResultsLinearLayout = findViewById(R.id.choose_bluetooth_linearLayout)
        chooseBluetoothEditText = findViewById(R.id.choose_bluetooth_edit_text)
        val confirmWifiButton: MaterialButton = findViewById(R.id.confirm_bluetooth_network_button)
        confirmWifiButton.setOnClickListener {
            if (chooseBluetoothEditText.text.isNotEmpty()) {
                onDeviceChosen(chooseBluetoothEditText.text.toString(), "")
            }
        }
        populateList(getBluetoothDevices())
    }

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
}