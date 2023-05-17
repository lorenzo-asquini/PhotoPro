package com.photopro

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/*TODO:
*  -See if analyzer has right orientation
*  -Set up base of analyzer
*  -See if everything works*/
class MainActivity : AppCompatActivity() {
    //Object that becomes not null when (and if) the camera is started
    private var imageCapture: ImageCapture? = null

    //Using lateinit makes it possible to initialize later a variable (inside onCreate)
    //Create a cameraExecutor to use the camera
    private lateinit var cameraExecutor: ExecutorService

    //Necessary lateinit because the SharedPreferences need the activity to be created
    private lateinit var preferences : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        preferences = getPreferences(MODE_PRIVATE)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val features = getAvailableFeatures(this, cameraManager)

        //Draw from preferences
        drawAllButtons(this, preferences, features)

        // Request camera permissions if not already granted
        if (cameraPermissionGranted(this)) {
            imageCapture = startCamera(this, preferences)  //Start camera if permission already granted
        } else {
            //Ask for CAMERA permission
            //The actions to perform when permission request result arrive are described inside onRequestPermissionsResult (below)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), Constant.REQUEST_CODE_PERMISSIONS)
        }

        //Add listener to button to open the options menu
        val optionsButton: ImageButton = findViewById(R.id.options_button)
        optionsButton.setOnClickListener{
            val openSettingsIntent = Intent(this, OptionsActivity::class.java)
            startActivity(openSettingsIntent)
        }

        //Add listener to button to change flash mode
        val flashButton : ImageButton = findViewById(R.id.flash_button)
        flashButton.setOnClickListener{
            changeFlashValue(preferences)
            drawFlashButton(this, preferences, true)

            //No need to create new imageCapture. Change the flash mode in imageCapture
            val savedFlashValue = preferences.getInt(SharedPrefs.FLASH_KEY, Constant.FLASH_OFF)

            //To switch to and from always on flash it is necessary to start th camera
            when(savedFlashValue){
                Constant.FLASH_OFF -> {
                    imageCapture = startCamera(this, preferences)
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
                }

                Constant.FLASH_ON -> imageCapture!!.flashMode = ImageCapture.FLASH_MODE_ON

                Constant.FLASH_AUTO -> imageCapture!!.flashMode = ImageCapture.FLASH_MODE_AUTO

                Constant.FLASH_ALWAYS_ON -> imageCapture = startCamera(this, preferences)

                else -> {
                    imageCapture = startCamera(this, preferences)
                    imageCapture!!.flashMode = ImageCapture.FLASH_MODE_OFF
                }  //If something goes wrong
            }
        }

        //Add listener to button to change frame average mode
        val frameAvgButton: ImageButton = findViewById(R.id.frame_avg_button)
        frameAvgButton.setOnClickListener{
            changeFrameAvgValue(preferences)
            drawFrameAvgButton(this, preferences, true)
            //No need to restart camera. It has effect only when taking a picture
        }

        //Add listener to button to change pose shoot mode
        val poseShootButton: ImageButton = findViewById(R.id.pose_shoot_button)
        poseShootButton.setOnClickListener{
            changePoseShootValue(preferences)
            drawPoseShootButton(this, preferences, true)
            imageCapture = startCamera(this,preferences)  //Start camera to start analyzer
        }

        //Add listener to button to change night mode mode
        val nightModeButton: ImageButton = findViewById(R.id.night_mode_button)
        nightModeButton.setOnClickListener{
            changeNightModeValue(preferences)
            drawNightModeButton(this, preferences, true)
            imageCapture = startCamera(this,preferences)
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
            imageCapture = startCamera(this,preferences)
            drawAllButtons(this, preferences, features)  //When changing camera the available features change
        }

        //Create a single thread for processing camera data
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto() {
        if(preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF) == Constant.FRAME_AVG_ON){
            //TODO: find a way to retrieve the image from analyzer and save it
        }

        // Get a stable reference of the modifiable image capture use case
        //If the camera did not start successfully, imageCapture is still null
        val imageCapture = imageCapture ?: return

        // Create time stamped name using the FILENAME_FORMAT defined inside the companion object
        // This allows the MediaStore to be unique
        val name = SimpleDateFormat(Constant.FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        //Create MediaStore to store the image. Specify the path of the saved image
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhotoPro")
            }
        }

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
                override fun onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    //TODO: Toast only for debug
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(Constant.TAG, msg)
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
                imageCapture = startCamera(this, preferences)  //Start camera if camera permission is granted
            } else {
                //Show a message that explains why the app does not work (camera permission not granted) and exit the app
                Toast.makeText(this, "Permissions for the camera granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
