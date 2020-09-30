package com.my.ido4u

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


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

    fun addTask(task: Task) {
        taskList.add(task)
        refreshSharedPreferences()
    }

    fun getPosition(i: Int): Task {
        return taskList[i]
    }

    fun setPosition(i: Int, task: Task) {
        taskList[i] = task
        refreshSharedPreferences()
    }

    fun removeTask(i: Int) {
        taskList.removeAt(i)
        refreshSharedPreferences()
    }

    fun getSize(): Int {
        return taskList.size
    }

    fun getTaskList(): MutableList<Task> {
        return taskList.toMutableList()
    }

    fun switchTask(i: Int, isSwitched: Boolean) {
        taskList[i].isOn = isSwitched
        refreshSharedPreferences()
    }

    fun renameTask(i: Int, name: String) {
        taskList[i].name = name
        refreshSharedPreferences()
    }

    private fun refreshSharedPreferences() {
        sp.edit().putString(TASK_LIST, gson.toJson(taskList)).apply()
    }
}