package com.my.ido4u

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

/**
 * This class manages all the tasks
 */
object TaskManager {

    private var taskList = ArrayList<Task>()
    private val gson = Gson()
    private var sp = Ido4uApp.applicationContext()
        .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)

    init {
        val taskListJsonString = sp.getString(TASK_LIST, null)
        if (taskListJsonString != null) {
            val groupListType: Type = object : TypeToken<ArrayList<Task>>() {}.type
            taskList = gson.fromJson(taskListJsonString, groupListType)
        }
    }

    /**
     * Add a new task
     */
    fun addTask(task: Task) {
        taskList.add(task)
        refreshSharedPreferences()
    }

    /**
     * Get the task in a certain position
     */
    fun getPosition(i: Int): Task {
        return taskList[i]
    }

    /**
     * Add a task to a certain position
     */
    fun setPosition(i: Int, task: Task) {
        if (i >= taskList.size || i < 0) return

        taskList[i] = task
        refreshSharedPreferences()
    }

    /**
     * remove a task in a certain position
     */
    fun removeTask(i: Int) {
        taskList.removeAt(i)
        refreshSharedPreferences()
    }

    /**
     * get the size of the task list
     */
    fun getSize(): Int {
        return taskList.size
    }

    /**
     * get the entire task list
     */
    fun getTaskList(): MutableList<Task> {
        return taskList.toMutableList()
    }

    /**
     * Turn a task on or off
     */
    fun switchTask(i: Int, isSwitched: Boolean) {
        taskList[i].isOn = isSwitched
        refreshSharedPreferences()
    }

    private fun refreshSharedPreferences() {
        sp.edit().putString(TASK_LIST, gson.toJson(taskList)).apply()
    }
}