package com.my.ido4u

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter : RecyclerView.Adapter<TaskHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskHolder {
        val context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_one_task, parent, false)
        return TaskHolder(view)
    }

    override fun getItemCount(): Int {
        return TaskManager.getSize()
    }

    override fun onBindViewHolder(holder: TaskHolder, position: Int) {
        val task = TaskManager.getPosition(position)
        holder.text.text = task.name
        holder.switch.isChecked = task.isOn
    }
}