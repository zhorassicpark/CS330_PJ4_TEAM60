package com.example.pj4test.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.pj4test.MainActivity
import com.example.pj4test.ProjectConfiguration
import com.example.pj4test.audioInference.SnapClassifier
import com.example.pj4test.databinding.FragmentAudioBinding

class AudioFragment: Fragment(), SnapClassifier.DetectorListener {
    private val TAG = "AudioFragment"

    private var _fragmentAudioBinding: FragmentAudioBinding? = null

    private val fragmentAudioBinding
        get() = _fragmentAudioBinding!!

    // classifiers
    lateinit var snapClassifier: SnapClassifier

    // views
    lateinit var snapView: TextView


    lateinit var mainActivity: MainActivity

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mainActivity = context as MainActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentAudioBinding = FragmentAudioBinding.inflate(inflater, container, false)

        return fragmentAudioBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        snapView = fragmentAudioBinding.SnapView

        snapClassifier = SnapClassifier()
        snapClassifier.initialize(requireContext())
        snapClassifier.setDetectorListener(this)
    }

    override fun onPause() {
//        Log.w(TAG, "Pause!")
        super.onPause()
        snapClassifier.stopInferencing()
    }


    override fun onResume() {
//        Log.w(TAG, "Resume!")
        super.onResume()
        snapClassifier.startInferencing()
    }

    override fun onResults(score: Float) {
        mainActivity.increaseCoolCount()
        activity?.runOnUiThread {
            if (score > SnapClassifier.THRESHOLD) {
                mainActivity.setAudioDetectedTrue()
                snapView.text = "CAT MEOW DETECTED"
                snapView.setBackgroundColor(ProjectConfiguration.activeBackgroundColor)
                snapView.setTextColor(ProjectConfiguration.activeTextColor)
            } else {
                mainActivity.setAudioDetectedFalse()
                snapView.text = "NOT DETECTED"
                snapView.setBackgroundColor(ProjectConfiguration.idleBackgroundColor)
                snapView.setTextColor(ProjectConfiguration.idleTextColor)
            }
        }
    }
}