package com.photopro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraInfoUnavailableException
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

//Functions useful for both normal camera and PRO camera

fun drawAllButtons(activity: AppCompatActivity, preferences: SharedPreferences){
    drawFlashButton(activity, preferences)
    drawSpeechShootButton(activity, preferences)
    drawFrameAvgButton(activity, preferences)
    drawPoseShootButton(activity, preferences)
    drawNightModeButton(activity, preferences)

}

fun drawFlashButton(activity: AppCompatActivity, preferences: SharedPreferences){
    val flashButton: ImageButton = activity.findViewById(R.id.flash_button)

    val flashMode = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

    when(flashMode){
        Constant.FLASH_OFF -> flashButton.setImageResource(R.drawable.flash_off)
        Constant.FLASH_ON -> flashButton.setImageResource(R.drawable.flash_on)
        Constant.FLASH_AUTO -> flashButton.setImageResource(R.drawable.flash_auto)
    }
}

fun changeFlashValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

    val newValue = (currentValue+1) % 3

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.FLASH_KEY, newValue)
    editor.apply()
}

fun drawSpeechShootButton(activity: AppCompatActivity, preferences: SharedPreferences){
    val speechShootButton: ImageButton = activity.findViewById(R.id.speech_shoot_button)

    val speechShootValue = preferences.getInt(SharedPrefs.SPEECH_SHOOT_KEY, Constant.SPEECH_SHOOT_OFF)

    when(speechShootValue){
        Constant.SPEECH_SHOOT_OFF -> speechShootButton.setImageResource(R.drawable.speech_shoot_off)
        Constant.SPEECH_SHOOT_ON -> speechShootButton.setImageResource(R.drawable.speech_shoot_on)
    }
}

fun changeSpeechShootValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.SPEECH_SHOOT_KEY, Constant.SPEECH_SHOOT_OFF)

    val newValue = (currentValue+1) % 2

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.SPEECH_SHOOT_KEY, newValue)
    editor.apply()
}

fun drawFrameAvgButton(activity: AppCompatActivity, preferences: SharedPreferences){
    val frameAvgButton: ImageButton = activity.findViewById(R.id.frame_avg_button)

    val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    when(frameAvgValue){
        Constant.FRAME_AVG_OFF -> frameAvgButton.setImageResource(R.drawable.frame_avg_off)
        Constant.FRAME_AVG_ON -> frameAvgButton.setImageResource(R.drawable.frame_avg_on)
    }
}

fun changeFrameAvgValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    val newValue = (currentValue+1) % 2

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.FRAME_AVG_KEY, newValue)
    editor.apply()
}

fun drawPoseShootButton(activity: AppCompatActivity, preferences: SharedPreferences){
    val poseShootButton: ImageButton = activity.findViewById(R.id.pose_shoot_button)

    val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)

    when(poseShootValue){
        Constant.POSE_SHOOT_OFF -> poseShootButton.setImageResource(R.drawable.pose_shoot_off)
        Constant.POSE_SHOOT_ON -> poseShootButton.setImageResource(R.drawable.pose_shoot_on)
    }
}

fun changePoseShootValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)

    val newValue = (currentValue+1) % 2

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.POSE_SHOOT_KEY, newValue)
    editor.apply()
}

fun drawNightModeButton(activity: AppCompatActivity, preferences: SharedPreferences){
    val nightModeButton: ImageButton = activity.findViewById(R.id.night_mode_button)

    val nightMode = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)

    when(nightMode){
        Constant.NIGHT_MODE_OFF -> nightModeButton.setImageResource(R.drawable.night_mode_off)
        Constant.NIGHT_MODE_ON -> nightModeButton.setImageResource(R.drawable.night_mode_on)
        Constant.NIGHT_MODE_AUTO -> nightModeButton.setImageResource(R.drawable.night_mode_auto)
    }
}

fun changeNightModeValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)

    val newValue = (currentValue+1) % 3

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.NIGHT_MODE_KEY, newValue)
    editor.apply()
}

fun initialiseChangeCameraButton(activity: AppCompatActivity, preferences: SharedPreferences){
    //Check if both a front camera and back camera are available
    var hasFrontCamera = false
    var hasBackCamera = false
    //TODO: Maybe possible to use CameraCharacteristics
    val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
    cameraProviderFuture.addListener({
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider = cameraProviderFuture.get()
        try {
            hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }

        try {
            hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        } catch (e: CameraInfoUnavailableException) {
            e.printStackTrace()
        }

        val changeCameraButton : ImageButton = activity.findViewById(R.id.change_camera_button)

        //If one camera is not available, disable the change camera button
        //Set the only available camera as the only value possible
        if(!hasFrontCamera || !hasBackCamera){
            changeCameraButton.visibility = View.GONE

            val editor = preferences.edit()
            if(hasFrontCamera){
                editor.putInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_FRONT)
            }else{  //Just else because at least a camera is present
                editor.putInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK)
            }
            editor.apply()
        }
    }, ContextCompat.getMainExecutor(activity))
}

fun changeCameraFacingValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK)

    val newValue = (currentValue+1) % 2

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