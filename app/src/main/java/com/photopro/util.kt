package com.photopro

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.HIGH_RESOLUTION_FLAG_ON
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

//Functions useful for both normal camera and PRO camera

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
){}

fun cameraPermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun vibratePhone(activity: AppCompatActivity, duration: Long){
    val vib =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //TODO: How to fix?
            val vibratorManager = activity.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            activity.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vib.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE) )
    }else{
        @Suppress("DEPRECATION")
        vib.vibrate(duration)
    }
}

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

fun getSaveImageContentValues() : ContentValues {
    // Create time stamped name using the FILENAME_FORMAT defined inside the companion object
    // This allows the MediaStore to be unique
    val name = SimpleDateFormat(Constant.FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

    //Create ContentValue to hold data about the image saving.
    return ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoPro")
        }
    }
}

fun createImageAnalysis(activity: AppCompatActivity, preferences: SharedPreferences) : Pair<ImageAnalysis?, MultiPurposeAnalyzer?>{

    val nightModeValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
    val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)
    val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    if(frameAvgValue == Constant.FRAME_AVG_ON ||
        nightModeValue == Constant.NIGHT_MODE_AUTO ||
        poseShootValue == Constant.POSE_SHOOT_ON){

        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraId = getBackCameraId(cameraManager)

        //Change only if not camera back
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraId = getFrontCameraId(cameraManager)
        }

        //Select the max available size for the analyzer. Not necessary for night mode auto and smart delay, but necessary for frame avg
        //TODO: Better handling of null
        //If at least one camera is present, cameraId cannot be null
        val cameraCharacteristics: CameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId!!)
        val cameraConfigs: StreamConfigurationMap? = cameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
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
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                activity.display!!.rotation
            }else{
                activity.windowManager.defaultDisplay.rotation  //Used for API < 30
            }

        //Change width with height for portrait (currentMaxSize refers to landscape resolution)
        if(currentRotation == Surface.ROTATION_0){
            currentMaxSize = Size(currentMaxSize.height, currentMaxSize.width)
        }

        //This will use the maximum resolution supported by the analyzer. This may be lower than the maximum resolution of the camera
        val resStrategy = ResolutionStrategy(currentMaxSize, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER)
        val resSelector = ResolutionSelector.Builder()
            .setHighResolutionEnabledFlag(HIGH_RESOLUTION_FLAG_ON)
            .setResolutionStrategy(resStrategy).build()

        val analyzer = MultiPurposeAnalyzer(activity, currentRotation)

        //This is a UseCase
        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(resSelector)
            .build().also{
                it.setAnalyzer(ContextCompat.getMainExecutor(activity), analyzer)
            }

        return Pair(imageAnalysis, analyzer)
    }

    return Pair(null, null)
}