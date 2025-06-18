package com.unewexp.notice

import android.content.Context
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log

class RecordController(private val context: Context) {

    private var audioRecorder: MediaRecorder? = null
    private var path = ""

    fun start() {
        Log.d(TAG, "Start")
        path = getAudioPath()
        audioRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(path)
            prepare()
            start()
        }
    }

    private fun getAudioPath(): String {
        return "${Environment.getExternalStorageDirectory().absolutePath}/AudioBooks/au${System.currentTimeMillis()}.ogg"
    }

    fun stop(): String {
        audioRecorder?.let {
            Log.d(TAG, "Stop")
            it.stop()
            it.release()
        }
        audioRecorder = null
        return path
    }

    fun isAudioRecording() = audioRecorder != null

    fun getVolume() = audioRecorder?.maxAmplitude ?: 0

    private companion object {
        private val TAG = RecordController::class.java.name
    }
}