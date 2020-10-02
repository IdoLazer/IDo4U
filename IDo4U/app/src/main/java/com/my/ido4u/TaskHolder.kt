package com.my.ido4u

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * This class holds the views of one task item in the task list
 */
class TaskHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text: TextView = itemView.findViewById(R.id.task_name)
    val switch: SwitchMaterial = itemView.findViewById(R.id.task_switch)
}