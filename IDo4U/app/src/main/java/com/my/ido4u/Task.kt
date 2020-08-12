package com.my.ido4u

import com.google.gson.Gson

class Task (var name : String, var isOn : Boolean, var condition : Condition, var action : Action) {

    val gson : Gson = Gson()
    var wifiNetworkSSID : String? = null
    var actions: List<Action> = listOf()

    init{
        val extraData = condition.extraData
        when(condition.conditionType){
            ConditionEnum.WIFI -> {
                val extraData = gson.fromJson(extraData, WifiConditionData::class.java)
                wifiNetworkSSID = extraData.bssid
            }
        }
    }

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