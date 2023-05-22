package com.photopro

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.*


//File with useful functions for MainActivity

//The savedInstanceState is passed only on the first camera initialization at the beginning of onCreate
fun startCamera(activity: MainActivity, preferences: SharedPreferences, savedInstanceState: Bundle? = null) : Pair<ImageCapture, MultiPurposeAnalyzer?>{
    var imageCapture: ImageCapture? = null

    // This is used to bind the lifecycle of cameras to the lifecycle owner (the main activity).
    // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
    val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)

    // Created outside cameraProviderFuture listener to be able to return the analyzer and activate the frame averaging
    val imageAnalysisCreatorResult = createImageAnalysis(activity, preferences)
    val imageAnalysis : ImageAnalysis? = imageAnalysisCreatorResult.first
    val analyzer : MultiPurposeAnalyzer? = imageAnalysisCreatorResult.second

    cameraProviderFuture.addListener({
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Attach the preview of the camera to the UI widget that will contain that preview
        val cameraPreview : PreviewView = activity.findViewById(R.id.camera_preview)

        val preview = Preview.Builder().build()
            .also {
                it.setSurfaceProvider(cameraPreview.surfaceProvider)
            }

        // Select back camera as a default when starting at first
        var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        var cameraSelected = Constant.CAMERA_BACK

        //Change only if not camera back (default value
        if(preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK) == Constant.CAMERA_FRONT) {
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            cameraSelected = Constant.CAMERA_FRONT
        }

        // Make sure nothing is bound to the cameraProvider,
        // and then bind our cameraSelector and preview object to the cameraProvider.
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera

            val camera =
                if(imageAnalysis == null){
                    cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture)
                }else{
                    cameraProvider.bindToLifecycle(activity, cameraSelector, preview, imageCapture, imageAnalysis)
                }

            if(preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF) == Constant.FLASH_ALWAYS_ON){
                camera.cameraControl.enableTorch(true)
            }else{
                camera.cameraControl.enableTorch(false)
            }

            //If it is the first initialisation of the camera after the creation of the activity
            if(savedInstanceState != null){
                //If the camera remained the same
                if(savedInstanceState.getInt(Constant.ZOOM_VALUE_CAMERA_KEY) == cameraSelected) {
                    //Keep the zoom value. So the zoom is kept when changing from landscape to portrait
                    val zoomValue = savedInstanceState.getFloat(Constant.ZOOM_VALUE_KEY)
                    camera.cameraControl.setZoomRatio(zoomValue)
                }
            }

            activity.camera = camera

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

    return Pair(imageCapture, analyzer)
}

//Variable used to be sure that the circle is set to invisible depending on the last tap
var startTime : Long = 0
@SuppressLint("ClickableViewAccessibility")
fun setPreviewGestures(activity: MainActivity){

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
