package com.example.servicetest

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var editTextInput: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        editTextInput = findViewById(R.id.edit_text_input)
    }

    fun startService(v: View?) {
        val input = editTextInput!!.text.toString()
        val serviceIntent = Intent(this, ExampleService::class.java)
        serviceIntent.putExtra("inputExtra", input)
        startService(serviceIntent)
    }

    fun stopService(v: View?) {
        val serviceIntent = Intent(this, ExampleService::class.java)
        stopService(serviceIntent)
    }
}