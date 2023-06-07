package com.photopro

import android.Manifest
import android.annotation.SuppressLint
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
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.util.Size
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionSelector.HIGH_RESOLUTION_FLAG_ON
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

//Functions useful for both normal camera and PRO camera

fun cameraPermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

//The constant appears to not be wrong according to documentation
@SuppressLint("WrongConstant")
fun vibratePhone(activity: AppCompatActivity, duration: Long){
    val vib =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            //Gives error (suppressed) while it is correct as for:
            //https://developer.android.com/reference/android/content/Context#VIBRATOR_MANAGER_SERVICE
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
    val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)
    val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

    if(frameAvgValue == Constant.FRAME_AVG_ON ||
        nightModeValue == Constant.NIGHT_MODE_AUTO ||
        smartDelayValue == Constant.SMART_DELAY_ON){

        val cameraManager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        var cameraId = getBackCameraId(cameraManager)

        //Change only if not camera back
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraId = getFrontCameraId(cameraManager)
        }

        //Select the max available size for the analyzer. Not necessary for night mode auto and smart delay, but necessary for frame avg

        //If at least one camera is present, cameraId cannot be null. The other variables should not be null
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
        analyzer.addListener(activity as MyListener) //Adding analyzer listener for smart delay

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

//Variable used to be sure that the circle is set to invisible depending on the last tap
var startTime : Long = 0
@SuppressLint("ClickableViewAccessibility")
fun setPreviewGestures(activity: CameraAppCompactActivity){

    // Listen to pinch gestures
    val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Get the camera's current zoom ratio
            val currentZoomRatio = activity.camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0F

            // Get the pinch gesture's scaling factor
            val delta = detector.scaleFactor

            // Update the camera's zoom ratio
            activity.camera?.cameraControl?.setZoomRatio(currentZoomRatio * delta)

            // Return true, as the event was handled
            return true
        }
    }
    val scaleGestureDetector = ScaleGestureDetector(activity, listener)

    // Attach the pinch gesture listener to the viewfinder
    val cameraPreview : PreviewView = activity.findViewById(R.id.camera_preview)

    cameraPreview.setOnTouchListener { _, motionEvent: MotionEvent ->

        //Handle zoom
        scaleGestureDetector.onTouchEvent(motionEvent)

        //Handle tap to focus
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                return@setOnTouchListener true
            }

            MotionEvent.ACTION_UP -> {
                // Get the MeteringPointFactory from PreviewView
                val factory = cameraPreview.meteringPointFactory

                // Create a MeteringPoint from the tap coordinates
                val point = factory.createPoint(motionEvent.x, motionEvent.y)

                //Show focus circle where the user tapped
                val focusCircle: ImageView = activity.findViewById(R.id.tapToFocus_circle)

                focusCircle.x = motionEvent.x - focusCircle.width/2
                focusCircle.y = motionEvent.y - focusCircle.height/2

                focusCircle.visibility = View.VISIBLE
                val timeTillInvisible : Long = 2000

                //Reset the time of the last tap
                startTime = Calendar.getInstance().timeInMillis

                //Make the circle disappear after a few seconds
                Handler(Looper.getMainLooper()).postDelayed({
                    //If the delayed action is referring to the last tap
                    if(Calendar.getInstance().timeInMillis - startTime >= timeTillInvisible) {
                        focusCircle.visibility = View.INVISIBLE
                    }
                }, timeTillInvisible)

                // Create a MeteringAction from the MeteringPoint
                // All actions are performed: AF(Auto Focus), AE(Auto Exposure) and AWB(Auto White Balance)
                val action = FocusMeteringAction.Builder(point).build()

                // Trigger the focus and metering
                activity.camera?.cameraControl?.startFocusAndMetering(action)

                return@setOnTouchListener true
            }
            else -> return@setOnTouchListener false
        }
    }
}
