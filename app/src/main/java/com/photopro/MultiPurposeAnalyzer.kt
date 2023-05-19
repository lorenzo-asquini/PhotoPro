package com.photopro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class MultiPurposeAnalyzer(activity: AppCompatActivity, rotation: Int) : ImageAnalysis.Analyzer {

    private val preferences : SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    private var imageBitmap : Bitmap? = null
    override fun analyze(image: ImageProxy){
        val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)
        val nightModeValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
        val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

        val isOneFunctionEnabled = poseShootValue == Constant.POSE_SHOOT_ON
                                    || nightModeValue == Constant.NIGHT_MODE_ON
                                    || frameAvgValue == Constant.FRAME_AVG_ON

        //Create only one copy of the image that will be used by everyone
        //TODO: Create this Bitmap once every x ms (when night mode auto or pose shoot are used)
        imageBitmap =
            if(isOneFunctionEnabled){
                image.toBitmap()
            }else{
                null  //Clear old bitmap to free memory
            }

        if(poseShootValue == Constant.POSE_SHOOT_ON){
            //TODO: run function. Create function inside this class and access the imageBitmap directly as class variable
        }

        if(nightModeValue == Constant.NIGHT_MODE_ON){
            //TODO: run function. Create function inside this class and access the imageBitmap directly as class variable
        }
        if(frameAvgValue == Constant.FRAME_AVG_ON){
            //TODO: run function. Create function inside this class and access the imageBitmap directly as class variable
        }

        image.close()
    }

}