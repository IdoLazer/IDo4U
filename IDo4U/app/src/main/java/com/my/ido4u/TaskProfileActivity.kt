package com.my.ido4u

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

class TaskProfileActivity : FragmentActivity() {

    val MAX_ACTIONS = 10

    var condition: Task.Condition? = null
    var actions: MutableList<Task.Action> = mutableListOf()
    var gson = Gson()
    var id = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_profile)

        val editTaskTitle: EditText = findViewById(R.id.edit_task_title)
        val addConditionButton: MaterialButton = findViewById(R.id.add_condition_button)
        val addActionButton: MaterialButton = findViewById(R.id.add_action_button)
        val applyNewTaskButton: MaterialButton = findViewById(R.id.apply_new_task_button)
        val conditionScrollViewLL: LinearLayout =
            findViewById(R.id.condition_scrollView_linearLayout)
        val actionsScrollViewLL: LinearLayout = findViewById(R.id.actions_scrollView_linearLayout)

        id = intent.getIntExtra("id", -1)

        addConditionButton.setOnClickListener {
            if (conditionScrollViewLL.childCount == 1) {
                conditionScrollViewLL.removeAllViewsInLayout()
            } else {
                addConditionButton.text = "Edit Condition"
            }
            val wifiCondition = WifiConditionData("fAk3_n3Tw0Rk")
            var newCondition = Task.Condition(
                Task.ConditionEnum.WIFI,
                gson.toJson(wifiCondition),
                wifiCondition.toString()
            )
            createCondition(newCondition, conditionScrollViewLL)
        }

        addActionButton.setOnClickListener {
            if (actionsScrollViewLL.childCount < MAX_ACTIONS) {
                val volumeAction = VolumeActionData(VolumeActionData.VolumeAction.SOUND, 0.5f)
                val action = Task.Action(
                    Task.ActionEnum.VOLUME,
                    gson.toJson(volumeAction),
                    volumeAction.toString()
                )
                createAction(action, actionsScrollViewLL)
            }
        }

        applyNewTaskButton.setOnClickListener {
            if (condition == null || actions.size == 0) {
            } else {
                val task =
                    Task(editTaskTitle.text.toString(), true, condition!!, actions.toTypedArray())
                if (id == -1) {
                    TaskManager.addTask(task)
                } else {
                    TaskManager.setPosition(id, task)
                }
                var intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
        }

        if (id == -1) {
            return
        }

        addConditionButton.text = "Edit Condition"
        var task = TaskManager.getPosition(id)
        editTaskTitle.setText(task.name)
        createCondition(task.condition, conditionScrollViewLL)
        for (action in task.actions) {
            createAction(action, actionsScrollViewLL)
        }
    }

    private fun createAction(action: Task.Action, actionsScrollViewLL: LinearLayout) {
        actions.add(
            action
        )
        val tv = TextView(this)
        tv.text = action.description
        tv.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        actionsScrollViewLL.addView(tv)
    }

    private fun createCondition(newCondition: Task.Condition, conditionScrollViewLL: LinearLayout) {
        condition = newCondition
        val tv = TextView(this)
        tv.text = newCondition.description
        conditionScrollViewLL.addView(tv)
    }
}
