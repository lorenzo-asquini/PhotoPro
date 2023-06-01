package com.photopro

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial


//Draw extension options according to the feature availability
//If an extension is not available for both the front and the back camera, hide the row
//If an extension is not available only for one of the two, disable that check
fun showExtensionsToggles(activity: AppCompatActivity, features: AvailableFeatures){

    //See if at least one extension is available. If not, do not show the extension choices
    var isOneExtensionAvailable = false

    //HDR
    if(!features.isFrontHDRAvailable && !features.isBackHDRAvailable){

        //Do not show the row containing the HDR choice if not available
        activity.findViewById<ImageView>(R.id.HDR_logo).visibility = View.GONE
        activity.findViewById<TextView>(R.id.HDR_textView).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.HDR_front_camera_switch).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.HDR_back_camera_switch).visibility = View.GONE
    }else if(!features.isFrontHDRAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.HDR_front_camera_switch).isEnabled = false
    }else if(!features.isBackHDRAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.HDR_back_camera_switch).isEnabled = false
    }else{
        isOneExtensionAvailable = true
    }

    //Bokeh
    if(!features.isFrontBokehAvailable && !features.isBackBokehAvailable){

        //Do not show the row containing the bokeh choice if not available
        activity.findViewById<ImageView>(R.id.bokeh_logo).visibility = View.GONE
        activity.findViewById<TextView>(R.id.bokeh_textView).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.bokeh_front_camera_switch).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.bokeh_back_camera_switch).visibility = View.GONE
    }else if(!features.isFrontBokehAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.bokeh_front_camera_switch).isEnabled = false
    }else if(!features.isBackBokehAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.bokeh_back_camera_switch).isEnabled = false
    }else{
        isOneExtensionAvailable = true
    }

    //Face retouch
    if(!features.isFrontFaceRetouchAvailable && !features.isBackFaceRetouchAvailable){

        //Do not show the row containing the HDR choice if not available
        activity.findViewById<ImageView>(R.id.face_retouch_logo).visibility = View.GONE
        activity.findViewById<TextView>(R.id.face_retouch_textView).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.face_retouch_front_camera_switch).visibility = View.GONE
        activity.findViewById<SwitchMaterial>(R.id.face_retouch_back_camera_switch).visibility = View.GONE
    }else if(!features.isFrontFaceRetouchAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.face_retouch_front_camera_switch).isEnabled = false
    }else if(!features.isBackFaceRetouchAvailable){

        //Do not allow to select mode if unavailable
        activity.findViewById<SwitchMaterial>(R.id.face_retouch_back_camera_switch).isEnabled = false
    }else{
        isOneExtensionAvailable = true
    }

    //No extension available
    if(!isOneExtensionAvailable){
        activity.findViewById<LinearLayout>(R.id.extension_menu).visibility = View.GONE
    }
}

//Retrieve values from SharePreferences and show their value
fun retrieveOptionsValue(activity: AppCompatActivity, preferences: SharedPreferences){

    //Set the switches to the saved value
    //HDR
    activity.findViewById<SwitchMaterial>(R.id.HDR_front_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_FRONT_OFF) == Constant.HDR_FRONT_ON

    activity.findViewById<SwitchMaterial>(R.id.HDR_back_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_BACK_OFF) == Constant.HDR_BACK_ON

    //Bokeh
    activity.findViewById<SwitchMaterial>(R.id.bokeh_front_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_FRONT_OFF) == Constant.BOKEH_FRONT_ON

    activity.findViewById<SwitchMaterial>(R.id.bokeh_back_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_BACK_OFF) == Constant.BOKEH_BACK_ON

    //Face retouch
    activity.findViewById<SwitchMaterial>(R.id.face_retouch_front_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_FRONT_OFF) == Constant.FACE_RETOUCH_FRONT_ON

    activity.findViewById<SwitchMaterial>(R.id.face_retouch_back_camera_switch).isChecked =
        preferences.getInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_BACK_OFF) == Constant.FACE_RETOUCH_BACK_ON

    //Smart delay seconds
    activity.findViewById<EditText>(R.id.smart_delay_seconds_editText).setText(
        preferences.getInt(SharedPrefs.SMART_DELAY_SECONDS_KEY, Constant.DEFAULT_SMART_DELAY_SECONDS).toString()
    )

    //Set EditText to the saved values
    //Frames to average
    activity.findViewById<EditText>(R.id.frame_avg_frame_number_editText).setText(
        preferences.getInt(SharedPrefs.NR_FRAMES_TO_AVERAGE_KEY, Constant.DEFAULT_FRAMES_TO_AVERAGE).toString()
    )

    //Smart delay notification
    activity.findViewById<SwitchMaterial>(R.id.smart_delay_notification_sound_switch).isChecked =
        preferences.getInt(SharedPrefs.SMART_DELAY_NOTIFICATION_KEY, Constant.SMART_DELAY_NOTIFICATION_ON) == Constant.SMART_DELAY_NOTIFICATION_ON
}

//Add listener to the different elements in order to handle the value changes and save the results in SharedPreferences
fun setValueChangeListeners(activity: AppCompatActivity, preferences: SharedPreferences){

    /*SWITCHES*/
    val HDRBackSwitch = activity.findViewById<SwitchMaterial>(R.id.HDR_back_camera_switch)
    val HDRFrontSwitch = activity.findViewById<SwitchMaterial>(R.id.HDR_front_camera_switch)

    val bokehBackSwitch = activity.findViewById<SwitchMaterial>(R.id.bokeh_back_camera_switch)
    val bokehFrontSwitch = activity.findViewById<SwitchMaterial>(R.id.bokeh_front_camera_switch)

    val faceRetouchBackSwitch = activity.findViewById<SwitchMaterial>(R.id.face_retouch_back_camera_switch)
    val faceRetouchFrontSwitch = activity.findViewById<SwitchMaterial>(R.id.face_retouch_front_camera_switch)

    //HDR
    HDRBackSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_BACK_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            bokehBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_BACK_OFF)

            faceRetouchBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_BACK_OFF)

        }else{
            editor.putInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_BACK_OFF)
        }

        editor.apply()
    }

    HDRFrontSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_FRONT_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            bokehFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_FRONT_OFF)

            faceRetouchFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_FRONT_OFF)

        }else{
            editor.putInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_FRONT_OFF)
        }

        editor.apply()
    }

    //Bokeh
    bokehBackSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_BACK_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            HDRBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_BACK_OFF)

            faceRetouchBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_BACK_OFF)

        }else{
            editor.putInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_BACK_OFF)
        }

        editor.apply()
    }

    bokehFrontSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_FRONT_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            HDRFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_FRONT_OFF)

            faceRetouchFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_FRONT_OFF)

        }else{
            editor.putInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_FRONT_OFF)
        }

        editor.apply()
    }

    //Face retouch
    faceRetouchBackSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_BACK_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            HDRBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_BACK_OFF)

            bokehBackSwitch.isChecked = false
            editor.putInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_BACK_OFF)

        }else{
            editor.putInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_BACK_OFF)
        }

        editor.apply()
    }

    faceRetouchFrontSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_FRONT_ON)
            //TODO: Is it possible to have multiple extensions at the same time? If so, remove
            //Only one switch at a time can be enabled
            HDRFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_FRONT_OFF)

            bokehFrontSwitch.isChecked = false
            editor.putInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_FRONT_OFF)

        }else{
            editor.putInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_FRONT_OFF)
        }

        editor.apply()
    }

    /*EDITTEXT*/
    //Smart delay seconds
    val smartDelaySecondsEditText = activity.findViewById<EditText>(R.id.smart_delay_seconds_editText)
    //Validate input on focus change
    smartDelaySecondsEditText.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            validateInputSmartDelaySeconds(smartDelaySecondsEditText, activity, preferences)

            // Hide keyboard
            val imm = smartDelaySecondsEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(smartDelaySecondsEditText.windowToken, 0)
        }
    }

    //Validate input when action done (keyboard confirm)
    smartDelaySecondsEditText.setOnEditorActionListener { view, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
            EditorInfo.IME_ACTION_DONE -> {
                validateInputSmartDelaySeconds(smartDelaySecondsEditText, activity, preferences)

                // Hide keyboard
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                // Give up focus
                view.clearFocus()
                true
            }
            else -> false
        }
    }

    //Smart delay seconds
    val framesToAverageEditText = activity.findViewById<EditText>(R.id.frame_avg_frame_number_editText)
    //Validate input on focus change
    framesToAverageEditText.setOnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            validateInputFramesToAverage(framesToAverageEditText, activity, preferences)

            // Hide keyboard
            val imm = framesToAverageEditText.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(framesToAverageEditText.windowToken, 0)
        }
    }

    //Validate input when action done (keyboard confirm)
    framesToAverageEditText.setOnEditorActionListener { view, actionId, _ ->
        return@setOnEditorActionListener when (actionId) {
            EditorInfo.IME_ACTION_DONE -> {

                validateInputFramesToAverage(framesToAverageEditText, activity, preferences)

                // Hide keyboard
                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
                // Give up focus
                view.clearFocus()
                true
            }
            else -> false
        }
    }

    //Notification sound
    val smartDelayNotificationSoundSwitch = activity.findViewById<SwitchMaterial>(R.id.smart_delay_notification_sound_switch)

    smartDelayNotificationSoundSwitch.setOnCheckedChangeListener { _, isChecked ->
        val editor = preferences.edit()

        if(isChecked) {
            editor.putInt(SharedPrefs.SMART_DELAY_NOTIFICATION_KEY, Constant.SMART_DELAY_NOTIFICATION_ON)
        }else{
            editor.putInt(SharedPrefs.SMART_DELAY_NOTIFICATION_KEY, Constant.SMART_DELAY_NOTIFICATION_OFF)
        }

        editor.apply()
    }
}

fun validateInputSmartDelaySeconds(smartDelaySecondsEditText: EditText, activity: AppCompatActivity, preferences: SharedPreferences){
    //Input validation
    val input = smartDelaySecondsEditText.text.toString()
    val inputNumber = input.toIntOrNull()

    if (inputNumber == null || inputNumber < 1) {
        // Invalid input. Set to error color. This value will not be saved
        smartDelaySecondsEditText.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.error_color))
        smartDelaySecondsEditText.setTextColor(activity.getColor(R.color.error_color))

        val toast = Toast.makeText(activity, R.string.invalid_input, Toast.LENGTH_SHORT)
        toast.show()

    } else {
        // Valid input. Set white and save
        smartDelaySecondsEditText.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.white))
        smartDelaySecondsEditText.setTextColor(activity.getColor(R.color.white))

        val editor = preferences.edit()
        editor.putInt(SharedPrefs.SMART_DELAY_SECONDS_KEY, inputNumber)
        editor.apply()
    }
}

fun validateInputFramesToAverage(framesToAverageEditText: EditText, activity: AppCompatActivity, preferences: SharedPreferences){
    //Input validation
    val input = framesToAverageEditText.text.toString()
    val inputNumber = input.toIntOrNull()

    if (inputNumber == null || inputNumber < 1) {
        // Invalid input. Set to error color. This value will not be saved
        framesToAverageEditText.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.error_color))
        framesToAverageEditText.setTextColor(activity.getColor(R.color.error_color))

        val toast = Toast.makeText(activity, R.string.invalid_input, Toast.LENGTH_SHORT)
        toast.show()

    } else {
        // Valid input. Set white and save
        framesToAverageEditText.backgroundTintList = ColorStateList.valueOf(activity.getColor(R.color.white))
        framesToAverageEditText.setTextColor(activity.getColor(R.color.white))

        val editor = preferences.edit()
        editor.putInt(SharedPrefs.NR_FRAMES_TO_AVERAGE_KEY, inputNumber)
        editor.apply()
    }
}