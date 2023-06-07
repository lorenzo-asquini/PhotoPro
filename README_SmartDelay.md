MainActivity:

	At the beginning of the source
	Uncomment tv_timer Textview for timer of smart delay
	Added interface for listener of smart delay

	Added onEventCall() method, used to handle the timer for smart delay.
	Uncomment tv_timer Textview for timer of smart delay
	
	Modified setOnClickListener of SmartDelayButton
	Uncomment tv_timer Textview for timer of smart delay


MultiPurposeAnalyzer:

	Added multiple methods:
	mlPoseDetection() for initializing PoseDetector
	smartDelay(image: ImageProxy) to handle SmartDelay	
	addListener(ls: MyListener) to have access on MainActivity onEventCall
	notifyActivity() to handle the call of onEventCall()
	
	Added check on image.close() in analyze() to avoid conflicts with smart delay onSuccessListener


CameraUtil:
	 Added listener with analyzer.addListener(activity as MyListener) for smart delay.

build.gradle:
	Added dependencies for mlkit-posedetecion

IMPORTANT:
	Please add the tv_timer TextView on the layout file with id = count_timer