package com.my.ido4u

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson


class CreateConditionActivity : AppCompatActivity() {

    private data class MenuItem(
        var item_name: String,
        var icon_src: Int,
        var onClickListener: View.OnClickListener
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_condition)
        createMainConditionMenu()
    }

    private fun createMainConditionMenu() {
        val conditionMenuLinearLayout: LinearLayout = findViewById(R.id.condition_menu_linearLayout)

        val menuItems = mutableListOf(
            MenuItem(
                "Date and Time",
                R.drawable.ic_baseline_access_time_24,
                View.OnClickListener { clickedOnDateAndTime() }
            ),
            MenuItem(
                "Location",
                R.drawable.ic_baseline_location_on_24,
                View.OnClickListener { clickedOnLocation() }
            ),
            MenuItem(
                "WiFi",
                R.drawable.ic_baseline_wifi_24,
                View.OnClickListener { clickedOnWifi() }
            ),
            MenuItem(
                "Bluetooth",
                R.drawable.ic_baseline_bluetooth_24,
                View.OnClickListener { clickedOnBluetooth() }
            )
        )

        for (item in menuItems) {
            val menuItemLayout =
                LayoutInflater.from(this)
                    .inflate(R.layout.item_menu, conditionMenuLinearLayout, false)
            menuItemLayout.findViewById<TextView>(R.id.menu_item_name).text = item.item_name
            menuItemLayout.findViewById<ImageView>(R.id.menu_item_icon)
                .setImageResource(item.icon_src)
            menuItemLayout.setOnClickListener(item.onClickListener)
            conditionMenuLinearLayout.addView(menuItemLayout)
        }
    }

    private fun clickedOnDateAndTime() {

    }

    private fun clickedOnLocation() {
        if (!checkConditionsPermissions(Task.ConditionEnum.LOCATION, this)) return

        val intent = Intent(this, ChooseLocationActivity::class.java)
        startActivityForResult(intent, CHOOSE_LOCATION_REQUEST_CODE)
    }

    private fun clickedOnWifi() {

    }

    private fun clickedOnBluetooth() {

    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHOOSE_LOCATION_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null
        ) {
            setResult(FragmentActivity.RESULT_OK, data)
            finish()
        }
    }
}