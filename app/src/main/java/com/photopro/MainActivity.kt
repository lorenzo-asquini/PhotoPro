package com.photopro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

//TODO: Tap to focus not working in front camera

interface MyListener {
    fun onEventCall(obj : MultiPurposeAnalyzer)
}

//Implements also MyListener
class MainActivity : CameraAppCompactActivity(),MyListener {
    //Object that becomes not null when (and if) the camera is started
    private var imageCapture: ImageCapture? = null

    //Variable used to start frame averaging when needed
    private var imageAnalyzer : MultiPurposeAnalyzer? = null

    //Public variable set when the camera is initialised.
    //Not returned because the initialisation may happen after the startCamera has finished
    override var camera: Camera? = null

    //Using lateinit makes it possible to initialize later a variable (inside onCreate)
    //Create a cameraExecutor to use the camera
    private lateinit var cameraExecutor: ExecutorService

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences

    //Avoid opening the options menu multiple times when spamming button
    private var isOptionsButtonClicked = false

    //Timer display and toggle check for smart delay
    private lateinit var tv_timer : TextView
    private var toggle : Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences(SharedPrefs.SHARED_PREFERENCES_KEY, MODE_PRIVATE)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val features = getAvailableFeatures(this, cameraManager)

        //Draw from preferences
        drawAllButtons(this, preferences, features)

        // Request camera permissions if not already granted
        if (cameraPermissionGranted(this)) {
            //Keep zoom value
            val startCameraResult = startCamera(this, preferences, savedInstanceState)  //Start camera if permission already granted
            imageCapture = startCameraResult.first
            imageAnalyzer = startCameraResult.second
        } else {
            //Ask for CAMERA permission
            //The actions to perform when permission request result arrive are described inside onRequestPermissionsResult (below)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Constant.REQUEST_CODE_PERMISSIONS)
        }

        /*ADD LISTENERS TO BUTTONS*/
        //TODO: Move listeners to cameraButtonsUtil because used both by the PRO mode and the normal mode?

        //Add listener to button to open the options menu
        val optionsButton: ImageButton = findViewById(R.id.options_button)
        optionsButton.setOnClickListener{
            if(!isOptionsButtonClicked) {
                val openSettingsIntent = Intent(this, OptionsActivity::class.java)
                startActivity(openSettingsIntent)
                isOptionsButtonClicked = true
            }
        }

        //Add listener to button to change flash mode
        val flashButton : ImageButton = findViewById(R.id.flash_button)
        flashButton.setOnClickListener{
            changeFlashValue(preferences)
            drawFlashButton(this, preferences, true)

            //No need to create new imageCapture. Change the flash mode in imageCapture
            val savedFlashValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

            //To switch to and from always on flash it is necessary to start th camera
            //If cameraControl is not defined, that means that the camera has not started,
            //so the correct status of the torch will be set when the initialisation will be finished
            when(savedFlashValue){
                Constant.FLASH_OFF -> {
                    camera?.cameraControl?.enableTorch(false)
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
                }

                Constant.FLASH_ON -> {
                    camera?.cameraControl?.enableTorch(false)
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_ON
                }

                Constant.FLASH_AUTO -> {
                    camera?.cameraControl?.enableTorch(false)
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_AUTO
                }

                Constant.FLASH_ALWAYS_ON -> {
                    camera?.cameraControl?.enableTorch(true)
                }

                else -> {
                    val result = startCamera(this, preferences)
                    imageCapture = result.first
                    imageAnalyzer = result.second
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
                }  //If something goes wrong
            }
        }

        //Add listener to button to change frame average mode
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setOnClickListener{
                changeFrameAvgValue(preferences)
                drawFrameAvgButton(this, preferences, true)
                val startCameraResult = startCamera(this, preferences)
                imageCapture = startCameraResult.first
                imageAnalyzer = startCameraResult.second
        }

        //Add listener to button to change smart delay mode
        val smartDelayButton: ImageButton = findViewById(R.id.smart_delay_button)
        tv_timer = findViewById(R.id.tv_timer)
        tv_timer.visibility = View.INVISIBLE
        smartDelayButton.setOnClickListener{
            tv_timer.text = "00"
            toggle = !toggle
            if(toggle) {
                tv_timer.visibility = View.VISIBLE
            }else {
                tv_timer.visibility = View.INVISIBLE
            }
            changeSmartDelayValue(preferences)
            drawSmartDelayButton(this, preferences, true)
            val startCameraResult = startCamera(this,preferences)  //Start camera to start analyzer
            imageCapture = startCameraResult.first
            imageAnalyzer = startCameraResult.second
        }

        //Add listener to button to change night mode mode
        val nightModeButton: ImageButton = findViewById(R.id.night_mode_button)
        nightModeButton.setOnClickListener{
            changeNightModeValue(preferences)
            drawNightModeButton(this, preferences, true)
            val startCameraResult = startCamera(this,preferences)
            imageCapture = startCameraResult.first
            imageAnalyzer = startCameraResult.second
        }

        //Add listener to button to make it take photos
        val takePhotoButton : Button = findViewById(R.id.image_capture_button)
        takePhotoButton.setOnClickListener{
            takePhoto()
        }

        //Add listener to button to open gallery
        val openGalleryButton : ImageButton = findViewById(R.id.gallery_button)
        openGalleryButton.setOnClickListener{
            openGallery(this)
        }

        //Handle switch camera button
        //Display button only if both front and back camera are available
        initialiseChangeCameraButton(this, features, preferences)
        val changeCameraButton : ImageButton = findViewById(R.id.change_camera_button)

        //If button is present and pressed, then both cameras are available
        changeCameraButton.setOnClickListener{
            changeCameraFacingValue(preferences)
            val startCameraResult = startCamera(this,preferences)
            imageCapture = startCameraResult.first
            imageAnalyzer = startCameraResult.second
            drawAllButtons(this, preferences, features)  //When changing camera the available features change
        }

        //Add listener to the camera preview that will allow zoom
        //Zoom is maintained when changing to landscape
        //Add listener to the camera preview that will get the point of the tap and set the focus on that point
        //Focus point is lost when changing to landscape and changing camera
        setPreviewGestures(this)

        //Create a single thread for processing camera data
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {

        //Vibrate when photo is taken
        vibratePhone(this, 100)

        if(preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF) == Constant.FRAME_AVG_ON){
            //Check to see if a frame average is happening right now. Do not start a new one
            if(imageAnalyzer!!.framesAveraged == -1) {
                imageAnalyzer!!.startFrameAvg()  //The image will be saved by the analyzer
            }
            return
        }

        // Get a stable reference of the modifiable image capture use case
        //If the camera did not start successfully, imageCapture is still null
        val imageCapture = imageCapture ?: return

        val contentValues = getSaveImageContentValues()

        // Create output options object which contains file + metadata
        // This object is where it is possible to specify things about how the output should be
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(Constant.TAG, "Photo saved")
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e(Constant.TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    //What to do when the permission request result are available
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constant.REQUEST_CODE_PERMISSIONS) {
            if (cameraPermissionGranted(this)) {
                val startCameraResult = startCamera(this, preferences)  //Start camera if camera permission is granted
                imageCapture = startCameraResult.first
                imageAnalyzer = startCameraResult.second
            } else {
                //Show a message that explains why the app does not work (camera permission not granted) and exit the app
                Toast.makeText(this, "Permissions for the camera granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        //Reset the value when pausing the current activity
        isOptionsButtonClicked = false

        //Stop frame averaging if activity is stopped (if analyzer was initialised)
        imageAnalyzer?.framesAveraged = -1

        //Change color to make visible that image averaging is stopped
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setColorFilter(getColor(R.color.white))
    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 0F
        outState.putFloat(Constant.ZOOM_VALUE_KEY, currentZoomRatio)

        val currentCamera = preferences.getInt(SharedPrefs.CAMERA_FACING_KEY, Constant.CAMERA_BACK)
        outState.putInt(Constant.ZOOM_VALUE_CAMERA_KEY, currentCamera)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    //Method used for manage count down timer for smart delay
    override fun onEventCall(smartAnalyzer : MultiPurposeAnalyzer){
        smartAnalyzer.setDetected(true)
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(applicationContext, notification)
            r.play()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        val timer = object: CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timeleft = millisUntilFinished / 1000
                tv_timer.text = timeleft.toString()
            }

            override fun onFinish() {
                tv_timer.text = "00"
                if(toggle){ //Only if the smart delay button is on
                    Log.d("PoseDetection: ", "Photo capturing")
                    takePhoto()
                    smartAnalyzer.setDetected(false)
                }
            }
        }.start()
        Log.d("PoseDetection: ", "Photo taken")
    }
}
