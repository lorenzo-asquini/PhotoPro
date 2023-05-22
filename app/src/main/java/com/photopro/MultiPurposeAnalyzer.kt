package com.photopro

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.Core.addWeighted
import org.opencv.core.Mat
import org.opencv.core.Size
import java.io.OutputStream

class MultiPurposeAnalyzer(private val activity: AppCompatActivity, private val rotation: Int) : ImageAnalysis.Analyzer{

    private val preferences : SharedPreferences = activity.getPreferences(Context.MODE_PRIVATE)

    //Global variable so it can be accessed without passing it as a parameter
    private var imageBitmap : Bitmap? = null

    //Running result of frame avg
    private var frameAvgResult : Mat? = null

    //Number of frames averaged till now
    var framesAveraged = -1

    init {
        OpenCVLoader.initDebug()
    }

    override fun analyze(image: ImageProxy){

        val poseShootValue = preferences.getInt(SharedPrefs.POSE_SHOOT_KEY, Constant.POSE_SHOOT_OFF)
        val nightModeValue = preferences.getInt(SharedPrefs.NIGHT_MODE_KEY, Constant.NIGHT_MODE_OFF)
        val frameAvgValue = preferences.getInt(SharedPrefs.FRAME_AVG_KEY, Constant.FRAME_AVG_OFF)

        val isImageBitmapNeeded = poseShootValue == Constant.POSE_SHOOT_ON || nightModeValue == Constant.NIGHT_MODE_ON

        //Create only one copy of the image that will be used by everyone
        imageBitmap =
            if(isImageBitmapNeeded){
                image.toBitmap()
            }else{
                null  //Clear old bitmap to free memory
            }

        if(poseShootValue == Constant.POSE_SHOOT_ON){
            //TODO: run function. Create function inside this class and access the imageBitmap directly as class variable
        }

        if(nightModeValue == Constant.NIGHT_MODE_ON){
            //TODO: run function. Create function inside this class and access the imageBitmap directly as class variable
        }
        if(frameAvgValue == Constant.FRAME_AVG_ON){
            //If averaging is happening, create imageBitmap if not already created
            if(framesAveraged >= 0){
                imageBitmap = imageBitmap ?: image.toBitmap()
                frameAvg()
            }
        }

        image.close()
    }

    fun startFrameAvg() {
        //Start capturing a picture with frame average
        framesAveraged = 0
    }

    //TODO: Put a disclaimer that shows image averaging in process
    private fun frameAvg(){
        //Load current frame in Mat
        val currentFrame = Mat()
        bitmapToMat(imageBitmap, currentFrame)

        if(frameAvgResult == null){
            //Create the result Mat with the same characteristics as the input images
            frameAvgResult = Mat(Size(currentFrame.width().toDouble(), currentFrame.height().toDouble()), currentFrame.type())
        }
        //Add weighted to add current frame to result
        val alpha = 1.0/(framesAveraged + 1)
        val beta = 1.0 - alpha
        addWeighted(currentFrame, alpha, frameAvgResult, beta, 0.0, frameAvgResult)

        //Increase the number of frames used
        framesAveraged++

        val frameToAverage = preferences.getInt(SharedPrefs.FRAMES_TO_AVERAGE_KEY, Constant.DEFAULT_FRAMES_TO_AVERAGE)

        //If enough frames are averaged
        if(framesAveraged >= frameToAverage){
            //Create bitmap for result from Mat
            val resultBitmap = imageBitmap!!.copy(imageBitmap!!.config, true)
            matToBitmap(frameAvgResult, resultBitmap)

            //Reverse portrait is not supported by the app
            //Image delivered by the analyzer are not rotated correctly
            val rotationDegrees =
                when(rotation){
                    Surface.ROTATION_0 -> 90.0f
                    Surface.ROTATION_90 -> 0.0f
                    Surface.ROTATION_270 -> 180.0f
                    else -> 0.0f
                }

            //Rotate correctly the final image
            val matrix = Matrix()
            matrix.setRotate(rotationDegrees)
            val rotatedResultBitmap = Bitmap.createBitmap(resultBitmap, 0, 0, resultBitmap.width, resultBitmap.height, matrix, true)

            val contentValues = getSaveImageContentValues()

            //Get Uri where to save the image
            val uri: Uri? = activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                saveImageToStream(rotatedResultBitmap, activity.contentResolver.openOutputStream(uri))
                activity.contentResolver.update(uri, contentValues, null, null)
            }

            //Vibration when frame avg has ended
            vibratePhone(activity, 50)

            //Clear memory and stop frame averaging
            frameAvgResult = null
            framesAveraged = -1

            return
        }
    }

    private fun saveImageToStream(bitmap: Bitmap, outputStream: OutputStream?) {
        if (outputStream != null) {
            try {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
                Log.i(Constant.TAG, "Photo saved")
            } catch (e: Exception) {
                Log.e(Constant.TAG, "Photo save failed: ${e.printStackTrace()}")
            }
        }
    }
}