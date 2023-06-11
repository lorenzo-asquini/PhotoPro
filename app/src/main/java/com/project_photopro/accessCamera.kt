package com.project_photopro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

//File with useful functions for MainActivity

//The savedInstanceState is passed only on the first camera initialization at the beginning of onCreate
fun startCamera(activity: MainActivity, preferences: SharedPreferences, zoomValue: Float = 1.0F, forceNightMode : Boolean = false)
        : Pair<ImageCapture, MultiPurposeAnalyzer?> {
    var imageCapture: ImageCapture? = null

    // This is used to bind the lifecycle of cameras to the lifecycle owner (the main activity).
    // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
    val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

    // Created outside cameraProviderFuture listener to be able to return the analyzer and activate the frame averaging
    val imageAnalysisCreatorResult = createImageAnalysis(activity, preferences)
    val imageAnalysis: ImageAnalysis? = imageAnalysisCreatorResult.first
    val analyzer: MultiPurposeAnalyzer? = imageAnalysisCreatorResult.second

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)

        extensionsManagerFuture.addListener({

            // Attach the preview of the camera to the UI widget that will contain that preview
            val cameraPreview: PreviewView = activity.findViewById(R.id.camera_preview)
            val preview = Preview.Builder().build()
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            // Select back camera as a default when starting at first
            var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            //Get extension values for back camera
            var isHDRSelected = preferences.getInt(SharedPrefs.HDR_BACK_KEY, Constant.HDR_OFF) == Constant.HDR_ON
            var isBokehSelected = preferences.getInt(SharedPrefs.BOKEH_BACK_KEY, Constant.BOKEH_OFF) == Constant.BOKEH_ON
            var isFaceRetouchSelected = preferences.getInt(SharedPrefs.FACE_RETOUCH_BACK_KEY, Constant.FACE_RETOUCH_OFF) == Constant.FACE_RETOUCH_ON

            //Change only if not camera back (default value
            if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
                cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                //Get extension values for front camera
                isHDRSelected = preferences.getInt(SharedPrefs.HDR_FRONT_KEY, Constant.HDR_OFF) == Constant.HDR_ON
                isBokehSelected = preferences.getInt(SharedPrefs.BOKEH_FRONT_KEY, Constant.BOKEH_OFF) == Constant.BOKEH_ON
                isFaceRetouchSelected = preferences.getInt(SharedPrefs.FACE_RETOUCH_FRONT_KEY, Constant.FACE_RETOUCH_OFF)  == Constant.FACE_RETOUCH_ON
            }

            //Only one value for front and back camera
            val isNightModeSelected = forceNightMode ||
                    (preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF) == Constant.NIGHT_MODE_ON)

            //Used to handle extension, if available
            val extensionsManager = extensionsManagerFuture.get()

            // Make sure nothing is bound to the cameraProvider,
            // and then bind our cameraSelector and preview object to the cameraProvider.
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                //SELECT AN EXTENSION IF NEEDED (change the normal cameraSelector)
                if(isNightModeSelected){
                    //Check again if it is really available
                    if(extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)){
                        cameraSelector =
                            extensionsManager.getExtensionEnabledCameraSelector(
                                cameraSelector,
                                ExtensionMode.NIGHT
                            )
                    }
                }else{  //Night mode has priority on the others

                    if(isHDRSelected) {
                        //Check again if it is really available
                        if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)) {
                            cameraSelector =
                                extensionsManager.getExtensionEnabledCameraSelector(
                                    cameraSelector,
                                    ExtensionMode.HDR
                                )
                        }
                    }else if(isBokehSelected){
                        //Check again if it is really available
                        if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)) {
                            cameraSelector =
                                extensionsManager.getExtensionEnabledCameraSelector(
                                    cameraSelector,
                                    ExtensionMode.BOKEH
                                )
                        }
                    }else if(isFaceRetouchSelected){
                        //Check again if it is really available
                        if (extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)) {
                            cameraSelector =
                                extensionsManager.getExtensionEnabledCameraSelector(
                                    cameraSelector,
                                    ExtensionMode.FACE_RETOUCH
                                )
                        }
                    }

                }

                // Bind use cases to camera
                val camera =
                    if (imageAnalysis == null) {
                        cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture)
                    } else {
                        cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture, imageAnalysis)
                    }

                if (preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF) == Constant.FLASH_ALWAYS_ON) {
                    camera.cameraControl.enableTorch(true)
                } else {
                    camera.cameraControl.enableTorch(false)
                }

                //Set zoom
                camera.cameraControl.setZoomRatio(zoomValue)

                activity.camera = camera

            } catch (exc: Exception) {
                Log.e(Constant.TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(activity))
    }, ContextCompat.getMainExecutor(activity))

    //When starting the camera, build the ImageCapture object that will be able to take pictures

    val savedFlashValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)
    imageCapture = ImageCapture.Builder().build()

    when (savedFlashValue) {
        Constant.FLASH_OFF -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
        Constant.FLASH_ON -> imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
        Constant.FLASH_AUTO -> imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
        Constant.FLASH_ALWAYS_ON -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF  //Flash always one overrides the imageCapture flash mode

        else -> imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
    }

    return Pair(imageCapture, analyzer)
}

fun createImageAnalysis(activity: MainActivity, preferences: SharedPreferences)
    : Pair<ImageAnalysis?, MultiPurposeAnalyzer?> {

    val nightModeValue =
        preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
    val smartDelayValue =
        preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)
    val frameAvgValue =
        preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    //Create only if necessary
    if (frameAvgValue == Constant.FRAME_AVG_ON ||
        nightModeValue == Constant.NIGHT_MODE_AUTO ||
        smartDelayValue == Constant.SMART_DELAY_ON
    ) {

        val cameraManager =
            activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraId = getBackCameraId(cameraManager)

        //Change only if not camera back
        if (preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraId = getFrontCameraId(cameraManager)
        }

        //Select the max available size for the analyzer. Not necessary for night mode auto and smart delay, but necessary for frame avg

        //If at least one camera is present, cameraId cannot be null. The other variables should not be null
        val cameraCharacteristics: CameraCharacteristics =
            cameraManager.getCameraCharacteristics(cameraId!!)
        val cameraConfigs: StreamConfigurationMap? =
            cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
        val supportedSizes = cameraConfigs!!.getOutputSizes(ImageFormat.YUV_420_888)!!.toList()  //YUV_420_888 is commonly supported in Android

        //Select the size with the maximum resolution. Necessary to make good photos with frame averaging
        var currentMaxSize = supportedSizes[0]
        for (size in supportedSizes) {
            if (size.width * size.height > currentMaxSize.width * currentMaxSize.height) {
                currentMaxSize = size
            }
        }

        //Rotation of the default display
        @Suppress("DEPRECATION")
        val currentRotation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display!!.rotation
            } else {
                activity.windowManager.defaultDisplay.rotation  //Used for API < 30
            }

        //Change width with height for portrait (currentMaxSize refers to landscape resolution)
        if (currentRotation == Surface.ROTATION_0) {
            currentMaxSize = Size(currentMaxSize.height, currentMaxSize.width)
        }

        //This will use the maximum resolution supported by the analyzer. This may be lower than the maximum resolution of the camera
        val resStrategy = ResolutionStrategy(
            currentMaxSize,
            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
        )
        val resSelector = ResolutionSelector.Builder()
            .setResolutionStrategy(resStrategy).build()

        val analyzer = MultiPurposeAnalyzer(activity, currentRotation)
        analyzer.addListener(activity as SmartDelayListener) //Adding analyzer listener for smart delay

        //This is a UseCase
        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resSelector)
            .build().also {
                it.setAnalyzer(ContextCompat.getMainExecutor(activity), analyzer)
            }

        return Pair(imageAnalysis, analyzer)
    }

    return Pair(null, null)
}