package com.example.pj4test

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.pj4test.audioInference.SnapClassifier
import com.example.pj4test.cameraInference.PersonClassifier
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


const val SERVICE_ID = 1
class ForeService : PersonClassifier.DetectorListener, SnapClassifier.DetectorListener, LifecycleService() {
    private val TAG = "ForeService"
    val FORE_CHANNEL_ID = "forenotification"
    private var coolCount = 60.0
    private var isAudioDetected = false;


    private lateinit var personClassifier: PersonClassifier
    private lateinit var bitmapBuffer: Bitmap
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService


    // classifiers
    lateinit var snapClassifier: SnapClassifier

    fun Notification() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nc = NotificationChannel(FORE_CHANNEL_ID, "Foreground", NotificationManager.IMPORTANCE_DEFAULT)
            val nm = getSystemService(NotificationManager::class.java)

            nm.createNotificationChannel(nc)
        } else {
            Toast.makeText(this, "알림을 실행할 수 없음", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.w(TAG, "onStartCommand called!")
        Notification()

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, //context
            0, //request code
            intent, //flag
            PendingIntent.FLAG_IMMUTABLE //flag
        )
        val notiBuilder = NotificationCompat.Builder(this, FORE_CHANNEL_ID)
            .setContentTitle("Foreground Task")
            .setContentText("Hungry Cat Detection Is Running")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)

//        Log.w(TAG, "start foreground...")
        startForeground(SERVICE_ID,notiBuilder.build())
//        Log.w(TAG, "end foreground...")

        personClassifier = PersonClassifier()
        personClassifier.initialize(this)
        personClassifier.setDetectorListener(this)
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()
//        setUpCamera()

        imageAnalyzer = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
        imageAnalyzer!!.setAnalyzer(Executors.newSingleThreadExecutor()) { image -> detectObjects(image) }

        snapClassifier = SnapClassifier()
        snapClassifier.initialize(this)
        snapClassifier.setDetectorListener(this)


//        GlobalScope.launch{
//            while(true){
//                delay(1000)
//                coolCount++
//            }
//        }

        return START_NOT_STICKY
    }


    // Initialize CameraX, and prepare to bind the camera use cases
    private fun setUpCamera() {
        Log.w(TAG, "set up camera!")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                val cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases(cameraProvider)
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    // Declare and bind preview, capture and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases(cameraProvider: ProcessCameraProvider) {
        Log.w(TAG, "Bind Camera Uses!")

        // CameraSelector - makes assumption that we're only using the back camera
        val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
        // The analyzer can then be assigned to the instance
        Log.w(TAG, "set analyzer to camera executor")
//        imageAnalyzer!!.setAnalyzer(cameraExecutor) { image -> detectObjects(image) }
        imageAnalyzer!!.setAnalyzer(Executors.newSingleThreadExecutor()) { image -> detectObjects(image) }
        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        Log.w(TAG, "detectObject is being Called!")
        if (!::bitmapBuffer.isInitialized) {
            // The image rotation and RGB image buffer are initialized only once
            // the analyzer has started running
            bitmapBuffer = Bitmap.createBitmap(
                image.width,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        }
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }
        val imageRotation = image.imageInfo.rotationDegrees

        // Pass Bitmap and rotation to the object detector helper for processing and detection
        personClassifier.detect(bitmapBuffer, imageRotation)
    }


    // Update UI after objects have been detected. Extracts original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onObjectDetectionResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
            // find at least one bounding box of the person
            val isCatDetected: Boolean = results!!.find { it.categories[0].label == "cat" } != null

            // change UI according to the result
            if (isCatDetected) {
//                Log.w(TAG, "Cat Detected!")

//                snapClassifier.startInferencing()

                // 알림 띄우기
//                if(isAudioDetected && coolCount > 60){
                if(coolCount > 60){
                    val smsManager: SmsManager
                    if (Build.VERSION.SDK_INT>=23) {
                        smsManager = this.getSystemService(SmsManager::class.java)
                    }
                    else{
                        smsManager = SmsManager.getDefault()
                    }
                    Log.w(TAG, "sending messages")
                    smsManager.sendTextMessage("+821074776872", null, "고양이 배고프다옹! 밥 달라옹!", null, null)
                    Log.w(TAG, "end sending messages\n")
                    coolCount = 0.0
                }
            } else {
//                snapClassifier.stopInferencing()
            }
    }

    override fun onObjectDetectionError(error: String) {

    }

    override fun onResults(score: Float) {
//        mainActivity.increaseCoolCount()
        coolCount += 0.0333
//        isAudioDetected = score > SnapClassifier.THRESHOLD
        if(score > SnapClassifier.THRESHOLD && !isAudioDetected){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener(
                {
                    // CameraProvider
                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                    try {
                        // A variable number of use-cases can be passed here -
                        // camera provides access to CameraControl & CameraInfo
                        camera = cameraProvider.bindToLifecycle(
                            this,
                            cameraSelector,
                            imageAnalyzer
                        )
                    } catch (exc: Exception) {
                        Log.e(TAG, "Use case binding failed", exc)
                    }
                },
                ContextCompat.getMainExecutor(this)
            )
            isAudioDetected = true;
        }
        else if(score < SnapClassifier.THRESHOLD && isAudioDetected){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener(
                {
                    // CameraProvider
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                },
                ContextCompat.getMainExecutor(this)
            )
            isAudioDetected = false;
        }
    }

}