package com.project_photopro

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.util.Log
import android.util.Range
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Math.round
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
    handleProModeCommands(activity, preferences)

    proModeButton.visibility = VISIBLE

    val proMode = preferences.getInt(SharedPrefs.PRO_MODE_KEY, Constant.PRO_MODE_OFF)

    when(proMode){
        Constant.PRO_MODE_OFF -> {
            proModeMenu.visibility = GONE
            proModeButton.setImageResource(R.drawable.pro)
        }
        Constant.PRO_MODE_ON -> {
            proModeMenu.visibility = VISIBLE
            proModeButton.setImageResource(R.drawable.normal)
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
fun handleProModeCommands(activity: AppCompatActivity, preferences: SharedPreferences) {

    //Add listener to hide and show pro mode sliders
    val hideProModeSwitch: com.google.android.material.switchmaterial.SwitchMaterial =
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

    //Add listener to reset the pro mode values
    val resetProModeButton: ImageButton = activity.findViewById(R.id.reset_pro_mode_button)
    resetProModeButton.setOnClickListener {
        val editor = preferences.edit()
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putInt(SharedPrefs.ISO_BACK_KEY, Constant.PRO_MODE_AUTO_VALUE)
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, Constant.PRO_MODE_AUTO_VALUE.toFloat())
        }

        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            editor.putInt(SharedPrefs.ISO_FRONT_KEY, Constant.PRO_MODE_AUTO_VALUE)
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, Constant.PRO_MODE_AUTO_VALUE.toFloat())
        }
        editor.apply()

        //Set values for the slider to auto
        val isoSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.iso_slider)
        val isoTextView: TextView = activity.findViewById(R.id.iso_slider_value)
        isoSlider.value = isoSlider.valueFrom //If auto, set slider to the left
        isoTextView.text = "A"

        val shutterSpeedSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.shutter_speed_slider)
        val shutterSpeedTextView: TextView = activity.findViewById(R.id.shutter_speed_slider_value)
        shutterSpeedSlider.value = shutterSpeedSlider.valueFrom  //If auto, set slider to the left
        shutterSpeedTextView.text = "A"

        //TODO:Change camera settings
    }
}

fun handleISOSlider(activity: AppCompatActivity, preferences: SharedPreferences){
    val proRanges = getSliderRanges(activity)

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
    }

    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_FRONT) == Constant.CAMERA_FRONT) {
        if(proRanges.frontISORange == null){
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = VISIBLE
            isoSlider.valueFrom = proRanges.frontISORange!!.lower.toFloat()
            isoSlider.valueTo = proRanges.frontISORange!!.upper.toFloat()
        }
    }

    //View the selected value
    val savedISOValue =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getInt(SharedPrefs.ISO_BACK_KEY, Constant.PRO_MODE_AUTO_VALUE)
        }else{
            preferences.getInt(SharedPrefs.ISO_FRONT_KEY, Constant.PRO_MODE_AUTO_VALUE)
        }

    if(savedISOValue == Constant.PRO_MODE_AUTO_VALUE) {
        isoSlider.value = isoSlider.valueFrom //If auto, set slider to the left
        isoTextView.text = "A"
    }else{
        isoTextView.text = savedISOValue.toString()
        isoSlider.value = savedISOValue.toFloat()
    }

    //Add listener
    isoSlider.addOnChangeListener { _, value, _ ->
        isoTextView.text = value.toInt().toString()

        //Save new value
        val editor = preferences.edit()
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putInt(SharedPrefs.ISO_BACK_KEY, value.toInt())
        }else{
            editor.putInt(SharedPrefs.ISO_FRONT_KEY, value.toInt())
        }
        editor.apply()

        //TODO: Set camera setting
    }
}

fun handleShutterSpeedSlider(activity: AppCompatActivity, preferences: SharedPreferences){
    val proRanges = getSliderRanges(activity)

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
    }

    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_FRONT) == Constant.CAMERA_FRONT) {
        if(proRanges.frontExposureTimeRange == null){
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = GONE
        }else{
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = VISIBLE
            shutterSpeedSlider.valueFrom = proRanges.frontExposureTimeRange!!.lower.toFloat()
            shutterSpeedSlider.valueTo = proRanges.frontExposureTimeRange!!.upper.toFloat()
        }
    }

    //View the selected value
    val savedShutterSpeedValue =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, Constant.PRO_MODE_AUTO_VALUE.toFloat())
        }else{
            preferences.getFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, Constant.PRO_MODE_AUTO_VALUE.toFloat())
        }

    if(savedShutterSpeedValue == Constant.PRO_MODE_AUTO_VALUE.toFloat()) {
        shutterSpeedSlider.value = shutterSpeedSlider.valueFrom  //If auto, set slider to the left
        shutterSpeedTextView.text = "A"
    }else{
        shutterSpeedTextView.text = convertNanosecondsToReadableTime(savedShutterSpeedValue)
        shutterSpeedSlider.value = savedShutterSpeedValue
    }

    //Add the listener
    shutterSpeedSlider.addOnChangeListener { _, value, _ ->
        shutterSpeedTextView.text = convertNanosecondsToReadableTime(value)

        //Save new value
        val editor = preferences.edit()
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_BACK_KEY, value)
        }else{
            editor.putFloat(SharedPrefs.SHUTTER_SPEED_FRONT_KEY, value)
        }
        editor.apply()

        //TODO: Set camera setting
    }
}

fun getSliderRanges(activity: AppCompatActivity) : ProModeRanges{
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