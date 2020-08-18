package com.my.ido4u

class Task(
    var name: String,
    var isOn: Boolean,
    var condition: Condition,
    var actions: Array<Action>
) {

    enum class ConditionEnum {
        WIFI, LOCATION, BLUETOOTH, TIME
    }

    enum class ActionEnum {
        VOLUME, BRIGHTNESS, DATA, APPS, COMMUNICATION,
        TOAST // todo delete
    }

    data class Condition(
        var conditionType: ConditionEnum,
        var extraData: String,
        var description: String
    ) {}

    data class Action(
        var actionType: ActionEnum,
        var extraData: String,
        var description: String
    ) {}

}