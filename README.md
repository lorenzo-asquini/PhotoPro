# PhotoPro

- App available on the Google Play Store: [PhotoPro](https://play.google.com/store/apps/details?id=com.project_photopro).
- The app was developed with the goal of utilizing the features provided by [CameraX](https://developer.android.com/training/camerax) and [Camera2](https://developer.android.com/training/camera2) on Android.

## Features

- The app can be used to take photos using the different features provided:
  - Flash: The flash can be on, off, automatic, or always on.
  - Frame Averaging: It is possible to take a photo by averaging multiple consecutive frames. The number of frames averaged to produce a single image can be set in the settings menu. The output resolution may be lower than the maximum resolution of the sensor (the resolution of the resulting image is the maximum allowed by the image analyzer).
  - Smart Delay: It is possible to take a photo after a countdown, which starts when a person is recognized in front of the camera. The user is notified of a person's identification with a sound. The duration of the timer and the presence of the notification sound can be set in the settings menu.
  - Night Mode: It is possible to activate or deactivate the night mode, or to set the night mode to automatic. This possibility will make the camera automatically switch to night mode in low-light conditions.
  - HDR, Bokeh, Face Retouch: Inside the settings menu, it is possible to decide whether to use an extension when taking a photo. If night mode is active, any other extension is not used.
  - Pro Mode: It is possible to take photos by setting the desired ISO and shutter speed. The maximum shutter speed allowed is 500 ms (1/2).

## Notes

  - Depending on the device used, the extensions (Night Mode, HDR, Bokeh, Face Retouch) may not be available. This depends on the manufacturer's decision to make the extension available to third party apps.
  - Pro Mode may not be available on some devices. A camera hardware level equal to FULL or LEVEL_3 is required.
  - The app uses the library OpenCV to perform the frame average.
  - Because not all devices have access to the device extensions, it was not possible to deeply test the extension integration. The app was tested on:
    - Xiaomi Mi 10 Lite 5G (no extensions available)
    - Tablet?

## Authors

The project was realized by: 
  - [Lorenzo Asquini](https://github.com/LorenzoAsquini)
  - [Michela Schibuola](https://github.com/Fabrifio)
  - [Fabrizio Genilotti](https://github.com/michela-schibuola)
