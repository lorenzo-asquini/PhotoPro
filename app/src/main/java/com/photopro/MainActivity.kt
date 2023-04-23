package com.photopro

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.Toast
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import android.util.Log
import android.widget.Button
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.PreviewView
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    //Object that becomes not null when (and if) the camera is started
    private var imageCapture: ImageCapture? = null

    //Using lateinit makes it possible to initialize later a variable (inside onCreate)
    //Create a cameraExecutor to use the camera
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Request camera permissions if not already granted
        if (allPermissionsGranted()) {
            startCamera()  //Start camera if permission already granted
        } else {
            //Ask for all permissions inside REQUIRED_PERMISSIONS (declared inside the companion object)
            //Actions to perform when permission request result arrive is onRequestPermissionsResult (below)
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        //Add listener to button to make it take photos
        val takePhotoButton : Button = findViewById(R.id.image_capture_button)
        takePhotoButton.setOnClickListener{
            takePhoto()
        }

        //Create a single thread for processing camera data
        //TODO: create multiple threads for processing data while being displayed?
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun startCamera() {
        // This is used to bind the lifecycle of cameras to the lifecycle owner.
        // This eliminates the task of opening and closing the camera since CameraX is lifecycle-aware.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Attach the preview of the camera to the UI widget that will contain that preview
            val preview = Preview.Builder().build()
                .also {
                    val cameraPreview : PreviewView = findViewById(R.id.camera_preview)
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }

            // Select back camera as a default
            //TODO: Make the user select what camera to use, if more available. Make also decide between back and front camera
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Make sure nothing is bound to the cameraProvider,
            // and then bind our cameraSelector and preview object to the cameraProvider.
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

        //When starting the camera, build the ImageCapture object
        imageCapture = ImageCapture.Builder().build()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        //If the camera did not start successfully, imageCapture is still null
        val imageCapture = imageCapture ?: return

        // Create time stamped name using the FILENAME_FORMAT defined inside the companion object
        // This allows the MediaStore to be unique
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())

        //Create MediaStore to store the image. Used to share data across the different applications inside the device
        //TODO: Is this really what it does?
        //TODO: Modify the path where to save the images
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
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
                    Log.d(TAG, msg)
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }
            }
        )
    }

    //TODO: Decide how to handle permissions. Require all permissions or only the basic ones
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    //What to do when the permission request result are available
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //TODO: If asked permission for microphone, start camera also if microphone permission is not granted so not all permissions need to be granted
                startCamera()  //Start camera if all permissions are granted
            } else {
                //Show a message that explains why the app does not work (camera permission not granted) and exit the app
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        //TAG for debug
        private const val TAG = "PhotoPro"

        //File format when photos are saved
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        //Permission code decided arbitrarily
        private const val REQUEST_CODE_PERMISSIONS = 100

        //Array of required permissions that need to be checked
        private val REQUIRED_PERMISSIONS =arrayOf(Manifest.permission.CAMERA)
    }
}
