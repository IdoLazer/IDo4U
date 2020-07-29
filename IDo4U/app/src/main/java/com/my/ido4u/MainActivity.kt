package com.my.ido4u

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val adapter = TaskAdapter()
        val recycler : RecyclerView = findViewById(R.id.task_recycler)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val addButton : FloatingActionButton = findViewById(R.id.add_task_button)
        TaskManager.addTask(Task("Stupid Task", true))
        TaskManager.addTask(Task("Shut up bitch Im tryin to talk", false))
        adapter.notifyDataSetChanged()
    }
}
