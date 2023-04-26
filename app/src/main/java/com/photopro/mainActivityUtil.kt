package com.photopro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

//File with useful functions for MainActivity

fun cameraPermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun microphonePermissionGranted(baseContext : Context) =
    ContextCompat.checkSelfPermission(baseContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

fun cameraSelector(baseContext: Context){

}
