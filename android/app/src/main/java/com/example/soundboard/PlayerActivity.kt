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
    private lateinit var playback: PlayerCore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoResId = intent.getIntExtra(VIDEO_EXTRA, 0)
        val loop = intent.getBooleanExtra(LOOP_EXTRA, false)
        playback = PlayerCore(loop)
        if (videoResId == 0 && intent.getStringExtra("uri") == null) {
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
                player.isLooping = playback.loop
                start()
            }
            setOnCompletionListener {
                playback.send(PlayerCore.Action.Completed)
                if (playback.closed) finish() else { seekTo(0); start() }
            }
            setOnErrorListener { _, _, _ ->
                playback.send(PlayerCore.Action.Failed)
                if (playback.error != null) Toast.makeText(this@PlayerActivity, R.string.playback_error, Toast.LENGTH_SHORT).show()
                finish()
                true
            }
        }
        root.addView(videoView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        val closePlayer = View.OnClickListener {
            playback.send(PlayerCore.Action.Close)
            videoView.stopPlayback()
            finish()
        }
        videoView.setOnClickListener(closePlayer)
        videoView.contentDescription = getString(R.string.close_video)
        root.setOnClickListener(closePlayer)
        setContentView(root)
        videoView.setVideoURI(Uri.parse(intent.getStringExtra("uri") ?: "android.resource://$packageName/$videoResId"))
    }

    override fun onStop() {
        if (::playback.isInitialized) playback.send(PlayerCore.Action.Leave)
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onStop()
        finish()
    }

    companion object {
        const val VIDEO_EXTRA = "video"
        const val LOOP_EXTRA = "loop"
    }
}
