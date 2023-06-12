package com.project_photopro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class OptionsActivity : AppCompatActivity() {

    //Avoid opening the info menu multiple times when spamming button
    private var isInfoButtonClicked = false

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_options)

        //Program waits until all the features have been determined
        val features = getAvailableFeatures(this)

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

        //Add listener to button to open the information activity
        val infoButton: ImageButton = findViewById(R.id.info_image_button)
        infoButton.setOnClickListener{
            if(!isInfoButtonClicked) {
                val openSettingsIntent = Intent(this, InfoActivity::class.java)
                startActivity(openSettingsIntent)
                isInfoButtonClicked = true
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //Retrieve the saved preferences and show them
        //Necessary when coming back from info activity and an invalid value was set
        retrieveOptionsValue(this, preferences)

        //Valid values because just retrieved
        val framesToAverageEditText = findViewById<EditText>(R.id.frame_avg_frame_number_editText)
        framesToAverageEditText.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        framesToAverageEditText.setTextColor(getColor(R.color.white))

        val smartDelaySecondsEditText = findViewById<EditText>(R.id.smart_delay_seconds_editText)
        smartDelaySecondsEditText.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
        smartDelaySecondsEditText.setTextColor(getColor(R.color.white))
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