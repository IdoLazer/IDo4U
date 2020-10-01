package com.my.ido4u

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.gson.Gson

const val CHECKED_BOX = "checked box"

class VolumeActionActivity : AppCompatActivity() {

    private lateinit var setVolumeLevelCheckBox: CheckBox
    private lateinit var putPhoneOnMuteCheckBox: CheckBox
    private lateinit var putPhoneOnVibrateCheckBox: CheckBox
    private lateinit var setVolumeSlider: Slider
    private lateinit var setVolumeLinearLayout: LinearLayout

    private var volumeActionType: VolumeActionData.VolumeAction =
        VolumeActionData.VolumeAction.SOUND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_volume_action)
        initializeViews(savedInstanceState)
    }

    private fun initializeViews(savedInstanceState: Bundle?) {
        setVolumeLevelCheckBox = findViewById(R.id.set_custom_volume_level_checkbox)
        putPhoneOnMuteCheckBox = findViewById(R.id.mute_phone_checkbox)
        putPhoneOnVibrateCheckBox = findViewById(R.id.put_phone_on_vibrate_checkbox)
        setVolumeSlider = findViewById(R.id.choose_volume_slider)
        setVolumeLinearLayout = findViewById(R.id.set_custom_volume_level_linearLayout)
        setVolumeLinearLayout.removeViewAt(1)

        putPhoneOnMuteCheckBox.setOnClickListener { putOnMute() }
        putPhoneOnVibrateCheckBox.setOnClickListener { putOnVibrate() }
        setVolumeLevelCheckBox.setOnClickListener { setVolumeLevel() }
        val confirmVolumeButton: MaterialButton = findViewById(R.id.confirm_volume_action_button)
        confirmVolumeButton.setOnClickListener {
            if (!checkActionsPermissions(Task.ActionEnum.VOLUME, this)) {
                showVolumePermissionsDialog(this)
            } else {
                confirmVolumeAction()
            }
        }

        if (savedInstanceState != null) {
            volumeActionType = Gson().fromJson(
                savedInstanceState.getString(CHECKED_BOX),
                VolumeActionData.VolumeAction::class.java
            )
            when(volumeActionType) {
                VolumeActionData.VolumeAction.SOUND -> setVolumeLevel()
                VolumeActionData.VolumeAction.MUTE -> putOnMute()
                VolumeActionData.VolumeAction.VIBRATE -> putOnVibrate()
            }
        }
        else {
            setVolumeLevel()
        }
    }

    private fun setVolumeLevel() {
        volumeActionType = VolumeActionData.VolumeAction.SOUND
        putPhoneOnMuteCheckBox.isChecked = false
        setVolumeLevelCheckBox.isChecked = true
        putPhoneOnVibrateCheckBox.isChecked = false
        if (setVolumeLinearLayout.childCount <= 1) {
            setVolumeLinearLayout.addView(setVolumeSlider)
        }
    }

    private fun putOnVibrate() {
        volumeActionType = VolumeActionData.VolumeAction.VIBRATE
        putPhoneOnMuteCheckBox.isChecked = false
        setVolumeLevelCheckBox.isChecked = false
        putPhoneOnVibrateCheckBox.isChecked = true
        if (setVolumeLinearLayout.childCount > 1) {
            setVolumeLinearLayout.removeViewAt(1)
        }
    }

    private fun putOnMute() {
        volumeActionType = VolumeActionData.VolumeAction.MUTE
        putPhoneOnMuteCheckBox.isChecked = true
        setVolumeLevelCheckBox.isChecked = false
        putPhoneOnVibrateCheckBox.isChecked = false
        if (setVolumeLinearLayout.childCount > 1) {
            setVolumeLinearLayout.removeViewAt(1)
        }
    }

    private fun confirmVolumeAction() {
        val resultIntent = Intent()
        val volumeActionData =
            VolumeActionData(volumeActionType, setVolumeSlider.value)
        val action = Task.Action(
            Task.ActionEnum.VOLUME,
            Gson().toJson(volumeActionData),
            volumeActionData.toString()
        )
        resultIntent.putExtra(ACTION, Gson().toJson(action))
        setResult(FragmentActivity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(CHECKED_BOX, Gson().toJson(volumeActionType))
    }
}