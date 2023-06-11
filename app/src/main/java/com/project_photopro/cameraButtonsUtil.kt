package com.project_photopro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

//Functions useful for both normal camera and PRO camera

fun drawAllButtons(activity: AppCompatActivity, preferences: SharedPreferences, features: AvailableFeatures){

    //Draw flash button and night mode button only if those features are available for the given camera
    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
        if(features.isFrontFlashAvailable){
            drawFlashButton(activity, preferences, true)
        }else{
            drawFlashButton(activity, preferences, false)
        }

        if(features.isFrontNightModeAvailable){
            drawNightModeButton(activity, preferences, true)
        }else{
            drawNightModeButton(activity, preferences, false)
        }

        if(features.isFrontProModeAvailable){
            drawProModeMenu(activity, preferences, true)  //Present in another file
        }else{
            drawProModeMenu(activity, preferences, false)  //Present in another file
        }
    }else{  //Back camera
        if(features.isBackFlashAvailable){
            drawFlashButton(activity, preferences, true)
        }else{
            drawFlashButton(activity, preferences, false)
        }

        if(features.isBackNightModeAvailable){
            drawNightModeButton(activity, preferences, true)
        }else{
            drawNightModeButton(activity, preferences, false)
        }

        if(features.isBackProModeAvailable){
            drawProModeMenu(activity, preferences, true)  //Present in another file
        }else{
            drawProModeMenu(activity, preferences, false)  //Present in another file
        }
    }

    drawFrameAvgButton(activity, preferences, true)
    drawSmartDelayButton(activity, preferences, true)
}

fun drawFlashButton(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    val flashButton: ImageButton = activity.findViewById(R.id.flash_button)

    if(!show){
        flashButton.visibility = View.GONE
        return
    }
    flashButton.visibility = View.VISIBLE

    val flashMode = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

    when(flashMode){
        Constant.FLASH_OFF -> flashButton.setImageResource(R.drawable.flash_off)
        Constant.FLASH_ON -> flashButton.setImageResource(R.drawable.flash_on)
        Constant.FLASH_AUTO -> flashButton.setImageResource(R.drawable.flash_auto)
        Constant.FLASH_ALWAYS_ON -> flashButton.setImageResource(R.drawable.flash_always_on)
    }
}

fun changeFlashValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

    val newValue = (currentValue+1) % Constant.FLASH_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.FLASH_KEY, newValue)
    editor.apply()
}

fun drawFrameAvgButton(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    val frameAvgButton: ImageButton = activity.findViewById(R.id.frame_avg_button)

    if(!show){
        frameAvgButton.visibility = View.GONE
        return
    }
    frameAvgButton.visibility = View.VISIBLE

    val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    //From any state, the final color after touching the icon should be white
    frameAvgButton.setColorFilter(activity.getColor(R.color.white))

    when(frameAvgValue){
        Constant.FRAME_AVG_OFF -> frameAvgButton.setImageResource(R.drawable.frame_avg_off)
        Constant.FRAME_AVG_ON -> frameAvgButton.setImageResource(R.drawable.frame_avg_on)
    }
}

fun changeFrameAvgValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    val newValue = (currentValue+1) % Constant.FRAME_AVG_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.FRAME_AVG_KEY, newValue)
    editor.apply()
}

fun drawSmartDelayButton(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    val smartDelayButton: ImageButton = activity.findViewById(R.id.smart_delay_button)

    //In any case, when drawing the button there should not be the timer visible
    //(Both if not active or if just activated)
    val smartDelayTimer : TextView = activity.findViewById(R.id.smart_delay_timer)
    smartDelayTimer.visibility = View.INVISIBLE

    if(!show){
        smartDelayButton.visibility = View.GONE
        return
    }
    smartDelayButton.visibility = View.VISIBLE

    val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)

    when(smartDelayValue){
        Constant.SMART_DELAY_OFF -> smartDelayButton.setImageResource(R.drawable.smart_delay_off)
        Constant.SMART_DELAY_ON -> smartDelayButton.setImageResource(R.drawable.smart_delay_on)
    }
}

fun changeSmartDelayValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)

    val newValue = (currentValue+1) % Constant.SMART_DELAY_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.SMART_DELAY_KEY, newValue)
    editor.apply()
}

fun drawNightModeButton(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    val nightModeButton: ImageButton = activity.findViewById(R.id.night_mode_button)

    if(!show){
        nightModeButton.visibility = View.GONE
        return
    }
    nightModeButton.visibility = View.VISIBLE

    val nightMode = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)

    when(nightMode){
        Constant.NIGHT_MODE_OFF -> nightModeButton.setImageResource(R.drawable.night_mode_off)
        Constant.NIGHT_MODE_ON -> nightModeButton.setImageResource(R.drawable.night_mode_on)
        Constant.NIGHT_MODE_AUTO -> nightModeButton.setImageResource(R.drawable.night_mode_auto)
    }
}

fun changeNightModeValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)

    val newValue = (currentValue+1) % Constant.NIGHT_MODE_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.NIGHT_MODE_KEY, newValue)
    editor.apply()
}

fun initialiseChangeCameraButton(activity: AppCompatActivity, features: AvailableFeatures, preferences: SharedPreferences){
    val changeCameraButton : ImageButton = activity.findViewById(R.id.change_camera_button)

    //If one camera is not available, disable the change camera button
    //Set the only available camera as the only value possible
    if(!features.isFrontCameraAvailable || !features.isBackCameraAvailable){
        changeCameraButton.visibility = View.GONE

        val editor = preferences.edit()
        if(!features.isFrontCameraAvailable){
            editor.putInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_FRONT)
        }else{  //Just else because at least a camera is present
            editor.putInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK)
        }
        editor.apply()
    }
}

fun changeCameraFacingValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK)

    val newValue = (currentValue+1) % Constant.CAMERA_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.CAMERA_FACING_KEY, newValue)
    editor.apply()
}

fun openGallery(baseContext : Context) {
    //Create an intent that opens the default gallery app
    val intent = Intent(Intent.ACTION_MAIN)
    //The app must support CATEGORY_APP_GALLERY (most gallery apps do, but there may be some exceptions)
    intent.addCategory(Intent.CATEGORY_APP_GALLERY)

    //This flag is necessary to open the gallery app as a new task, and not as an activity of this app
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    // Start the default gallery app
    baseContext.startActivity(intent)
}