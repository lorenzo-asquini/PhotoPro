package com.photopro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat


//File with useful functions for MainActivity

fun cameraPermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun microphonePermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

fun openGallery(baseContext : Context) {
    //Create an intent that opens the default gallery app
    val intent = Intent(Intent.ACTION_MAIN)
    //The app must support CATEGORY_APP_GALLERY (most gallery apps do, but there may be some exceptions)
    intent.addCategory(Intent.CATEGORY_APP_GALLERY)

    //This flag is necessary to open the gallery app as a new task, and not as an activity of this app
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

    // Start the default gallery app
    baseContext.startActivity(intent)
}
