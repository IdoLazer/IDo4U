package com.my.ido4u

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * The adapter for the task list in the main activity
 */
class TaskAdapter(private var taskClickListener: TaskClickListener) :
    RecyclerView.Adapter<TaskHolder>() {

    interface TaskClickListener {
        fun onTaskClicked(id: Int)
        fun onSwitchClicked(id: Int, isChecked: Boolean)
    }

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
        holder.itemView.setOnClickListener { taskClickListener.onTaskClicked(position) }
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            taskClickListener.onSwitchClicked(position, isChecked)
        }
    }
}