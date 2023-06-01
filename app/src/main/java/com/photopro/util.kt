package com.photopro

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

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
    var isBackFaceRetouchAvailable : Boolean = false
)

fun getAvailableFeatures(activity: AppCompatActivity, cameraManager: CameraManager) : AvailableFeatures{
    val frontCameraId = getFrontCameraId(cameraManager)
    val backCameraId = getBackCameraId(cameraManager)

    val availableFeatures = AvailableFeatures()

    if(frontCameraId != null) {
        availableFeatures.isFrontCameraAvailable = true

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
        availableFeatures.isFrontFlashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

        //cameraManager.getCameraExtensionCharacteristics could be used, but it requires higher API levels
        //It becomes uselessly difficult to handle each case. For this reason the extensionManager is used
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)
            extensionsManagerFuture.addListener({

                val extensionsManager = extensionsManagerFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                availableFeatures.isFrontNightModeAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)
                availableFeatures.isFrontHDRAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)
                availableFeatures.isFrontBokehAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)
                availableFeatures.isFrontFaceRetouchAvailable = extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)

            }, ContextCompat.getMainExecutor(activity))
        }, ContextCompat.getMainExecutor(activity))
    }

    if(backCameraId != null){
        availableFeatures.isBackCameraAvailable = true

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(backCameraId)
        availableFeatures.isBackFlashAvailable =  cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)
            extensionsManagerFuture.addListener({

                val extensionsManager = extensionsManagerFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                availableFeatures.isBackNightModeAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)
                availableFeatures.isBackHDRAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)
                availableFeatures.isBackBokehAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)
                availableFeatures.isBackFaceRetouchAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)

            }, ContextCompat.getMainExecutor(activity))
        }, ContextCompat.getMainExecutor(activity))
    }

    return availableFeatures
}