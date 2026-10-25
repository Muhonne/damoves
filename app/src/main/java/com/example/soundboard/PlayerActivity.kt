package com.example.soundboard

import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import android.widget.VideoView
import android.view.Gravity
import android.view.View

class PlayerActivity : Activity() {
    private lateinit var videoView: VideoView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoResId = intent.getIntExtra(VIDEO_EXTRA, 0)
        if (videoResId == 0) {
            finish()
            return
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            applySystemInsets()
        }
        videoView = VideoView(this).apply {
            setOnPreparedListener { player ->
                val enabled = getSharedPreferences(MainActivity.SOUND_PREFS, MODE_PRIVATE)
                    .getBoolean(MainActivity.KEY_SOUND_ENABLED, true)
                val volume = if (enabled) 1f else 0f
                player.setVolume(volume, volume)
                start()
            }
            setOnCompletionListener { finish() }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@PlayerActivity, R.string.playback_error, Toast.LENGTH_SHORT).show()
                finish()
                true
            }
        }
        root.addView(videoView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        val closePlayer = View.OnClickListener {
            videoView.stopPlayback()
            finish()
        }
        videoView.setOnClickListener(closePlayer)
        videoView.contentDescription = getString(R.string.close_video)
        root.setOnClickListener(closePlayer)
        setContentView(root)
        videoView.setVideoURI(Uri.parse("android.resource://$packageName/$videoResId"))
    }

    override fun onStop() {
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onStop()
        finish()
    }

    companion object {
        const val VIDEO_EXTRA = "video"
    }
}
