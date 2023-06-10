package com.project_photopro

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

data class AvailableFeatures(
    var isBackCameraAvailable : Boolean = false,
    var isFrontCameraAvailable : Boolean = false,

    var isFrontFlashAvailable : Boolean = false,
    var isBackFlashAvailable : Boolean = false,

    var isFrontNightModeAvailable : Boolean = false,
    var isFrontBokehAvailable : Boolean = false,
    var isFrontHDRAvailable : Boolean = false,
    var isFrontFaceRetouchAvailable : Boolean = false,

    var isBackNightModeAvailable : Boolean = false,
    var isBackBokehAvailable : Boolean = false,
    var isBackHDRAvailable : Boolean = false,
    var isBackFaceRetouchAvailable : Boolean = false,

    var isFrontAutoFocusAvailable : Boolean = false,
    var isBackAutoFocusAvailable : Boolean = false
)

fun getAvailableFeatures(activity: AppCompatActivity) : AvailableFeatures{

    val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val frontCameraId = getFrontCameraId(cameraManager)
    val backCameraId = getBackCameraId(cameraManager)

    val features = AvailableFeatures()

    //Used to execute the Runnable used to check if the extensions are available
    val executorService = Executors.newCachedThreadPool()
    //Value used to see if all the necessary features have been considered, for front and back camera
    var camerasConsidered = 0

    if(frontCameraId != null) {
        features.isFrontCameraAvailable = true

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
        features.isFrontFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

        //If the number of maximum AutoFocus regions is greater than 0, AutoFocus can work
        features.isFrontAutoFocusAvailable = (cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! > 0)

        //cameraManager.getCameraExtensionCharacteristics could be used, but it requires higher API levels
        //It becomes uselessly difficult to handle each case. For this reason the extensionManager is used
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)
            extensionsManagerFuture.addListener({

                val extensionsManager = extensionsManagerFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                features.isFrontNightModeAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)
                features.isFrontHDRAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)
                features.isFrontBokehAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)
                features.isFrontFaceRetouchAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)

                camerasConsidered++

            }, executorService)
        }, executorService)
    }else {
        camerasConsidered++
    }

    if(backCameraId != null){
        features.isBackCameraAvailable = true

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId)
        features.isBackFlashAvailable =  cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

        //If the number of maximum AutoFocus regions is greater than 0, AutoFocus can work
        features.isBackAutoFocusAvailable = (cameraCharacteristics.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF)!! > 0)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)
            extensionsManagerFuture.addListener({

                val extensionsManager = extensionsManagerFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                features.isBackNightModeAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)
                features.isBackHDRAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)
                features.isBackBokehAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)
                features.isBackFaceRetouchAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)

                camerasConsidered++

            }, executorService)
        }, executorService)
    }else {
        camerasConsidered++
    }

    //Waits for all the features to be determined (both cameras)
    while(camerasConsidered < 2){
        //This is similar to polling, 50 times a second. The user usually does not perceive this little latency
        executorService.awaitTermination(20, TimeUnit.MILLISECONDS)
    }
    executorService.shutdown()

    return features
}