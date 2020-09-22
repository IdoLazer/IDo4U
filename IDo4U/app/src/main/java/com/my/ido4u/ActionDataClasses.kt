package com.my.ido4u

import android.location.Location

/**
 * A class that contains all the data needed for an action that changes screen brightness
 */
data class BrightnessActionData (var brightness: Int){} //todo toString with %

/**
 * A class that contains all the data needed for an action that opens another application
 */
data class OpenAppActionData(var packageName : String){} //todo toString with app name

/**
 * A class that contains all the data needed for an action that changes device volume
 */
data class VolumeActionData(var volumeAction: VolumeAction, var volumeLevel: Float) {
    enum class VolumeAction { SOUND, MUTE, VIBRATE }  //todo toString with %

    override fun toString(): String {
        return when (volumeAction) {
            VolumeAction.SOUND -> "Put volume level at $volumeLevel"
            VolumeAction.MUTE -> "Put phone on mute"
            VolumeAction.VIBRATE -> "Put phone on vibrate"
        }
    }
}


data class ToastActionData(var toastAction: ToastAction, var text: String) { //todo - remove
    enum class ToastAction { LONG, SHORT }
}

