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

class ImportActivity : Activity() {
    private lateinit var videoView: VideoView
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var editing = false
    private var previewing = false
    private lateinit var timeline: ClipTimeline
    private lateinit var rangeLabel: android.widget.TextView
    private lateinit var previewButton: android.widget.Button
    private var repeatRunnable: Runnable? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val checkRange = object : Runnable {
        override fun run() {
            if (editing && previewing && videoView.currentPosition >= timeline.end) {
                previewing = false
                videoView.pause()
                videoView.seekTo(timeline.start)
                updatePreviewButton()
            }
            handler.postDelayed(this, 20)
        }
    }

    private fun updateRange(startChanged: Boolean) {
        previewing = false
        videoView.pause()
        videoView.seekTo(if (startChanged) timeline.start else (timeline.end - 1).coerceAtLeast(0))
        rangeLabel.text = java.lang.String.format(java.util.Locale.US,
            "Start %.2f s  —  End %.2f s", timeline.start / 1000.0, timeline.end / 1000.0)
        timeline.invalidate()
        updatePreviewButton()
    }

    private fun updatePreviewButton() {
        if (!::previewButton.isInitialized) return
        previewButton.text = if (previewing) "Pause preview" else "Preview selection"
    }

    private fun seekBy(deltaMs: Int) {
        val duration = videoView.duration
        if (duration <= 0) return
        videoView.seekTo((videoView.currentPosition + deltaMs).coerceIn(0, duration))
    }

    private fun nudge(startChanged: Boolean, deltaMs: Int) {
        if (startChanged) timeline.start = (timeline.start + deltaMs).coerceIn(0, timeline.end - 1)
        else timeline.end = (timeline.end + deltaMs).coerceIn(timeline.start + 1, timeline.duration)
        updateRange(startChanged)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    private fun addNudgeButton(
        parent: android.widget.LinearLayout,
        label: String,
        startChanged: Boolean,
        deltaMs: Int,
    ) {
        val button = android.widget.Button(this).apply {
            text = label
            contentDescription = "$label 0.5 seconds, hold to repeat"
        }
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    stopRepeat()
                    nudge(startChanged, deltaMs)
                    val runnable = object : Runnable {
                        override fun run() {
                            if (!button.isPressed) {
                                stopRepeat()
                                return
                            }
                            nudge(startChanged, deltaMs)
                            handler.postDelayed(this, REPEAT_INTERVAL_MS)
                        }
                    }
                    repeatRunnable = runnable
                    handler.postDelayed(runnable, REPEAT_DELAY_MS)
                }
                android.view.MotionEvent.ACTION_UP -> {
                    stopRepeat()
                    view.performClick()
                }
                android.view.MotionEvent.ACTION_CANCEL -> stopRepeat()
            }
            false
        }
        parent.addView(button, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.data
        val loop = intent.getBooleanExtra(LOOP_EXTRA, false)
        if (source == null) {
            finish()
            return
        }
        val root = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
            applySystemInsets()
        }
        videoView = VideoView(this).apply {
            setOnPreparedListener { player ->
                mediaPlayer = player
                val enabled = getSharedPreferences(MainActivity.SOUND_PREFS, MODE_PRIVATE)
                    .getBoolean(MainActivity.KEY_SOUND_ENABLED, true)
                val volume = if (enabled) 1f else 0f
                player.setVolume(volume, volume)
                player.isLooping = loop && !editing
                timeline.duration = duration.coerceAtLeast(1)
                timeline.start = savedInstanceState?.getInt("start", 0)?.coerceIn(0, timeline.duration - 1) ?: 0
                timeline.end = savedInstanceState?.getInt("end", timeline.duration)?.coerceIn(timeline.start + 1, timeline.duration) ?: timeline.duration
                timeline.invalidate()
                editing = true
                player.isLooping = false
                updateRange(true)
            }
            setOnCompletionListener {
                if (editing) {
                    previewing = false
                    seekTo(timeline.start)
                } else if (!loop) finish()
            }
            setOnErrorListener { _, _, _ ->
                Toast.makeText(this@ImportActivity, R.string.playback_error, Toast.LENGTH_SHORT).show()
                finish()
                true
            }
        }
        val videoContainer = FrameLayout(this)
        videoContainer.addView(videoView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        root.addView(videoContainer, android.widget.LinearLayout.LayoutParams(-1, 0, 1f))
        val closePlayer = View.OnClickListener {
            videoView.stopPlayback()
            finish()
        }
        videoView.setOnClickListener(closePlayer)
        videoView.contentDescription = getString(R.string.close_video)
        root.setOnClickListener(closePlayer)
        val controls = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xDD000000.toInt())
        }
        val editor = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }
        rangeLabel = android.widget.TextView(this).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        timeline = ClipTimeline(this).apply {
            contentDescription = "Clip start and end; use the buttons below for fine adjustment"
            onChange = { updateRange(it) }
        }
        editor.addView(rangeLabel)
        editor.addView(timeline, android.widget.LinearLayout.LayoutParams(-1, (64 * resources.displayMetrics.density).toInt()))
        val fine = android.widget.LinearLayout(this)
        addNudgeButton(fine, "Start −", true, -500)
        addNudgeButton(fine, "Start +", true, 500)
        addNudgeButton(fine, "End −", false, -500)
        addNudgeButton(fine, "End +", false, 500)
        editor.addView(fine)
        val previewRow = android.widget.LinearLayout(this)
        previewRow.addView(android.widget.Button(this).apply {
            text = "−5s"
            contentDescription = "Rewind 5 seconds"
            setOnClickListener { seekBy(-5000) }
        }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        previewButton = android.widget.Button(this).apply {
            text = "Preview selection"
            setOnClickListener {
                if (previewing) {
                    previewing = false
                    videoView.pause()
                } else {
                    videoView.seekTo(timeline.start)
                    previewing = true
                    videoView.start()
                }
                updatePreviewButton()
            }
        }
        previewRow.addView(previewButton, android.widget.LinearLayout.LayoutParams(0, -2, 2f))
        previewRow.addView(android.widget.Button(this).apply {
            text = "+5s"
            contentDescription = "Forward 5 seconds"
            setOnClickListener { seekBy(5000) }
        }, android.widget.LinearLayout.LayoutParams(0, -2, 1f))
        editor.addView(previewRow)
        controls.addView(editor)
        val nameInput = android.widget.EditText(this).apply {
            hint = "Move name"
            setText(savedInstanceState?.getString("name").orEmpty())
            tag = "moveName"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine()
        }
        controls.addView(nameInput)
        controls.addView(android.widget.TextView(this).apply {
            text = "Fast trim: saved start snaps to the preceding video keyframe."
            setTextColor(Color.LTGRAY)
        })
        controls.addView(android.widget.Button(this).apply {
            text = "Save move"
            setOnClickListener {
                val name = nameInput.text.toString().trim()
                if (name.isEmpty()) { nameInput.error = "Enter a move name"; return@setOnClickListener }
                if (videoView.duration <= 0) return@setOnClickListener
                videoView.pause()
                previewing = false
                updatePreviewButton()
                isEnabled = false
                text = "Saving…"
                val start = timeline.start
                val end = timeline.end
                Thread {
                    val result = runCatching { SavedMoves.save(applicationContext, source, name, start, end) }
                    runOnUiThread {
                        if (!isDestroyed) {
                            if (result.isSuccess) finish() else {
                                isEnabled = true
                                text = "Save move"
                                Toast.makeText(this@ImportActivity, "Could not save this video. Try an MP4 video.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }.start()
            }
        })
        val scroll = android.widget.ScrollView(this)
        scroll.addView(controls)
        root.addView(scroll, android.widget.LinearLayout.LayoutParams(-1, (300 * resources.displayMetrics.density).toInt()))
        setContentView(root)
        handler.post(checkRange)
        videoView.setVideoURI(source)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("start", timeline.start)
        outState.putInt("end", timeline.end)
        outState.putString("name", window.decorView.findViewWithTag<android.widget.EditText>("moveName")?.text?.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        handler.removeCallbacksAndMessages(null)
        if (::videoView.isInitialized) videoView.stopPlayback()
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    companion object {
        const val VIDEO_EXTRA = "video"
        const val LOOP_EXTRA = "loop"
        private const val REPEAT_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 120L
    }
}
