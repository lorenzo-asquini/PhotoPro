package com.photopro

import android.content.SharedPreferences
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat


//File with useful functions for MainActivity

fun startCamera(activity: MainActivity, preferences: SharedPreferences, highQAnalyzer: Boolean = false) : ImageCapture{
    var imageCapture: ImageCapture? = null

    // This is used to bind the lifecycle of cameras to the lifecycle owner (the main activity).
    // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
    val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

    cameraProviderFuture.addListener({
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Attach the preview of the camera to the UI widget that will contain that preview
        val preview = Preview.Builder().build()
            .also {
                val cameraPreview : PreviewView = activity.findViewById(R.id.camera_preview)
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

        // Select back camera as a default when starting at first

        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        //Change only if not camera back (default value
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }

        val nightModeValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
        val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)

        var imageAnalysis : ImageAnalysis? = null
        if(!highQAnalyzer && (nightModeValue == Constant.NIGHT_MODE_AUTO || poseShootValue == Constant.POSE_SHOOT_ON)){
            val lowQualityAnalyzer = LowQualityAnalyzer(activity)

            //This is a UseCase
            //TODO: Set resolution?
            //TODO: handle rotation?
            imageAnalysis = ImageAnalysis.Builder().build().also{
                it.setAnalyzer(ContextCompat.getMainExecutor(activity), lowQualityAnalyzer)
            }
        }

        if(highQAnalyzer){
            val highQualityAnalyzer = HighQualityAnalyzer(activity)

            //This is a UseCase
            //TODO: Set resolution?
            //TODO: handle rotation?
            imageAnalysis = ImageAnalysis.Builder().build().also{
                it.setAnalyzer(ContextCompat.getMainExecutor(activity), highQualityAnalyzer)
            }
        }

        // Make sure nothing is bound to the cameraProvider,
        // and then bind our cameraSelector and preview object to the cameraProvider.
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera

            if(imageAnalysis == null){
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture)
            }else{
                cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture, imageAnalysis)
            }

        } catch(exc: Exception) {
            Log.e(Constant.TAG, "Use case binding failed", exc)
        }

    }, ContextCompat.getMainExecutor(activity))

    //When starting the camera, build the ImageCapture object that will be able to take pictures

    val savedFlashValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)
    imageCapture = ImageCapture.Builder().build()

    when(savedFlashValue){
        Constant.FLASH_OFF -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
        Constant.FLASH_ON -> imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
        Constant.FLASH_AUTO -> imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO

        else -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF  //If something goes wrong
    }

    return imageCapture
}
