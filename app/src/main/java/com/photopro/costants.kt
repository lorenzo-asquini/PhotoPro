package com.photopro

import androidx.camera.core.ImageCapture

object Constant{
    //Same values as in ImageCapture for consistency
    const val FLASH_OFF = ImageCapture.FLASH_MODE_OFF
    const val FLASH_ON = ImageCapture.FLASH_MODE_ON
    const val FLASH_AUTO = ImageCapture.FLASH_MODE_AUTO

    const val SPEECH_SHOOT_OFF = 0
    const val SPEECH_SHOOT_ON = 1

    const val FRAME_AVG_OFF = 0
    const val FRAME_AVG_ON = 1

    const val POSE_SHOOT_OFF = 0
    const val POSE_SHOOT_ON = 1

    const val NIGHT_MODE_OFF = 0
    const val NIGHT_MODE_ON = 1
    const val NIGHT_MODE_AUTO = 2

    const val CAMERA_BACK = 0
    const val CAMERA_FRONT = 1

    //TAG for debug
    const val TAG = "PhotoPro"

    //File format when photos are saved
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    //Permission code decided arbitrarily
    const val REQUEST_CODE_PERMISSIONS = 100
}

object SharedPrefs{
    const val FLASH_KEY = "flash_value"
    const val SPEECH_SHOOT_KEY = "speech_shoot_value"
    const val FRAME_AVG_KEY = "frameAvg_value"
    const val POSE_SHOOT_KEY = "pose_shoot_value"
    const val NIGHT_MODE_KEY = "night_mode_value"

    const val HDR_KEY = "HDR_mode"
    const val BOKEH_KEY = "bokeh_mode"

    const val CAMERA_FACING_KEY = "camera_orientation"
}