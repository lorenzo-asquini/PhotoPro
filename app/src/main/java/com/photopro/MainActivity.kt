package com.photopro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import com.google.android.material.slider.Slider
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

interface MyListener {
    fun onEventCall(analyzer : MultiPurposeAnalyzer)
}

//Implements also MyListener
class MainActivity : AppCompatActivity(), MyListener {
    //Object that becomes not null when (and if) the camera is started
    private var imageCapture: ImageCapture? = null

    //Variable used to start frame averaging when needed
    private var imageAnalyzer : MultiPurposeAnalyzer? = null

    //Timer for smart delay
    private var timer : CountDownTimer? = null

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

    //Initial value of Pro Mode
    private var isProModeOn = false

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
        val smartDelayTimer : TextView = findViewById(R.id.smart_delay_timer)
        smartDelayTimer.visibility = View.INVISIBLE

        smartDelayButton.setOnClickListener{
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

        //Add listener to PRO mode camera button
        val proModeButton : ImageButton = findViewById(R.id.pro_mode_button)
        val proModeComponent: LinearLayout = findViewById(R.id.pro_mode_component)
        //Initially pro mode is disactivated
        proModeComponent.setVisibility(View.INVISIBLE);
        proModeButton.setOnClickListener{
            if(!isProModeOn) {
                proModeComponent.setVisibility(View.VISIBLE);
                proModeButton.setImageResource(R.drawable.normal)
                isProModeOn = true
            }
            else {
                proModeComponent.setVisibility(View.INVISIBLE);
                proModeButton.setImageResource(R.drawable.pro)
                isProModeOn = false
            }
        }

        //Add listener to hide and show pro mode sliders
        val hideProModeSwitch : com.google.android.material.switchmaterial.SwitchMaterial = findViewById(R.id.hide_pro_mode_switch)
        val proModeSliders: LinearLayout = findViewById(R.id.pro_mode_sliders)
        hideProModeSwitch.setOnCheckedChangeListener { _, isChecked ->

            if(isChecked) {
                proModeSliders.setVisibility(View.GONE)
            }
            else{
                proModeSliders.setVisibility(View.VISIBLE)
            }
        }

        //Add listener to reset the pro mode values
        val resetProModeButton : ImageButton = findViewById(R.id.reset_pro_mode_button)
        resetProModeButton.setOnClickListener{
            //reset values
        }

        //Listener to White Balance Slider change value
        val whiteBalanceSlider : com.google.android.material.slider.Slider = findViewById(R.id.white_balance_slider)
        val whiteBalanceTextView : TextView = findViewById(R.id.white_balance_slider_value)
        whiteBalanceTextView.text = whiteBalanceSlider.value.toInt().toString()
        whiteBalanceSlider.addOnChangeListener { slider, value, fromUser ->
            whiteBalanceTextView.text = value.toInt().toString()
        }

        //Listener to ISO Slider change value
        val isoSlider : com.google.android.material.slider.Slider = findViewById(R.id.iso_slider)
        val isoTextView : TextView = findViewById(R.id.iso_slider_value)
        isoTextView.text = isoSlider.value.toInt().toString()
        isoSlider.addOnChangeListener { slider, value, fromUser ->
            isoTextView.text = value.toInt().toString()
        }

        //Listener to Shutter Speed Slider change value
        val shutterSpeedSlider : com.google.android.material.slider.Slider = findViewById(R.id.shutter_speed_slider)
        val shutterSpeedTextView : TextView = findViewById(R.id.shutter_speed_slider_value)
        shutterSpeedTextView.text = shutterSpeedSlider.value.toString()
        shutterSpeedSlider.addOnChangeListener { slider, value, fromUser ->
            shutterSpeedTextView.text = value.toString()
        }


        //Add listener to the camera preview that will allow zoom
        //Zoom is maintained when changing to landscape
        //Add listener to the camera preview that will get the point of the tap and set the focus on that point
        //Focus point is lost when changing to landscape and changing camera
        setPreviewGestures(this, preferences, features)

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

        //Make the person not detected anymore and cancel timer if it was set
        imageAnalyzer?.personDetected = false  //Also removes countdown
        timer?.cancel()

        //Change color to make visible that image averaging is stopped
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setColorFilter(getColor(R.color.white))
    }

    override fun onResume() {
        super.onResume()

        //Necessary to set again always on flash when resuming activity
        when(preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)){
            Constant.FLASH_ALWAYS_ON -> {
                camera?.cameraControl?.enableTorch(true)
            }
            else -> {
                imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
            }
        }
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

    //Manage count down timer for smart delay
    override fun onEventCall(analyzer : MultiPurposeAnalyzer){
        analyzer.personDetected = true

        //Notify the user with a sound if a person was detected
        try {
            //Play the notification only if enabled
            val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)
            if(smartDelayValue == Constant.SMART_DELAY_NOTIFICATION_ON) {
                val notification: Uri =
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val r = RingtoneManager.getRingtone(applicationContext, notification)
                r.play()
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }

        //Show timer text when a person is detected
        val smartDelayTimer : TextView = findViewById(R.id.smart_delay_timer)
        smartDelayTimer.visibility = View.VISIBLE

        val timerSeconds = preferences.getInt(SharedPrefs.SMART_DELAY_SECONDS_KEY, Constant.DEFAULT_SMART_DELAY_SECONDS)
        timer = object: CountDownTimer((timerSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val timeLeft = millisUntilFinished / 1000
                smartDelayTimer.text = timeLeft.toString()
            }

            override fun onFinish() {
                val smartDelayValue = preferences.getInt(SharedPrefs.SMART_DELAY_KEY, Constant.SMART_DELAY_OFF)
                //Only if the smart delay button is still on, take a photo
                if(smartDelayValue == Constant.SMART_DELAY_ON && analyzer.personDetected){
                    Log.d("PoseDetection: ", "Photo capturing")
                    takePhoto()
                    analyzer.personDetected = false
                }

                //Hide timer at the end of the countdown
                smartDelayTimer.visibility = View.INVISIBLE
            }
        }.start()
        Log.d("PoseDetection: ", "Photo taken")
    }
}
