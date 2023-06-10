package com.project_photopro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val backArrowButton : ImageButton = findViewById(R.id.info_to_settings_back_arrow_button)
        backArrowButton.setOnClickListener {
            finish()
        }
    }
}