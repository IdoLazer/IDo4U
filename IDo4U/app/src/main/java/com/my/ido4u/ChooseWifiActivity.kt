package com.my.ido4u

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * In this activity the user can choose a WiFi connection as a condition for a task.
 */
class ChooseWifiActivity : AppCompatActivity() {

    private lateinit var scanResultsLinearLayout: LinearLayout
    private lateinit var chooseWifiEditText: EditText
    private var progressBar: ProgressBar? = null
    private var scanResults: MutableList<WifiConditionData> = mutableListOf()
    private var gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_choose_wifi)

        initializeViews(savedInstanceState)
        createWifiChoiceTutorial()
    }

    /**
     * Creates a tutorial, if one should be shown
     */
    private fun createWifiChoiceTutorial() {
        val sp = Ido4uApp.applicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
        val showedTutorial = sp.getBoolean(SHOWED_WIFI_TUTORIAL, false)
        if(!showedTutorial) {
            val views = arrayOf<View>(
                findViewById(R.id.choose_wifi_Layout),
                findViewById(R.id.choose_wifi_edit_text),
                findViewById(R.id.scrollView3)
            )

            val texts = listOf<String>(
                getString(R.string.choose_wifi_tutorial),
                getString(R.string.type_wifi_tutorial),
                getString(R.string.choose_ready_wifi_tutorial)
            )

            createTutorial(this@ChooseWifiActivity, texts, SHOWED_WIFI_TUTORIAL, *views)
        }
    }


    /**
     * Initializes the views of the activity.
     */
    private fun initializeViews(savedInstanceState: Bundle?) {
        scanResultsLinearLayout = findViewById(R.id.choose_wifi_linearLayout)
        chooseWifiEditText = findViewById(R.id.choose_wifi_edit_text)
        val confirmWifiButton: MaterialButton = findViewById(R.id.confirm_wifi_network_button)
        confirmWifiButton.setOnClickListener {
            if (chooseWifiEditText.text.isNotEmpty()) {
                onNetworkChosen(WifiConditionData("", chooseWifiEditText.text.toString()))
            } else {
                AlertDialog.Builder(this)
                    .setTitle("No Wi-Fi Network Entered")
                    .setMessage("Please manually enter the name of the Wi-Fi network")
                    .setNeutralButton(
                        android.R.string.ok
                    ) { _, _ -> }
                    .show()
            }
        }

        if (savedInstanceState != null && savedInstanceState.containsKey(SCAN_RESULTS)) {
            val scanResultListJsonString = savedInstanceState.getString(SCAN_RESULTS, null)
            if (scanResultListJsonString != null) {
                scanResultsLinearLayout.removeAllViewsInLayout()
                val groupListType: Type =
                    object : TypeToken<MutableList<WifiConditionData>>() {}.type
                val restoredScanResults: MutableList<WifiConditionData> =
                    gson.fromJson(scanResultListJsonString, groupListType)
                for (result in restoredScanResults) {
                    addScanResultFromWifiData(result)
                }
                return
            }
        }

        initializeScan()
    }

    /**
     * Performs scanWifi - preempting actions and than calls scanWifi.
     */
    private fun initializeScan() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        progressBar = findViewById(R.id.progressBar_cyclic_wifi)
        progressBar!!.visibility = ProgressBar.VISIBLE
        scanWifi(this, wifiManager, ::populateList)
    }

    /**
     * todo
     */
    private fun addScanResultFromWifiData(result: WifiConditionData) {
        scanResults.add(result)
        val scanResultLayout =
            LayoutInflater.from(this)
                .inflate(R.layout.item_menu, scanResultsLinearLayout, false)
        scanResultLayout.findViewById<TextView>(R.id.menu_item_name).text = result.ssid
        scanResultLayout.findViewById<ImageView>(R.id.menu_item_icon)
            .setImageResource(R.drawable.ic_baseline_wifi_24)
        scanResultLayout.setOnClickListener { onNetworkChosen(result) }
        scanResultsLinearLayout.addView(scanResultLayout)
    }

    /**
     * todo
     */
    private fun populateList(list: List<ScanResult>) {
        progressBar!!.visibility = ProgressBar.INVISIBLE
        scanResultsLinearLayout.removeAllViewsInLayout()
        for (item in list) {
            val wifiData = WifiConditionData(item.BSSID, item.SSID)
            addScanResultFromWifiData(wifiData)
        }
    }

    /**
     * todo
     */
    private fun onNetworkChosen(wifiConditionData: WifiConditionData) {
        val resultIntent = Intent()
        val condition = Task.Condition(
            Task.ConditionEnum.WIFI,
            gson.toJson(wifiConditionData),
            wifiConditionData.toString()
        )
        resultIntent.putExtra(CONDITION, gson.toJson(condition))
        setResult(FragmentActivity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == WIFI_PERMISSION_REQUEST_CODE){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                initializeScan()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (scanResults.isNotEmpty()) {
            outState.putString(SCAN_RESULTS, gson.toJson(scanResults))
        }
    }
}