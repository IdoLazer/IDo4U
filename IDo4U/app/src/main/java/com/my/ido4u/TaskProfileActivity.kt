package com.my.ido4u

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity

class TaskProfileActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_profile)

        val editTaskTitle : EditText = findViewById(R.id.edit_task_title)

        val id = intent.getIntExtra("id", -1)
        if (id == -1) {

            return
        }
        var task = TaskManager.getPosition(id)
        editTaskTitle.setText(task.name)
    }

}
