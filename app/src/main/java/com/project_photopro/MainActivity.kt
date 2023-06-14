package com.project_photopro

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


//Implements also SmartDelayListener
class MainActivity : AppCompatActivity(), SmartDelayListener{
    //Object that becomes not null when (and if) the camera is started
    var imageCapture: ImageCapture? = null

    //Variable used to communicate with the analyzer if necessary
    private var imageAnalyzer : MultiPurposeAnalyzer? = null

    //Public variable set when the camera is initialised.
    //Not returned because the initialisation may happen after the startCamera has finished
    var camera: Camera? = null

    //Using lateinit makes it possible to initialize later a variable (inside onCreate)
    //Create a cameraExecutor to use the camera
    private lateinit var cameraExecutor: ExecutorService

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences

    //Avoid opening the options menu multiple times when spamming button
    private var isOptionsButtonClicked = false

    //Timer for smart delay
    private var smartDelayTimer : CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        preferences = getSharedPreferences(SharedPrefs.SHARED_PREFERENCES_KEY, MODE_PRIVATE)

        //Program waits until all the features have been determined
        val features = getAvailableFeatures(this)

        //Draw from preferences
        //If features are not available, the buttons are not present
        drawAllButtons(this, preferences, features)

        // Request camera permissions if not already granted
        if (cameraPermissionGranted(this)) {
            //Keep zoom value if it was saved
            val zoomValue= intent.getFloatExtra(Constant.ZOOM_VALUE_KEY, 1.0F)
            startCameraWrapper(zoomValue)  //Force saved zoom
        } else {
            //Ask for CAMERA permission
            //The actions to perform when permission request result arrive are described inside onRequestPermissionsResult (below)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Constant.REQUEST_CODE_PERMISSIONS)
        }

        /*ADD LISTENERS TO BUTTONS*/

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

            setTorchState(this, preferences)
        }

        //Add listener to button to change frame average mode
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setOnClickListener{
            changeFrameAvgValue(preferences)
            drawFrameAvgButton(this, preferences, true)
            startCameraWrapper()  //Start camera to start analyzer
        }

        //Add listener to button to change smart delay mode
        val smartDelayButton: ImageButton = findViewById(R.id.smart_delay_button)
        val smartDelayTimerText : TextView = findViewById(R.id.smart_delay_timer)
        smartDelayTimerText.visibility = View.INVISIBLE  //Timer always invisible when button clicked

        smartDelayButton.setOnClickListener{
            changeSmartDelayValue(preferences)
            drawSmartDelayButton(this, preferences, true)
            startCameraWrapper()  //Start camera to start analyzer

            //Stop in any case
            smartDelayTimer?.cancel()
        }

        //Add listener to button to change night mode mode
        val nightModeButton: ImageButton = findViewById(R.id.night_mode_button)
        nightModeButton.setOnClickListener{
            changeNightModeValue(preferences)
            drawNightModeButton(this, preferences, true)
            startCameraWrapper()  //Start camera to start analyzer
        }

        //Add listener to button to change to pro mode and back
        val proModeButton: ImageButton = findViewById(R.id.pro_mode_button)
        proModeButton.setOnClickListener {
            changeProModeValue(preferences)
            drawProModeMenu(this, preferences, true)  //Also the sliders are drawn
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
            startCameraWrapper(1.0F)  //Force zoom reset
            drawAllButtons(this, preferences, features)  //When changing camera the available features change
        }

        //Add listener to the camera preview that will allow zoom
        //Zoom is maintained when changing to landscape
        //Add listener to the camera preview that will get the point of the tap and set the focus on that point
        //Focus point is lost when changing to landscape and/or changing camera
        setPreviewGestures(this, preferences, features)

        //Create a single thread for processing camera data
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    //Wrapper for startCamera, simplifies function call
    fun startCameraWrapper(zoomValue : Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0F, forceNightMode : Boolean = false){
        //Delete old camera to reset Pro settings
        camera = null
        //Restart the camera with current zoom value if not told otherwise (and if the value is available)
        imageAnalyzer = startCamera(this, preferences, zoomValue, forceNightMode)  //Start camera if permission already granted
    }

    private fun takePhoto() {

        //Change color of the button while taking a picture (also while using frame average)
        val imageCaptureButton : Button = findViewById(R.id.image_capture_button)
        imageCaptureButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.lightBlue))

        //Vibrate when photo is taken
        vibratePhone(this, 100)

        //If a photo is taken using frame averaging, it is handled by the analyzer
        if(preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF) == Constant.FRAME_AVG_ON){
            //Check to see if a frame average is happening right now. Do not start a new one
            if(imageAnalyzer!!.framesAveraged == -1) {
                imageAnalyzer!!.startFrameAvg()  //The image will be saved by the analyzer
            }
            return
        }

        // Get a stable reference of the modifiable image capture use case
        // If the camera did not start successfully, imageCapture is still null
        val imageCapture = imageCapture ?: return

        val contentValues = getSaveImageContentValues()

        // Create output options object which contains file + metadata
        // This object is where it is possible to specify things about how the output should be
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Log.i(Constant.TAG, "Photo saved")

                    //Reset take picture button color
                    imageCaptureButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
                }
                override fun onError(exc: ImageCaptureException) {
                    Log.e(Constant.TAG, "Photo capture failed: ${exc.message}", exc)

                    //Reset take picture button color
                    imageCaptureButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))
                }
            }
        )
    }

    //What to do when the permission request result are available
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("aa", "aaa")
        if (requestCode == Constant.REQUEST_CODE_PERMISSIONS) {
            if (cameraPermissionGranted(this)) {
                startCameraWrapper()
            } else {
                //Show a message that explains why the app does not work (camera permission not granted) and exit the app
                Toast.makeText(this, "Permissions for the camera not granted", Toast.LENGTH_SHORT).show()

                //Continue to ask for permission everytime the app is opened if not granted
                //Necessary to delete the data (and close the app) to make it ask every time
                //Every data associated with the app is cancelled, but not the photos
                (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        //Reset the value when pausing the current activity
        isOptionsButtonClicked = false

        //Stop frame averaging if activity is stopped (if analyzer was initialised)
        imageAnalyzer?.framesAveraged = -1
        //Reset torch state in any case if it was changed while frame averaging
        setTorchState(this, preferences)

        //Make the person not detected anymore and cancel timer if it was set
        imageAnalyzer?.personDetected = false  //Also removes countdown
        smartDelayTimer?.cancel()

        //Change color to make visible that image averaging is stopped
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setColorFilter(getColor(R.color.white))

        val imageCaptureButton : Button = findViewById(R.id.image_capture_button)
        imageCaptureButton.backgroundTintList = ColorStateList.valueOf(getColor(R.color.white))

        //Save current zoom value, used when maintaining the same camera but rotating the phone
        //Not used onSavedInstanceState because the value is needed both in onCreate and in onResume
        val currentZoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0F
        intent.putExtra(Constant.ZOOM_VALUE_KEY,currentZoomRatio)
    }

    override fun onResume() {
        super.onResume()

        //Necessary to set again always on flash when resuming activity
        when(preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)){
            Constant.FLASH_ALWAYS_ON -> {
                camera?.cameraControl?.enableTorch(true)
            }
            else -> {
                imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
            }
        }

        //Keep zoom value if it was saved
        val zoomValue= intent.getFloatExtra(Constant.ZOOM_VALUE_KEY, 1.0F)
        //Necessary to restart camera in case a new extension was selected in the options menu
        startCameraWrapper(zoomValue)  //Force saved zoom
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    //Manage count down timer for smart delay
    override fun onPersonDetected(analyzer : MultiPurposeAnalyzer){
        analyzer.personDetected = true

        //Notify the user with a sound if a person was detected
        try {
            //Play the notification only if enabled
            val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_NOTIFICATION_KEY, Constant.SMART_DELAY_NOTIFICATION_ON)
            if(smartDelayValue == Constant.SMART_DELAY_NOTIFICATION_ON) {
                //Generate a tone from the default ones present inside the system
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                //For list of possible tones, see https://developer.android.com/reference/android/media/ToneGenerator
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_ABBR_ALERT, 500)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        //Show timer text when a person is detected
        val smartDelayTimer : TextView = findViewById(R.id.smart_delay_timer)
        smartDelayTimer.visibility = View.VISIBLE

        val timerSeconds = preferences.getInt(SharedPrefs.SMART_DELAY_SECONDS_KEY, Constant.DEFAULT_SMART_DELAY_SECONDS)
        this.smartDelayTimer = object: CountDownTimer((timerSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = (millisUntilFinished / 1000) + 1 //Seconds
                smartDelayTimer.text = timeLeft.toString()
            }

            override fun onFinish() {
                val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)
                //Only if the smart delay button is still on, take a photo
                if(smartDelayValue == Constant.SMART_DELAY_ON && analyzer.personDetected){
                    Log.d("PoseDetection: ", "Photo capturing")
                    takePhoto()
                    analyzer.personDetected = false
                    analyzer.smartDelayLastPhotoTaken = Calendar.getInstance().timeInMillis
                }

                //Hide timer at the end of the countdown
                smartDelayTimer.visibility = View.INVISIBLE
            }
        }.start()
        Log.d("PoseDetection: ", "Person detected, taking picture")
    }
}
