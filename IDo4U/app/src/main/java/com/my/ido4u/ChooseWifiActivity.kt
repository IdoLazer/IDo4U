package com.my.ido4u

import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
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

class ChooseWifiActivity : AppCompatActivity() {

    private lateinit var scanResultsLinearLayout: LinearLayout
    private lateinit var chooseWifiEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_wifi)

        initializeViews()
    }

    private fun initializeViews() {
        scanResultsLinearLayout = findViewById(R.id.choose_wifi_linearLayout)
        chooseWifiEditText = findViewById(R.id.choose_wifi_edit_text)
        val confirmWifiButton: MaterialButton = findViewById(R.id.confirm_wifi_network_button)
        confirmWifiButton.setOnClickListener {
            if (chooseWifiEditText.text.isNotEmpty()) {
                onNetworkChosen("", chooseWifiEditText.text.toString())
            }
        }
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        scanWifi(this, wifiManager, ::populateList)
    }

    private fun populateList(list: List<ScanResult>) {
        scanResultsLinearLayout.removeAllViewsInLayout()
        for (item in list) {
            val scanResultLayout =
                LayoutInflater.from(this)
                    .inflate(R.layout.item_menu, scanResultsLinearLayout, false)
            scanResultLayout.findViewById<TextView>(R.id.menu_item_name).text = item.SSID
            scanResultLayout.findViewById<ImageView>(R.id.menu_item_icon)
                .setImageResource(R.drawable.ic_baseline_wifi_24)
            scanResultLayout.setOnClickListener { onNetworkChosen(item.BSSID, item.SSID) }
            scanResultsLinearLayout.addView(scanResultLayout)
        }
    }

    private fun onNetworkChosen(bssid: String, ssid: String) {
        val resultIntent = Intent()
        val wifiConditionData =
            WifiConditionData(bssid, ssid)
        val condition = Task.Condition(
            Task.ConditionEnum.WIFI,
            Gson().toJson(wifiConditionData),
            wifiConditionData.toString()
        )
        resultIntent.putExtra(CONDITION, Gson().toJson(condition))
        setResult(FragmentActivity.RESULT_OK, resultIntent)
        finish()
    }
}


