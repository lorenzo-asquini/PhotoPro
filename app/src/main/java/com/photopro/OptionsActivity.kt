package com.photopro

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ActionMode
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout

class OptionsActivity : AppCompatActivity() {

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val features = getAvailableFeatures(this, cameraManager)

        preferences = getPreferences(MODE_PRIVATE)

        //Show the extension toggles if feature is available
        showExtensionsToggles(this, features)

        //Retrieve the saved preferences and show them
        retrieveOptionsValue(this, preferences)

        //Set listener to save the new values
        setValueChangeListeners(this, preferences)

        val backArrowButton : ImageButton = findViewById(R.id.back_arrow_button)
        backArrowButton.setOnClickListener{
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        //Validate inputs even when pausing the activity
        val framesToAverageEditText = findViewById<EditText>(R.id.frame_avg_frame_number)
        validateInputFramesToAverage(framesToAverageEditText, this, preferences)

        val smartDelaySecondsEditText = findViewById<EditText>(R.id.smart_delay_seconds)
        validateInputSmartDelaySeconds(smartDelaySecondsEditText, this, preferences)
    }
}