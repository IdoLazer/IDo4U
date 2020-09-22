package com.my.ido4u

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

class CreateConditionActivity : AppCompatActivity() {

    data class MenuItem(
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
        var conditionMenuLinearLayout: LinearLayout = findViewById(R.id.condition_menu_linearLayout)

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
                LayoutInflater.from(this).inflate(R.layout.item_menu, conditionMenuLinearLayout, false)
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
        
    }

    private fun clickedOnWifi() {

    }

    private fun clickedOnBluetooth() {

    }
}