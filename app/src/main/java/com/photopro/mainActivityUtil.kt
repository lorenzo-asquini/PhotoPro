package com.photopro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.ImageFormat.YUV_420_888
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.HIGH_RESOLUTION_FLAG_ON
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.core.resolutionselector.ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat


//File with useful functions for MainActivity

fun startCamera(activity: MainActivity, preferences: SharedPreferences) : ImageCapture{
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

        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        var cameraId = getBackCameraId(cameraManager)

        //Change only if not camera back (default value
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraId = getFrontCameraId(cameraManager)
        }

        val nightModeValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
        val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)
        val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

        var imageAnalysis : ImageAnalysis? = null

        if(frameAvgValue == Constant.FRAME_AVG_ON ||
            nightModeValue == Constant.NIGHT_MODE_AUTO ||
            poseShootValue == Constant.POSE_SHOOT_ON){

            //Select the max available size for the analyzer. Not necessary for night mode auto and smart delay, but necessary for frame avg
            //TODO: Better handling of null
            //If at least one camera is present, cameraId cannot be null
            val cameraCharacteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
            val cameraConfigs: StreamConfigurationMap? = cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
            val supportedSizes = cameraConfigs!!.getOutputSizes(YUV_420_888)!!.toList()  //YUV_420_888 is commonly supported in Android

            //Select the size with the maximum resolution. Necessary to make good photos with frame averaging
            var currentMaxSize = supportedSizes[0]
            for (size in supportedSizes) {
                if (size.width * size.height > currentMaxSize.width * currentMaxSize.height) {
                    currentMaxSize = size
                }
            }

            val resStrategy = ResolutionStrategy(currentMaxSize, FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
            val resSelector = ResolutionSelector.Builder()
                .setHighResolutionEnabledFlag(HIGH_RESOLUTION_FLAG_ON)
                .setResolutionStrategy(resStrategy).build()

            //Rotation of the default display
            val currentRotation =
                if(Build.VERSION.SDK_INT >= VERSION_CODES.R){
                    activity.display!!.rotation
                }else{
                    activity.windowManager.defaultDisplay.rotation  //Used for API < 30
                }

            val analyzer = MultiPurposeAnalyzer(activity, currentRotation)

            //This is a UseCase
            //TODO: fix handling of rotation
            imageAnalysis = ImageAnalysis.Builder()
                .setResolutionSelector(resSelector)
                .build().also{
                    it.setAnalyzer(ContextCompat.getMainExecutor(activity), analyzer)
            }
        }

        // Make sure nothing is bound to the cameraProvider,
        // and then bind our cameraSelector and preview object to the cameraProvider.
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera

            val cameraControl =
                if(imageAnalysis == null){
                    cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture).cameraControl
                }else{
                    cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture, imageAnalysis).cameraControl
                }

            if(preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF) == Constant.FLASH_ALWAYS_ON){
                cameraControl.enableTorch(true)
            }else{
                cameraControl.enableTorch(false)
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
        Constant.FLASH_ALWAYS_ON -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF  //Flash always one overrides the imageCapture flash mode

        else -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
    }

    return imageCapture
}
