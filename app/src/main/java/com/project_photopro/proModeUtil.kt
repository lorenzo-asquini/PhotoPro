package com.project_photopro

import android.content.Context
import android.content.SharedPreferences
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
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
    //Determined both the absolute ranges and the relative ranges inside the constant lists
    //ISO
    var frontISORange : Range<Int>? = null,
    var frontISORangeInList : Range<Int>? = null,
    var backISORange : Range<Int>? = null,
    var backISORangeInList : Range<Int>? = null,

    //Exposure time (Shutter speed)
    var frontExposureTimeRange : Range<Long>? = null,
    var frontExposureTimeRangeInList : Range<Int>? = null,
    var backExposureTimeRange : Range<Long>? = null,
    var backExposureTimeRangeInList : Range<Int>? = null
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

            //Reset everything once
            resetCameraOptions(activity)
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

//The slider are considered available if commands are shown
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

    val proModeValue = preferences.getInt(SharedPrefs.PRO_MODE_KEY, Constant.PRO_MODE_OFF)

    if(proModeValue == Constant.PRO_MODE_ON) {
        handleISOSlider(activity, preferences)
        handleShutterSpeedSlider(activity, preferences)
    }
}

//Slider considered available, with non-null ranges
//Values of the slider are the indexes of the values in the list
fun handleISOSlider(activity: MainActivity, preferences: SharedPreferences){
    val proRanges = getProModeSliderRanges(activity)

    //Listener to ISO Slider change value
    val isoSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.iso_slider)
    val isoTextView: TextView = activity.findViewById(R.id.iso_slider_value)

    //Set ranges
    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = VISIBLE
            isoSlider.valueFrom = proRanges.backISORangeInList!!.lower.toFloat()
            isoSlider.valueTo = proRanges.backISORangeInList!!.upper.toFloat()
    }else{  //Front
            activity.findViewById<LinearLayout>(R.id.iso_slider_block).visibility = VISIBLE
            isoSlider.valueFrom = proRanges.frontISORangeInList!!.lower.toFloat()
            isoSlider.valueTo = proRanges.frontISORangeInList!!.upper.toFloat()
    }

    //View the selected value restoring saved one (indexes in list)
    val savedISOValueIndex =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getInt(SharedPrefs.ISO_INDEX_BACK_KEY, proRanges.backISORangeInList!!.lower)
        }else{
            preferences.getInt(SharedPrefs.ISO_INDEX_FRONT_KEY, proRanges.frontISORangeInList!!.lower)
        }

    //Retrieve shown value from list
    isoTextView.text = Constant.ISO_VALUES[savedISOValueIndex].toString()
    isoSlider.value = savedISOValueIndex.toFloat()

    //Set saved values
    setProCameraOptions(activity, preferences)

    //Add listener
    isoSlider.addOnChangeListener { _, valueIndex, _ ->
        //Retrieve shown value from list
        isoTextView.text = Constant.ISO_VALUES[valueIndex.toInt()].toString()

        //Save new value (index)
        val editor = preferences.edit()
        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putInt(SharedPrefs.ISO_INDEX_BACK_KEY, valueIndex.toInt())
        } else {
            editor.putInt(SharedPrefs.ISO_INDEX_FRONT_KEY, valueIndex.toInt())
        }
        editor.apply()

        //Set new values
        setProCameraOptions(activity, preferences)
    }
}

//Slider considered available, with non-null ranges
//Values of the slider are the indexes of the values in the list
fun handleShutterSpeedSlider(activity: MainActivity, preferences: SharedPreferences){
    val proRanges = getProModeSliderRanges(activity)

    //Listener to Shutter Speed Slider change value
    val shutterSpeedSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.shutter_speed_slider)
    val shutterSpeedTextView: TextView = activity.findViewById(R.id.shutter_speed_slider_value)

    //Set ranges
    if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = VISIBLE
            shutterSpeedSlider.valueFrom = proRanges.backExposureTimeRangeInList!!.lower.toFloat()
            shutterSpeedSlider.valueTo = proRanges.backExposureTimeRangeInList!!.upper.toFloat()
    }else{  //Front
            activity.findViewById<LinearLayout>(R.id.shutter_speed_slider_block).visibility = VISIBLE
            shutterSpeedSlider.valueFrom = proRanges.frontExposureTimeRangeInList!!.lower.toFloat()
            shutterSpeedSlider.valueTo = proRanges.frontExposureTimeRangeInList!!.upper.toFloat()
    }

    //View the selected value restoring saved one (indexes in list)
    val savedShutterSpeedValueIndex =
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            preferences.getInt(SharedPrefs.SHUTTER_SPEED_INDEX_BACK_KEY, proRanges.backExposureTimeRangeInList!!.lower)
        }else{
            preferences.getInt(SharedPrefs.SHUTTER_SPEED_INDEX_FRONT_KEY, proRanges.frontExposureTimeRangeInList!!.lower)
        }

    //Retrieve shown value from list
    shutterSpeedTextView.text = convertNanosecondsToReadableTime(Constant.SHUTTER_SPEED_VALUE[savedShutterSpeedValueIndex])
    shutterSpeedSlider.value = savedShutterSpeedValueIndex.toFloat()

    //Set saved values
    setProCameraOptions(activity, preferences)

    //Add the listener
    shutterSpeedSlider.addOnChangeListener { _, valueIndex, _ ->
        //Retrieve shown value from list
        shutterSpeedTextView.text = convertNanosecondsToReadableTime(Constant.SHUTTER_SPEED_VALUE[valueIndex.toInt()])

        //Save new value (index)
        val editor = preferences.edit()
        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            editor.putInt(SharedPrefs.SHUTTER_SPEED_INDEX_BACK_KEY, valueIndex.toInt())
        } else {
            editor.putInt(SharedPrefs.SHUTTER_SPEED_INDEX_FRONT_KEY, valueIndex.toInt())
        }
        editor.apply()

        //Set new values
        setProCameraOptions(activity, preferences)
    }
}

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
fun resetCameraOptions(activity: MainActivity){
    //Wait until camera has started
    //Used a thread only to be sure that the camera is available
    Thread {
        while (activity.camera == null) {
            Thread.sleep(5)
        }

        val camera2CameraControl = Camera2CameraControl.from(activity.camera!!.cameraControl)

        //Reset the settings to Auto
        camera2CameraControl.clearCaptureRequestOptions()
    }.start()
}

@androidx.annotation.OptIn(androidx.camera.camera2.interop.ExperimentalCamera2Interop::class)
fun setProCameraOptions(activity: MainActivity, preferences: SharedPreferences){
    val proRanges = getProModeSliderRanges(activity)

    //Wait until camera has started
    //Used a thread only to be sure that the camera is available
    Thread {
        while (activity.camera == null) {
            Thread.sleep(5)
        }

        val camera2CameraControl = Camera2CameraControl.from(activity.camera!!.cameraControl)

        //Pro Mode is on
        val savedISOValueIndex: Int
        val savedShutterSpeedValueIndex: Int

        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_BACK) {
            savedISOValueIndex =
                preferences.getInt(SharedPrefs.ISO_INDEX_BACK_KEY, proRanges.backISORangeInList!!.lower)
            savedShutterSpeedValueIndex =
                preferences.getInt(SharedPrefs.SHUTTER_SPEED_INDEX_BACK_KEY, proRanges.backExposureTimeRangeInList!!.lower)

        } else {  //Front camera
            savedISOValueIndex =
                preferences.getInt(SharedPrefs.ISO_INDEX_FRONT_KEY, proRanges.frontISORangeInList!!.lower)
            savedShutterSpeedValueIndex =
                preferences.getInt(SharedPrefs.SHUTTER_SPEED_INDEX_FRONT_KEY, proRanges.frontExposureTimeRangeInList!!.lower)
        }

        //Disable automatic AE and set pro values (necessary to set all 3 values that are not automatic anymore)
        //Values taken from the list of valid values
        val newCaptureRequestOptions = CaptureRequestOptions.Builder()
            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            .setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, Constant.ISO_VALUES[savedISOValueIndex])
            //Target is 60fps. If exposure time is greater than frame duration, it is automatically adjusted
            .setCaptureRequestOption(CaptureRequest.SENSOR_FRAME_DURATION, 16666666)
            .setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, Constant.SHUTTER_SPEED_VALUE[savedShutterSpeedValueIndex].toLong())
            .build()

        camera2CameraControl.addCaptureRequestOptions(newCaptureRequestOptions)
    }.start()
}

//Get relative and absolute ranges of values supported in Pro Mode
//If relative ranges are null even if absolute ones are not, the feature is considered not available
fun getProModeSliderRanges(activity: AppCompatActivity) : ProModeRanges{
    val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val frontCameraId = getFrontCameraId(cameraManager)
    val backCameraId = getBackCameraId(cameraManager)

    val ranges = ProModeRanges()

    //Ranges in absolute values
    if(frontCameraId != null) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
        ranges.frontISORange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        ranges.frontExposureTimeRange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    }

    //Ranges relative to the list of possible values
    ranges.frontISORangeInList = getRangeInList_II(Constant.ISO_VALUES, ranges.frontISORange)
    ranges.frontExposureTimeRangeInList = getRangeInList_FL(Constant.SHUTTER_SPEED_VALUE, ranges.frontExposureTimeRange)

    //Ranges in absolute values
    if(backCameraId != null) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId)
        ranges.backISORange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        ranges.backExposureTimeRange =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
    }

    //Ranges relative to the list of possible values
    ranges.backISORangeInList = getRangeInList_II(Constant.ISO_VALUES, ranges.backISORange)
    ranges.backExposureTimeRangeInList = getRangeInList_FL(Constant.SHUTTER_SPEED_VALUE, ranges.backExposureTimeRange)
    
    return ranges
}

//List should be ordered (list of Int, range of Int)
fun getRangeInList_II(list: List<Int>, range: Range<Int>?) : Range<Int>?{
    if(range == null){
        return null
    }

    //Check if the list has at list one value that is compatible with the range given
    var rangeEmpty = true
    for(value in list){
        if(value in range){
            rangeEmpty = false
        }
    }

    if(rangeEmpty){
        return null
    }

    //Find the indexes in the given list that have values inside the range
    var low = 0
    while(list[low] !in range){
        low++
    }

    var high = list.size-1
    while(list[high] !in range) {
        high--
    }

    return Range(low, high)
}

//List should be ordered (list of Float, range of Long)
fun getRangeInList_FL(list: List<Float>, range: Range<Long>?) : Range<Int>?{
    if(range == null){
        return null
    }
    
    //Check if the list has at list one value that is compatible with the range given
    var rangeEmpty = true
    for(value in list){
        if(value.toLong() in range){
            rangeEmpty = false
        }
    }
    
    if(rangeEmpty){
        return null
    }
    
    //Find the indexes in the given list that have values inside the range
    var low = 0
    while(list[low].toLong() !in range){
        low++
    }

    var high = list.size-1
    while(list[high].toLong() !in range) {
        high--
    }

    return Range(low, high)
}

//Write nanoseconds value in a form that is human readable
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
