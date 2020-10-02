package com.my.ido4u

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson

/**
 * In this activity the user can choose what kind of action they want to add to their task
 */
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

        val actionMenuLinearLayout: LinearLayout = findViewById(R.id.action_menu_linearLayout)
        actionMenuLinearLayout.removeAllViewsInLayout()

        val menuItems = mutableListOf(
            MenuItem(
                "Change Volume",
                R.drawable.ic_baseline_volume_mute_24,
                View.OnClickListener { clickedOnVolume() }
            ),
            MenuItem(
                "Change Brightness",
                R.drawable.ic_baseline_brightness_6_24,
                View.OnClickListener { clickedOnBrightness() }
            ),
            MenuItem(
                "Open App",
                R.drawable.ic_baseline_apps_24,
                View.OnClickListener { clickedOnApps() }
            )
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

    private fun clickedOnBrightness() {
        val intent = Intent(this, BrightnessActionActivity::class.java)
        startActivityForResult(intent, CHOOSE_BRIGHTNESS_ACTION_REQUEST_CODE)
    }

    private fun clickedOnVolume() {
        val intent = Intent(this@CreateActionActivity, VolumeActionActivity::class.java)
        startActivityForResult(intent, CHOOSE_VOLUME_ACTION_REQUEST_CODE)
    }

    private fun clickedOnApps() {
        progressBar!!.visibility = ProgressBar.VISIBLE
        chooseApp(this)
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
            return Task.Action(
                Task.ActionEnum.APPS,
                Gson().toJson(openAppActionData),
                openAppActionData.toString()
            )
        }
        return null
    }
}