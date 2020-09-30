package com.my.ido4u

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson

const val MAX_ACTIONS = 10

class TaskProfileActivity : FragmentActivity() {

    var id = -1
    private var condition: Task.Condition? = null
    private var actions: MutableList<Task.Action> = mutableListOf()
    private var gson = Gson()


    private lateinit var conditionScrollViewLL: LinearLayout
    private lateinit var actionsScrollViewLL: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_profile)
        initializeViews()
    }

    private fun initializeViews() {
        conditionScrollViewLL = findViewById(R.id.condition_scrollView_linearLayout)
        actionsScrollViewLL = findViewById(R.id.actions_scrollView_linearLayout)

        val editTaskTitle: EditText = findViewById(R.id.edit_task_title)
        val addConditionButton: MaterialButton = findViewById(R.id.add_condition_button)
        val addActionButton: MaterialButton = findViewById(R.id.add_action_button)
        val applyNewTaskButton: MaterialButton = findViewById(R.id.apply_new_task_button)
        val deleteTaskButton: MaterialButton = findViewById(R.id.delete_task_button)
        val removeConditionButton: MaterialButton = findViewById(R.id.remove_condition_button)
        val removeActionButton: MaterialButton = findViewById(R.id.remove_action_button)

        id = intent.getIntExtra("id", -1)

        addConditionButton.setOnClickListener {
            if (condition == null) {
                conditionScrollViewLL.removeAllViewsInLayout()
            }
            addCondition()
            addConditionButton.text = getString(R.string.edit_condition)
        }

        removeConditionButton.setOnClickListener {
            condition = null
            addConditionButton.text = getString(R.string.add_condition)
            conditionScrollViewLL.removeAllViewsInLayout()
            val tv = TextView(this)
            tv.hint = getString(R.string.add_a_condition)
            conditionScrollViewLL.addView(tv)
        }

        addActionButton.setOnClickListener {
            if (actions.size == 0) {
                actionsScrollViewLL.removeAllViewsInLayout()
            }
            if (actionsScrollViewLL.childCount < MAX_ACTIONS) {
                addAction()
            }
        }

        removeActionButton.setOnClickListener {
            actions = mutableListOf()
            actionsScrollViewLL.removeAllViewsInLayout()
            val tv = TextView(this)
            tv.hint = "Add an action"
            actionsScrollViewLL.addView(tv)
        }

        applyNewTaskButton.setOnClickListener {
            if (condition == null || actions.size == 0 || editTaskTitle.text.isEmpty())
                return@setOnClickListener

            val task =
                Task(editTaskTitle.text.toString(), true, condition!!, actions.toTypedArray())
            if (id == -1) {
                TaskManager.addTask(task)
            } else {
                TaskManager.setPosition(id, task)
            }
            startService(this) // todo: uncomment when solved
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        deleteTaskButton.setOnClickListener {
            if (id != -1) {
                TaskManager.removeTask(id)
            }
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        if (id != -1) {
            populateFieldsForExistingTask(
                conditionScrollViewLL,
                actionsScrollViewLL,
                addConditionButton,
                editTaskTitle
            )
        }
    }

    private fun populateFieldsForExistingTask(
        conditionScrollViewLL: LinearLayout,
        actionsScrollViewLL: LinearLayout,
        addConditionButton: MaterialButton,
        editTaskTitle: EditText
    ) {
        conditionScrollViewLL.removeAllViewsInLayout()
        actionsScrollViewLL.removeAllViewsInLayout()
        addConditionButton.text = getString(R.string.edit_condition)
        val task = TaskManager.getPosition(id)
        editTaskTitle.setText(task.name)
        createCondition(task.condition)
        for (action in task.actions) {
            createAction(action)
        }
    }

    private fun addCondition() {
        val intent = Intent(this, CreateConditionActivity::class.java)
        startActivityForResult(intent, CHOOSE_CONDITION_REQUEST_CODE)
    }

    private fun addAction() {
        val intent = Intent(this, CreateActionActivity::class.java)
        startActivityForResult(intent, CHOOSE_ACTION_REQUEST_CODE)
    }

    private fun createAction(action: Task.Action) {
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

    private fun createCondition(newCondition: Task.Condition) {
        condition = newCondition
        val tv = TextView(this)
        tv.text = newCondition.description
        conditionScrollViewLL.removeAllViewsInLayout()
        conditionScrollViewLL.addView(tv)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHOOSE_CONDITION_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null
        ) {
            createCondition(
                gson.fromJson(
                    data.getStringExtra(CONDITION),
                    Task.Condition::class.java
                )
            )
        }
        if (requestCode == CHOOSE_ACTION_REQUEST_CODE &&
            resultCode == Activity.RESULT_OK && data != null) {
            createAction(gson.fromJson(data.getStringExtra(ACTION), Task.Action::class.java))
        }
    }
}
