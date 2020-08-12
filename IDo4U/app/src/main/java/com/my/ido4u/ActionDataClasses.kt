package com.my.ido4u

data class VolumeActionData (var volumeAction: VolumeAction, var volumeLevel: Float){
    enum class VolumeAction {SOUND, MUTE, VIBRATE}
}

data class ToastActionData (var toastAction: ToastAction, var text: String){
    enum class ToastAction {LONG, SHORT}
}