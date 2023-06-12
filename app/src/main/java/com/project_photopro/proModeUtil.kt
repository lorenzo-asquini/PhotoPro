package com.project_photopro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint.Cap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlin.math.roundToInt

data class ProModeRanges(
    //If values are still null, then that feature is not supported by the camera
    //ISO
    var frontISORange : Range<Int>? = null,
    var backISORange : Range<Int>? = null,

    //Exposure time (Shutter speed)
    var frontExposureTimeRange : Range<Long>? = null,
    var backExposureTimeRange : Range<Long>? = null
)

fun drawProModeMenu(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    val proModeMenu: LinearLayout = activity.findViewById(R.id.pro_mode_menu)
    val proModeButton: ImageButton = activity.findViewById(R.id.pro_mode_button)

    //If the camera hardware level is not high enough, show will be false and Pro Mode will not available
    if(!show){
        proModeButton.visibility = GONE
        proModeMenu.visibility = GONE
        return
    }

    //Set new ranges
    handleProModeCommands(activity as MainActivity, preferences)

    proModeButton.visibility = VISIBLE

    val proModeValue = preferences.getInt(SharedPrefs.PRO_MODE_KEY, Constant.PRO_MODE_OFF)

    when(proModeValue){
        Constant.PRO_MODE_OFF -> {
            proModeMenu.visibility = GONE
            proModeButton.setImageResource(R.drawable.pro)

            //Set everything to auto
            handleSetProSettingsThread(activity, preferences, false)
        }
        Constant.PRO_MODE_ON -> {
            proModeMenu.visibility = VISIBLE
            proModeButton.setImageResource(R.drawable.normal)

            //Adjust settings in near real time
            handleSetProSettingsThread(activity, preferences, true)
        }
    }
}

fun changeProModeValue(preferences: SharedPreferences){
    val currentValue = preferences.getInt(SharedPrefs.PRO_MODE_KEY, Constant.PRO_MODE_OFF)

    val newValue = (currentValue+1) % Constant.PRO_MODE_STATES

    val editor = preferences.edit()
    editor.putInt(SharedPrefs.PRO_MODE_KEY, newValue)
    editor.apply()
}

//At least one of the two sliders is considered available if the Pro Mode menu is shown
fun handleProModeCommands(activity: MainActivity, preferences: SharedPreferences) {

    //Add listener to hide and show pro mode sliders
    val hideProModeSwitch: SwitchMaterial =
        activity.findViewById(R.id.hide_pro_mode_switch)

    val proModeSliders: LinearLayout = activity.findViewById(R.id.pro_mode_sliders)
    hideProModeSwitch.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            proModeSliders.visibility = GONE
        } else {
            proModeSliders.visibility = VISIBLE
        }
    }

    handleISOSlider(activity, preferences)
    handleShutterSpeedSlider(activity, preferences)
}

fun handleISOSlider(activity: MainActivity, preferences: SharedPreferences){
    val proRanges = getProModeSliderRanges(activity)

    //Listener to ISO Slider change value
    val isoSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.iso_slider)
    val isoTextView: TextView = activity.findViewById(R.id.iso_slider_value)

    //Set ranges
    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
        if(proRanges.backISORange == null){
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = VISIBLE
            isoSlider.valueFrom = proRanges.backISORange!!.lower.toFloat()
            isoSlider.valueTo = proRanges.backISORange!!.upper.toFloat()
        }
    }else{  //Front
        if(proRanges.frontISORange == null){
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = VISIBLE
            isoSlider.valueFrom = proRanges.frontISORange!!.lower.toFloat()
            isoSlider.valueTo = proRanges.frontISORange!!.upper.toFloat()
        }
    }

    //View the selected value restoring saved one
    val savedISOValue =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getInt(SharedPrefs.ISO_BACK_KEY, proRanges.backISORange!!.lower)
        }else{
            preferences.getInt(SharedPrefs.ISO_FRONT_KEY, proRanges.frontISORange!!.lower)
        }

    isoTextView.text = savedISOValue.toString()
    isoSlider.value = savedISOValue.toFloat()

    //Add listener
    isoSlider.addOnChangeListener { _, value, _ ->
        isoTextView.text = value.toInt().toString()

        //Save new value
        val editor = preferences.edit()
        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putInt(SharedPrefs.ISO_BACK_KEY, value.toInt())
        } else {
            editor.putInt(SharedPrefs.ISO_FRONT_KEY, value.toInt())
        }
        editor.apply()
    }
}

fun handleShutterSpeedSlider(activity: MainActivity, preferences: SharedPreferences){
    val proRanges = getProModeSliderRanges(activity)

    //Listener to Shutter Speed Slider change value
    val shutterSpeedSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.shutter_speed_slider)
    val shutterSpeedTextView: TextView = activity.findViewById(R.id.shutter_speed_slider_value)

    //Set ranges
    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
        if(proRanges.backExposureTimeRange == null){
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = VISIBLE
            shutterSpeedSlider.valueFrom = proRanges.backExposureTimeRange!!.lower.toFloat()
            shutterSpeedSlider.valueTo = proRanges.backExposureTimeRange!!.upper.toFloat()
        }
    }else{  //Front
        if(proRanges.frontExposureTimeRange == null){
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = VISIBLE
            shutterSpeedSlider.valueFrom = proRanges.frontExposureTimeRange!!.lower.toFloat()
            shutterSpeedSlider.valueTo = proRanges.frontExposureTimeRange!!.upper.toFloat()
        }
    }

    //View the selected value restoring saved one
    val savedShutterSpeedValue =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, proRanges.backExposureTimeRange!!.lower.toFloat())
        }else{
            preferences.getFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, proRanges.frontExposureTimeRange!!.lower.toFloat())
        }

    shutterSpeedTextView.text = convertNanosecondsToReadableTime(savedShutterSpeedValue)
    shutterSpeedSlider.value = savedShutterSpeedValue

    //Add the listener
    shutterSpeedSlider.addOnChangeListener { _, value, _ ->
        shutterSpeedTextView.text = convertNanosecondsToReadableTime(value)
        //Save new value
        val editor = preferences.edit()
        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, value)
        } else {
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, value)
        }
        editor.apply()
    }
}

private var proModeSettingsThread : Thread? = null
@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
fun handleSetProSettingsThread(activity: MainActivity, preferences: SharedPreferences, run : Boolean){

    //Stop the last thread
    proModeSettingsThread?.interrupt()

    //If the mode is normal, it is not necessary to restart the thread
    //All settings will return to normal because a new camera will be started
    if(!run){
        return
    }
    val proRanges = getProModeSliderRanges(activity)

    //Create a new thread because Pro Mode is activated
    proModeSettingsThread = Thread {

        //Wait until camera has started
        while (activity.camera == null) {
            Thread.sleep(5)
        }

        try {
            while (!Thread.currentThread().isInterrupted) {

                val camera2CameraControl = Camera2CameraControl.from(activity.camera!!.cameraControl)

                val captureRequestOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    .build()

                camera2CameraControl.addCaptureRequestOptions(captureRequestOptions)

                var savedISOValue: Int
                var savedShutterSpeedValue: Float

                //TODO:Handle range = null

                if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
                    savedISOValue =
                        preferences.getInt(SharedPrefs.ISO_BACK_KEY, proRanges.backISORange!!.lower)
                    savedShutterSpeedValue =
                        preferences.getFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, proRanges.backExposureTimeRange!!.lower.toFloat())

                } else {  //Front camera
                    savedISOValue =
                        preferences.getInt(SharedPrefs.ISO_FRONT_KEY, proRanges.frontISORange!!.lower)
                    savedShutterSpeedValue =
                        preferences.getFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, proRanges.frontExposureTimeRange!!.lower.toFloat())
                }

                //Disable automatic AE and set pro values (necessary to set all 3 values that are not automatic anymore)
                val newCaptureRequestOptions = CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, savedISOValue)
                    //Target is 60fps. If exposure time is greater than frame duration, it is automatically adjusted
                    .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 16666666)
                    .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, savedShutterSpeedValue.toLong())
                    .build()

                camera2CameraControl.addCaptureRequestOptions(newCaptureRequestOptions)

                Thread.sleep(100)
            }
        } catch (e: InterruptedException) {
            Log.e(Constant.TAG, e.toString())
        }
    }
    proModeSettingsThread?.start()
}

fun getProModeSliderRanges(activity: AppCompatActivity) : ProModeRanges{
    val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val frontCameraId = getFrontCameraId(cameraManager)
    val backCameraId = getBackCameraId(cameraManager)

    val ranges = ProModeRanges()

    if(frontCameraId != null) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
        ranges.frontISORange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        ranges.frontExposureTimeRange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    }

    if(backCameraId != null) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId)
        ranges.backISORange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        ranges.backExposureTimeRange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    }

    return ranges
}

fun convertNanosecondsToReadableTime(nanoseconds: Float) : String{
    val microseconds : Float = (nanoseconds / 1000.0 * 10).roundToInt() / 10F
    val milliseconds : Float = (nanoseconds / 1_000_000.0 * 10).roundToInt() / 10F
    val seconds : Float = (nanoseconds / 1_000_000_000.0 * 10).roundToInt() / 10F

    if(nanoseconds < 1000){
        return "$nanoseconds ns"
    }else if(nanoseconds < 1_000_000){
        return "$microseconds μs"
    }else if (nanoseconds < 1_000_000_000){
        return "$milliseconds ms"
    }else{
        return "$seconds s"
    }
}