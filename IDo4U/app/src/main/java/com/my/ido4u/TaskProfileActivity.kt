package com.my.ido4u

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

const val MAX_ACTIONS = 10
const val CONDITION_EXIST = "condition exist"
const val RESTORE_CONDITION = "restore condition"
const val ACTIONS_EXIST = "actions exist"
const val RESTORE_ACTIONS = "restore actions"

class TaskProfileActivity : FragmentActivity() {

    var id = -1
    private var condition: Task.Condition? = null
    private var actions: MutableList<Task.Action> = mutableListOf()
    private var gson = Gson()


    private lateinit var conditionScrollViewLL: LinearLayout
    private lateinit var actionsScrollViewLL: LinearLayout
    private lateinit var addConditionButton: MaterialButton
    private lateinit var editTaskTitle: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.task_profile)

        initializeViews()
        restoreState(savedInstanceState)

        performFirsLaunchOperations()
    }

    /**
     * perform operations when opening the the activity for the first time
     */
    private fun performFirsLaunchOperations() {
        val sp = Ido4uApp.applicationContext()
            .getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
        if (!sp.getBoolean(SHOWED_TASK_PROFILE_TUTORIAL, false)) {
            createTaskProfileTutorial()
        }
    }

    /**
     * create a tutorial for the user on how to create a new task
     */
    private fun createTaskProfileTutorial() {
        val views = arrayOf<View>(
            findViewById(R.id.edit_task_title),
            findViewById(R.id.conditions_information),
            findViewById(R.id.add_condition_button),
            findViewById(R.id.actions_information),
            findViewById(R.id.add_action_button)
        )

        val texts = listOf(
            getString(R.string.task_name_tutorial),
            getString(R.string.task_cond_info_tutorial),
            getString(R.string.task_add_condit_tutorial),
            getString(R.string.task_actions_info_tut),
            getString(R.string.task_add_action_tutorial)
        )

        createTutorial(this@TaskProfileActivity, texts, SHOWED_TASK_PROFILE_TUTORIAL, *views)
    }

    private fun restoreState(bundle: Bundle?) {
        if (bundle != null) {
            populateFieldsFromCurrentEdit(bundle)
        } else if (id != -1) {
            populateFieldsFromExistingTask()
        }

    }

    private fun populateFieldsFromCurrentEdit(bundle: Bundle) {
        if (bundle.getBoolean(CONDITION_EXIST)) {
            val conditionString = bundle.getString(RESTORE_CONDITION, null)
            if (conditionString != null) {
                createCondition(gson.fromJson(conditionString, Task.Condition::class.java))
            }
        }
        if (bundle.getBoolean(ACTIONS_EXIST)) {
            val actionListJsonString = bundle.getString(RESTORE_ACTIONS, null)
            if (actionListJsonString != null) {
                val groupListType: Type = object : TypeToken<MutableList<Task.Action>>() {}.type
                val actionList: MutableList<Task.Action> =
                    gson.fromJson(actionListJsonString, groupListType)
                for (action in actionList) {
                    addAction(action)
                }
            }
        }

    }

    private fun initializeViews() {
        conditionScrollViewLL = findViewById(R.id.condition_scrollView_linearLayout)
        actionsScrollViewLL = findViewById(R.id.actions_scrollView_linearLayout)
        addConditionButton = findViewById(R.id.add_condition_button)
        editTaskTitle = findViewById(R.id.edit_task_title)

        val addActionButton: MaterialButton = findViewById(R.id.add_action_button)
        val applyNewTaskButton: MaterialButton = findViewById(R.id.apply_new_task_button)
        val deleteTaskButton: MaterialButton = findViewById(R.id.delete_task_button)
        val removeConditionButton: MaterialButton = findViewById(R.id.remove_condition_button)
        val removeActionButton: MaterialButton = findViewById(R.id.remove_action_button)

        id = intent.getIntExtra("id", -1)

        addConditionButton.setOnClickListener {
            addCondition()
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
            if (actionsScrollViewLL.childCount < MAX_ACTIONS) {
                addAction()
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Can't Add More Actions")
                    .setMessage("Can't create more than $MAX_ACTIONS actions")
                    .setNeutralButton(
                        android.R.string.ok
                    ) { _, _ -> }
                    .show()
            }
        }

        removeActionButton.setOnClickListener {
            removeActions()
        }

        applyNewTaskButton.setOnClickListener {
            if (condition == null || actions.size == 0 || editTaskTitle.text.isEmpty()) {
                showCantCreateTaskDialog()
                return@setOnClickListener
            }
            val task =
                Task(editTaskTitle.text.toString(), true, condition!!, actions.toTypedArray())
            if (id == -1) {
                TaskManager.addTask(task)
            } else {
                TaskManager.setPosition(id, task)
            }
            startService(this)
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
    }

    private fun showCantCreateTaskDialog() {
        var message = ""
        when {
            editTaskTitle.text.isEmpty() -> {
                message = "Can't create a task without a title"
            }
            condition == null -> {
                message = "Can't create a task with no condition"
            }
            actions.size == 0 -> {
                message = "Can't create a task with no actions"
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Can't Create Task")
            .setMessage(message)
            .setNeutralButton(
                android.R.string.ok
            ) { _, _ -> }
            .show()
    }

    private fun removeActions() {
        actions = mutableListOf()
        actionsScrollViewLL.removeAllViewsInLayout()
        val tv = TextView(this)
        tv.hint = getString(R.string.add_an_action)
        actionsScrollViewLL.addView(tv)
    }

    private fun populateFieldsFromExistingTask() {
        val task = TaskManager.getPosition(id)
        conditionScrollViewLL.removeAllViewsInLayout()
        actionsScrollViewLL.removeAllViewsInLayout()
        addConditionButton.text = getString(R.string.edit_condition)
        editTaskTitle.setText(task.name)
        createCondition(task.condition)
        for (action in task.actions) {
            addAction(action)

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

    private fun addAction(action: Task.Action) {
        if (actions.size == 0) {
            actionsScrollViewLL.removeAllViewsInLayout()
        }
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
        conditionScrollViewLL.removeAllViewsInLayout()
        condition = newCondition
        val tv = TextView(this)
        tv.text = newCondition.description
        conditionScrollViewLL.removeAllViewsInLayout()
        conditionScrollViewLL.addView(tv)
        addConditionButton.text = getString(R.string.edit_condition)
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
            resultCode == Activity.RESULT_OK && data != null
        ) {
            addAction(gson.fromJson(data.getStringExtra(ACTION), Task.Action::class.java))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (condition != null) {
            outState.putBoolean(CONDITION_EXIST, true)
            outState.putString(RESTORE_CONDITION, gson.toJson(condition))
        } else {
            outState.putBoolean(CONDITION_EXIST, false)
        }
        if (actions.size > 0) {
            outState.putBoolean(ACTIONS_EXIST, true)
            outState.putString(RESTORE_ACTIONS, gson.toJson(actions))
        } else {
            outState.putBoolean(ACTIONS_EXIST, false)
        }
    }
}
