package com.photopro

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class LowQualityAnalyzer(activity: AppCompatActivity) : ImageAnalysis.Analyzer  {

    private val preferences : SharedPreferences = activity.getPreferences(MODE_PRIVATE)
    override fun analyze(image: ImageProxy){
        if(preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF) == Constant.POSE_SHOOT_ON){
            //TODO: run function
        }

        if(preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF) == Constant.NIGHT_MODE_ON){
            //TODO: run function
        }
    }
}