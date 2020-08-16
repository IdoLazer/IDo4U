package com.my.ido4u

import com.google.gson.Gson

class Task (var name : String, var isOn : Boolean, var condition : Condition, var action : Action) {


    var actions: List<Action> = listOf()

    enum class ConditionEnum {
        WIFI, LOCATION, BLUETOOTH, TIME
    }

    enum class ActionEnum {
        VOLUME, BRIGHTNESS, DATA, APPS, COMMUNICATION,
        TOAST // todo delete
    }

    data class Condition(var conditionType: ConditionEnum, var extraData: String) {}

    data class Action(var actionType: ActionEnum, var extraData: String) {}

}