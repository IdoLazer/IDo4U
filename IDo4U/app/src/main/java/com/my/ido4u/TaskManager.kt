package com.my.ido4u

object TaskManager {

    private var taskList = mutableListOf<Task>()

    fun addTask(task : Task) {
        taskList.add(task)
    }

    fun getPosition(i : Int) : Task{
        return taskList[i]
    }

    fun removeTask(i : Int) {
        taskList.removeAt(i)
    }

    fun getSize() : Int {
        return taskList.size
    }

    fun getTaskList() : MutableList<Task>{
        return taskList
    }
}