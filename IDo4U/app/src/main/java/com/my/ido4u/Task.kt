package com.my.ido4u

/**
 * A class that represents a task with a condition a list of action to be performed if and when
 * the condition is fulfilled
 */
class Task(
    var name: String,
    var isOn: Boolean,
    var condition: Condition,
    var actions: Array<Action>
) {

    /**
     * The possible types of conditions
     */
    enum class ConditionEnum {
        WIFI, LOCATION, BLUETOOTH, //TIME //todo:remove
    }

    /**
     * The possible types of actions
     */
    enum class ActionEnum {
        VOLUME, BRIGHTNESS, DATA, APPS, COMMUNICATION,
        TOAST // todo delete
    }

    /**
     * A class that represents a condition of a task
     */
    data class Condition(
        var conditionType: ConditionEnum,
        var extraData: String,
        var description: String
    )

    /**
     * A class that represents an action of a task
     */
    data class Action(
        var actionType: ActionEnum,
        var extraData: String,
        var description: String
    )

}