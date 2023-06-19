package com.example.pj4test

import android.Manifest.permission.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    // permissions
    private val permissions = arrayOf(RECORD_AUDIO, CAMERA, SEND_SMS)
    private val PERMISSIONS_REQUEST = 0x0000001;
//    private var audioDetected = false;
//    private var smsCoolCount = 1800;
//    private var task: TimerTask? = null

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions() // check permissions
//        task = Timer().scheduleAtFixedRate(0, SnapClassifier.REFRESH_INTERVAL_MS) {
//            increaseCoolCount()
//        }
    }

    private fun checkPermissions() {
        if (permissions.all{ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED}){
            Log.d(TAG, "All Permission Granted")
        }
        else{
            requestPermissions(permissions, PERMISSIONS_REQUEST)
        }
    }

//    fun stopAudioInference(){
//        val audioFragment : Fragment = supportFragmentManager.findFragmentById(R.id.audioFragmentContainerView) as Fragment
//        audioFragment.onPause()
//    }
//    fun startAudioInference(){
//        val audioFragment : Fragment = supportFragmentManager.findFragmentById(R.id.audioFragmentContainerView) as Fragment
//        audioFragment.onResume()
//    }
//    fun setAudioDetectedTrue(){
//        audioDetected = true
//    }
//    fun setAudioDetectedFalse(){
//        audioDetected = false
//    }
//    fun isAudioDetected(): Boolean{
//        return audioDetected
//    }
//    fun initCoolCount(){
//        smsCoolCount = 0;
//    }
//    fun isCoolCountFull(): Boolean{
//        return (smsCoolCount > 1800);
//    }
//    fun increaseCoolCount(){
//        smsCoolCount++;
//    }


//    val camerafragment : Fragment = supportFragmentManager.findFragmentById(R.id.cameraFragmentContainerView) as Fragment
}