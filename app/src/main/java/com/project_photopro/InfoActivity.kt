package com.project_photopro

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.LinearLayout

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        //Program waits until all the features have been determined
        val features = getAvailableFeatures(this)

        val backArrowButton : ImageButton = findViewById(R.id.info_to_settings_back_arrow_button)
        backArrowButton.setOnClickListener {
            finish()
        }

        //Do not show the information about the features not available

        //FLASH
        findViewById<LinearLayout>(R.id.flash_information).visibility =
            if(!features.isBackFlashAvailable && !features.isFrontFlashAvailable){
                GONE
            }else{
                VISIBLE
            }

        //NIGHT MODE
        findViewById<LinearLayout>(R.id.night_mode_information).visibility =
            if(!features.isBackNightModeAvailable && !features.isFrontNightModeAvailable){
                GONE
            }else{
                VISIBLE
            }

        //HDR
        findViewById<LinearLayout>(R.id.HDR_information).visibility =
            if(!features.isBackHDRAvailable && !features.isFrontHDRAvailable){
                GONE
            }else{
                VISIBLE
            }

        //BOKEH
        findViewById<LinearLayout>(R.id.bokeh_information).visibility =
            if(!features.isBackBokehAvailable && !features.isFrontBokehAvailable){
                GONE
            }else{
                VISIBLE
            }

        //FACE RETOUCH
        findViewById<LinearLayout>(R.id.face_retouch_information).visibility =
            if(!features.isBackFaceRetouchAvailable && !features.isFrontFaceRetouchAvailable){
                GONE
            }else{
                VISIBLE
            }
    }
}