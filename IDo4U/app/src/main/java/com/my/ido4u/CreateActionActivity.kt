package com.my.ido4u

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class CreateActionActivity : AppCompatActivity() {

    private var progressBar: ProgressBar? = null
    private data class MenuItem(
        var item_name: String,
        var icon_src: Int,
        var onClickListener: View.OnClickListener
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_action)
        createMainConditionMenu()
        progressBar = findViewById(R.id.progressBar_cyclic)

    }

    override fun onResume() {
        super.onResume()

        progressBar!!.visibility = ProgressBar.INVISIBLE
    }

    private fun createMainConditionMenu() {
        val backButton: MaterialButton = findViewById(R.id.actions_menu_back_button)
        backButton.visibility = View.INVISIBLE
        val actionMenuLinearLayout: LinearLayout = findViewById(R.id.action_menu_linearLayout)
        actionMenuLinearLayout.removeAllViewsInLayout()

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
            )
//            ,MenuItem(
//                "Communication",
//                R.drawable.ic_baseline_message_24,
//                View.OnClickListener { clickedOnCommunication() }
//            ) //todo: add Communication action
        )

        for (item in menuItems) {
            val menuItemLayout =
                LayoutInflater.from(this)
                    .inflate(R.layout.item_menu, actionMenuLinearLayout, false)
            menuItemLayout.findViewById<TextView>(R.id.menu_item_name).text = item.item_name
            menuItemLayout.findViewById<ImageView>(R.id.menu_item_icon)
                .setImageResource(item.icon_src)
            menuItemLayout.setOnClickListener(item.onClickListener)
            actionMenuLinearLayout.addView(menuItemLayout)
        }
    }

    private fun clickedOnPhoneSettings() {
        createPhoneSettingsMenu()
    }

    private fun createPhoneSettingsMenu() {
        val backButton: MaterialButton = findViewById(R.id.actions_menu_back_button)
        backButton.visibility = View.VISIBLE
        backButton.setOnClickListener {
            createMainConditionMenu()
        }
        val phoneSettingsActionMenuLinearLayout: LinearLayout =
            findViewById(R.id.action_menu_linearLayout)
        phoneSettingsActionMenuLinearLayout.removeAllViewsInLayout()

        val menuItems = mutableListOf(
            MenuItem(
                "Volume",
                R.drawable.ic_baseline_volume_mute_24,
                View.OnClickListener { clickedOnVolume() }
            ),
            MenuItem(
                "Brightness",
                R.drawable.ic_baseline_brightness_6_24,
                View.OnClickListener { clickedOnBrightness() }
            )
        )

        for (item in menuItems) {
            val menuItemLayout =
                LayoutInflater.from(this)
                    .inflate(R.layout.item_menu, phoneSettingsActionMenuLinearLayout, false)
            menuItemLayout.findViewById<TextView>(R.id.menu_item_name).text = item.item_name
            menuItemLayout.findViewById<ImageView>(R.id.menu_item_icon)
                .setImageResource(item.icon_src)
            menuItemLayout.setOnClickListener(item.onClickListener)
            phoneSettingsActionMenuLinearLayout.addView(menuItemLayout)
        }
    }

    private fun clickedOnBrightness() {
        if (!checkActionsPermissions(Task.ActionEnum.BRIGHTNESS, this)) return

        val intent = Intent(this, BrightnessActionActivity::class.java)
        startActivityForResult(intent, CHOOSE_BRIGHTNESS_ACTION_REQUEST_CODE)
    }

    private fun clickedOnVolume() {
        if (!checkActionsPermissions(Task.ActionEnum.VOLUME, this)) return

        val intent = Intent(this, VolumeActionActivity::class.java)
        startActivityForResult(intent, CHOOSE_VOLUME_ACTION_REQUEST_CODE)
    }

    private fun clickedOnApps() {
        progressBar!!.visibility = ProgressBar.VISIBLE
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
            if (requestCode == CHOOSE_APP_ACTION_REQUEST_CODE) {
                progressBar!!.visibility = ProgressBar.INVISIBLE
                data.putExtra(ACTION, Gson().toJson(createOpenAppAction(data)))
            }
            setResult(FragmentActivity.RESULT_OK, data)
            finish()
        }
    }

    private fun createOpenAppAction(data: Intent): Task.Action? {
        val componentName: ComponentName? = data.component
        if (componentName != null) {
            val openAppActionData = OpenAppActionData(componentName.packageName)
            val action = Task.Action(
                Task.ActionEnum.APPS,
                Gson().toJson(openAppActionData),
                openAppActionData.toString()
            )
            return action
        }
        return null
    }
}