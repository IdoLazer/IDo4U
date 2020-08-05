package com.my.ido4u

class Task (var name : String, var isOn : Boolean) {

    enum class ConditionEnum {
        WIFI, LOCATION, BLUETOOTH, TIME
    }

    enum class ActionEnum {
        VOLUME, BRIGHTNESS, DATA, APPS, COMMUNICATION
    }

    data class Condition(var condition: ConditionEnum, var extraData: String) {}
    data class Action(var action: ActionEnum, var extraData: String) {}

    var condition: Condition? = null
    var actions: List<Action> = listOf()
}