package com.photopro

import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera

//Used to share more functions between normal mode and PRO mode
abstract class CameraAppCompactActivity : AppCompatActivity() {
    abstract var camera: Camera?
}