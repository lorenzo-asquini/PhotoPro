package com.project_photopro

object Constant{

    const val FLASH_OFF = 0
    const val FLASH_ON = 1
    const val FLASH_AUTO = 2
    const val FLASH_ALWAYS_ON = 3
    const val FLASH_STATES = 4  //Number of states the flash can be

    const val FRAME_AVG_OFF = 0
    const val FRAME_AVG_ON = 1
    const val FRAME_AVG_STATES = 2  //Number of states the frame average shoot can be

    const val SMART_DELAY_OFF = 0
    const val SMART_DELAY_ON = 1
    const val SMART_DELAY_STATES = 2  //Number of states the smart delay can be

    const val NIGHT_MODE_OFF = 0
    const val NIGHT_MODE_ON = 1
    const val NIGHT_MODE_AUTO = 2
    const val NIGHT_MODE_STATES = 3  //Number of states the night mode can be

    const val CAMERA_BACK = 0
    const val CAMERA_FRONT = 1
    const val CAMERA_STATES = 2  //Number of states the camera can be

    const val PRO_MODE_OFF = 0  //So normal mode is on
    const val PRO_MODE_ON = 1  //So normal mode is off
    const val PRO_MODE_STATES = 2

    const val HDR_OFF = 0
    const val HDR_ON = 1

    const val BOKEH_OFF = 0
    const val BOKEH_ON = 1

    const val FACE_RETOUCH_OFF = 0
    const val FACE_RETOUCH_ON = 1

    const val DEFAULT_SMART_DELAY_SECONDS = 3
    const val DEFAULT_FRAMES_TO_AVERAGE = 5

    const val SMART_DELAY_NOTIFICATION_OFF = 0
    const val SMART_DELAY_NOTIFICATION_ON = 1

    //TAG for debug
    const val TAG = "PhotoPro"

    //File format when photos are saved
    const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

    //Permission code decided arbitrarily
    const val REQUEST_CODE_PERMISSIONS = 100

    //Key for saving zoom in Bundle
    const val ZOOM_VALUE_KEY = "zoom_value"
}

object SharedPrefs{
    const val SHARED_PREFERENCES_KEY = "global_shared_preferences"

    const val FLASH_KEY = "flash_value"
    const val FRAME_AVG_KEY = "frameAvg_value"
    const val SMART_DELAY_KEY = "smart_delay_value"
    const val NIGHT_MODE_KEY = "night_mode_value"

    const val HDR_BACK_KEY = "HDR_back_mode"
    const val BOKEH_BACK_KEY = "bokeh_back_mode"
    const val FACE_RETOUCH_BACK_KEY = "face_retouch_back_mode"

    const val HDR_FRONT_KEY = "HDR_front_mode"
    const val BOKEH_FRONT_KEY = "bokeh_front_mode"
    const val FACE_RETOUCH_FRONT_KEY = "face_retouch_front_mode"

    const val SMART_DELAY_SECONDS_KEY = "smart_delay_seconds"
    const val NR_FRAMES_TO_AVERAGE_KEY = "frames_to_average"

    const val SMART_DELAY_NOTIFICATION_KEY = "smart_delay_notification"

    const val CAMERA_FACING_KEY = "camera_orientation"

    const val PRO_MODE_KEY = "pro_mode"

    const val ISO_FRONT_KEY = "iso_front"
    const val ISO_BACK_KEY = "iso_back"

    const val SHUTTER_SPEED_FRONT_KEY = "shutter_speed_front"
    const val SHUTTER_SPEED_BACK_KEY = "shutter_speed_back"
}