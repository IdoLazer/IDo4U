package com.my.ido4u

import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val text: TextView = itemView.findViewById(R.id.task_name)
    val switch: Switch = itemView.findViewById(R.id.task_switch)
}