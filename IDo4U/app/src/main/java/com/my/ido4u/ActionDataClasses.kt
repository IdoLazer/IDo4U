package com.my.ido4u

data class VolumeActionData(var volumeAction: VolumeAction, var volumeLevel: Float) {
    enum class VolumeAction { SOUND, MUTE, VIBRATE }

    override fun toString(): String {
        return when (volumeAction) {
            VolumeAction.SOUND -> "Put volume level at $volumeLevel"
            VolumeAction.MUTE -> "Put phone on mute"
            VolumeAction.VIBRATE -> "Put phone on vibrate"
        }
    }
}

data class ToastActionData(var toastAction: ToastAction, var text: String) {
    enum class ToastAction { LONG, SHORT }
}