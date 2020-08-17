package com.my.ido4u

data class VolumeActionData (var volumeAction: VolumeAction, var volumeLevel: Int){
    enum class VolumeAction {SOUND, MUTE, VIBRATE}
}

data class ToastActionData (var toastAction: ToastAction, var text: String){
    enum class ToastAction {LONG, SHORT}
}

data class BrightnessActionData (var brightness: Int){}