package com.photopro

import android.content.SharedPreferences
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

fun drawProModeMenu(activity: AppCompatActivity, preferences: SharedPreferences, show : Boolean){
    //TODO: See if PRO mode is not supported by some devices
    val proModeMenu: LinearLayout = activity.findViewById(R.id.pro_mode_menu)
    val proModeButton: ImageButton = activity.findViewById(R.id.pro_mode_button)

    val proMode = preferences.getInt(SharedPrefs.PRO_MODE_KEY, Constant.PRO_MODE_OFF)

    when(proMode){
        Constant.PRO_MODE_OFF -> {
            proModeMenu.visibility = View.INVISIBLE
            proModeButton.setImageResource(R.drawable.pro)
        }
        Constant.PRO_MODE_ON -> {
            proModeMenu.visibility = View.VISIBLE
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

fun handleProModeCommands(activity: MainActivity) {

    //Add listener to hide and show pro mode sliders
    val hideProModeSwitch: com.google.android.material.switchmaterial.SwitchMaterial =
        activity.findViewById(R.id.hide_pro_mode_switch)
    val proModeSliders: LinearLayout = activity.findViewById(R.id.pro_mode_sliders)
    hideProModeSwitch.setOnCheckedChangeListener { _, isChecked ->

        if (isChecked) {
            proModeSliders.visibility = View.GONE
        } else {
            proModeSliders.visibility = View.VISIBLE
        }
    }

    //Add listener to reset the pro mode values
    val resetProModeButton: ImageButton = activity.findViewById(R.id.reset_pro_mode_button)
    resetProModeButton.setOnClickListener {
        //reset values
    }

    //Listener to White Balance Slider change value
    val whiteBalanceSlider: com.google.android.material.slider.Slider =
        activity.findViewById(R.id.white_balance_slider)
    val whiteBalanceTextView: TextView = activity.findViewById(R.id.white_balance_slider_value)
    whiteBalanceTextView.text = whiteBalanceSlider.value.toInt().toString()
    whiteBalanceSlider.addOnChangeListener { slider, value, fromUser ->
        whiteBalanceTextView.text = value.toInt().toString()
    }

    //Listener to ISO Slider change value
    val isoSlider: com.google.android.material.slider.Slider = activity.findViewById(R.id.iso_slider)
    val isoTextView: TextView = activity.findViewById(R.id.iso_slider_value)
    isoTextView.text = isoSlider.value.toInt().toString()
    isoSlider.addOnChangeListener { slider, value, fromUser ->
        isoTextView.text = value.toInt().toString()
    }

    //Listener to Shutter Speed Slider change value
    val shutterSpeedSlider: com.google.android.material.slider.Slider =
        activity.findViewById(R.id.shutter_speed_slider)
    val shutterSpeedTextView: TextView = activity.findViewById(R.id.shutter_speed_slider_value)
    shutterSpeedTextView.text = shutterSpeedSlider.value.toString()
    shutterSpeedSlider.addOnChangeListener { slider, value, fromUser ->
        shutterSpeedTextView.text = value.toString()
    }
}