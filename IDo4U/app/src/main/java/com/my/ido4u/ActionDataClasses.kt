package com.my.ido4u


import android.location.Location

/**
 * A class that contains all the data needed for an action that changes screen brightness
 */

data class BrightnessActionData (var brightness: Int){
    override fun toString(): String {
        return "Put brightness level at ${(brightness.toFloat() * (100.0/255.0)).toInt()}%"
    }
}

/**
 * A class that contains all the data needed for an action that opens another application
 */
data class OpenAppActionData(var packageName : String){
    override fun toString(): String {
        val appName = packageName.removePrefix("com.").substringBefore('.')
        return "Open app: $packageName"
    }
}

/**
 * A class that contains all the data needed for an action that changes device volume
 */
data class VolumeActionData(var volumeAction: VolumeAction, var volumeLevel: Float) {
    enum class VolumeAction { SOUND, MUTE, VIBRATE }

    override fun toString(): String {
        return when (volumeAction) {
            VolumeAction.SOUND -> "Put volume level at ${volumeLevel.toInt()}%"
            VolumeAction.MUTE -> "Put phone on mute"
            VolumeAction.VIBRATE -> "Put phone on vibrate"
        }
    }
}


data class ToastActionData(var toastAction: ToastAction, var text: String) { //todo - remove
    enum class ToastAction { LONG, SHORT }
}

