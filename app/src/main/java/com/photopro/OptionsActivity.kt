package com.photopro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

//Avoid opening the info menu multiple times when spamming button
private var isInfoButtonClicked = false
class OptionsActivity : CameraAppCompactActivity() {

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences

    //Necessary global and public because some values as set asynchronously and they may not be available with a return
    override val features : AvailableFeatures = AvailableFeatures()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        getAvailableFeatures(this)

        preferences = getSharedPreferences(SharedPrefs.SHARED_PREFERENCES_KEY, MODE_PRIVATE)

        //Show the extension toggles if features are available
        showExtensionsToggles(this, features)

        //Retrieve the saved preferences and show them
        retrieveOptionsValue(this, preferences)

        //Set listener to save the new values
        setValueChangeListeners(this, preferences)

        val backArrowButton : ImageButton = findViewById(R.id.back_arrow_button)
        backArrowButton.setOnClickListener{
            finish()
        }

        //Add listener to button to open the options menu
        val infoButton: ImageButton = findViewById(R.id.info_image_button)
        infoButton.setOnClickListener{
            if(!isInfoButtonClicked) {
                val openSettingsIntent = Intent(this, InfoActivity::class.java)
                startActivity(openSettingsIntent)
                isInfoButtonClicked = true
            }
        }
    }

    override fun onPause() {
        super.onPause()

        //Reset the value when pausing the current activity
        isInfoButtonClicked = false

        //Validate inputs even when pausing the activity
        val framesToAverageEditText = findViewById<EditText>(R.id.frame_avg_frame_number_editText)
        validateInputFramesToAverage(framesToAverageEditText, this, preferences)

        val smartDelaySecondsEditText = findViewById<EditText>(R.id.smart_delay_seconds_editText)
        validateInputSmartDelaySeconds(smartDelaySecondsEditText, this, preferences)
    }

    //Hide the keyboard when touching anywhere in the activity
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (currentFocus != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)

            //Remove cursors when hiding keyboard
            findViewById<EditText>(R.id.frame_avg_frame_number_editText).clearFocus()
            findViewById<EditText>(R.id.smart_delay_seconds_editText).clearFocus()
        }
        return super.dispatchTouchEvent(ev)
    }
}