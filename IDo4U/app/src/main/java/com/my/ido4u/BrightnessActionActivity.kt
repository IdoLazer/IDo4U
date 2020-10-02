package com.my.ido4u

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import kotlin.math.roundToInt

/**
 * In this activity the user can choose the level of brightness they wish for.
 */
class BrightnessActionActivity : AppCompatActivity() {

    private lateinit var setBrightnessSlider: Slider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_brightness_action)
        initializeViews()
    }

    private fun initializeViews() {

        setBrightnessSlider = findViewById(R.id.choose_brightness_slider)

        val confirmBrightnessButton: MaterialButton =
            findViewById(R.id.confirm_brightness_action_button)
        confirmBrightnessButton.setOnClickListener {
            confirmBrightnessAction()
        }
    }

    private fun confirmBrightnessAction() {
        if (!checkActionsPermissions(Task.ActionEnum.BRIGHTNESS, this)) {
            showBrightnessPermissionsDialog(this)
        } else {
            val resultIntent = Intent()
            val brightnessActionData =
                BrightnessActionData(
                    (setBrightnessSlider.value * (255.0 / 100.0)).roundToInt(),
                    setBrightnessSlider.value.toInt()
                )
            val action = Task.Action(
                Task.ActionEnum.BRIGHTNESS,
                Gson().toJson(brightnessActionData),
                brightnessActionData.toString()
            )
            resultIntent.putExtra(ACTION, Gson().toJson(action))

            setResult(FragmentActivity.RESULT_OK, resultIntent)
            finish()
        }
    }
}