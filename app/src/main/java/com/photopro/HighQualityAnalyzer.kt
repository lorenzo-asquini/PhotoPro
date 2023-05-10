package com.photopro

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class HighQualityAnalyzer(activity: AppCompatActivity) : ImageAnalysis.Analyzer {

    private val preferences : SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)
    override fun analyze(image: ImageProxy){
        if(preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF) == Constant.FRAME_AVG_ON){
            //TODO: run function
        }
    }

}