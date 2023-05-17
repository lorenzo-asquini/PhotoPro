package com.photopro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat

//Functions useful for both normal camera and PRO camera

data class AvailableFeatures(val dummy : Int){
    var isBackCameraAvailable = false
    var isFrontCameraAvailable = false

    var isFrontFlashAvailable = false
    var isBackFlashAvailable = false

    var isFrontNightModeAvailable = false
    var isFrontBokehAvailable = false
    var isFrontHDRAvailable = false
    var isFrontFaceRetouchAvailable = false

    var isBackNightModeAvailable = false
    var isBackBokehAvailable = false
    var isBackHDRAvailable = false
    var isBackFaceRetouchAvailable = false
}

fun cameraPermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun getBackCameraId(cameraManager: CameraManager) : String?{
    var backCameraId : String? = null

    for(cameraId in cameraManager.cameraIdList) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val cameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

        if (cameraFacing == CameraMetadata.LENS_FACING_BACK) {
            backCameraId = cameraId
            break
        }
    }

    return backCameraId
}

fun getFrontCameraId(cameraManager: CameraManager) : String?{
    var frontCameraId : String? = null

    for(cameraId in cameraManager.cameraIdList) {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
        val cameraFacing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)

        if (cameraFacing == CameraMetadata.LENS_FACING_FRONT) {
            frontCameraId = cameraId
            break
        }
    }

    return frontCameraId
}

fun getAvailableFeatures(activity: AppCompatActivity, cameraManager: CameraManager) : AvailableFeatures{
    val frontCameraId = getFrontCameraId(cameraManager)
    val backCameraId = getBackCameraId(cameraManager)

    val availableFeatures = AvailableFeatures(1)

    if(frontCameraId != null){
        availableFeatures.isFrontCameraAvailable = true

        val cameraCharacteristics = cameraManager.getCameraCharacteristics(frontCameraId)
        availableFeatures.isFrontFlashAvailable =  cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)!!

        //cameraManager.getCameraExtensionCharacteristics could be used, but it requires higher API levels
        //It becomes uselessly difficult to handle each case. For this reason the extensionManager is used
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val extensionsManagerFuture = ExtensionsManager.getInstanceAsync(activity, cameraProvider)
            extensionsManagerFuture.addListener({

                val extensionsManager = extensionsManagerFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                availableFeatures.isFrontNightModeAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.NIGHT)
                availableFeatures.isFrontHDRAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.HDR)
                availableFeatures.isFrontBokehAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.BOKEH)
                availableFeatures.isFrontFaceRetouchAvailable =  extensionsManager.isExtensionAvailable(cameraSelector, ExtensionMode.FACE_RETOUCH)

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