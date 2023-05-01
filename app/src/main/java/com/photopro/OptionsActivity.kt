package com.photopro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class OptionsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        val backArrowButton : ImageButton = findViewById(R.id.back_arrow_button)
        backArrowButton.setOnClickListener{
            finish()
        }
    }
}