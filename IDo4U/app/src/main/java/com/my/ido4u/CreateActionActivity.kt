package com.my.ido4u

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity

class CreateActionActivity : AppCompatActivity() {
    private data class MenuItem(
        var item_name: String,
        var icon_src: Int,
        var onClickListener: View.OnClickListener
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_action)
        createMainConditionMenu()
    }

    private fun createMainConditionMenu() {
        val conditionMenuLinearLayout: LinearLayout = findViewById(R.id.action_menu_linearLayout)

        val menuItems = mutableListOf(
            MenuItem(
                "Phone Settings",
                R.drawable.ic_baseline_settings_24,
                View.OnClickListener { clickedOnPhoneSettings() }
            ),
            MenuItem(
                "Apps",
                R.drawable.ic_baseline_apps_24,
                View.OnClickListener { clickedOnApps() }
            ),
            MenuItem(
                "Communication",
                R.drawable.ic_baseline_message_24,
                View.OnClickListener { clickedOnCommunication() }
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

    private fun clickedOnPhoneSettings() {

    }

    private fun clickedOnApps() {
        chooseApp(this)
    }

    private fun clickedOnCommunication() {
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode in ACTION_REQUEST_CODES &&
            resultCode == Activity.RESULT_OK && data != null
        ) {
            val componentName: ComponentName? = data.component
                        if(componentName != null) {
                            val packageName = componentName.packageName
                            val activityName = componentName.className
                            Log.d("tag", packageName)
                            Log.d("tag", activityName)
                        }
//            setResult(FragmentActivity.RESULT_OK, data)
//            finish()
        }
    }
}