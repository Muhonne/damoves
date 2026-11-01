package com.example.soundboard

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import com.google.android.material.button.MaterialButton

class ImportActivity : Activity() {
    private lateinit var videoView: VideoView
    private var mediaPlayer: android.media.MediaPlayer? = null
    private var editing = false
    private var previewing = false
    private lateinit var timeline: ClipTimeline
    private lateinit var rangeLabel: TextView
    private lateinit var previewButton: MaterialButton
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
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    private fun nudge(startChanged: Boolean, deltaMs: Int) {
        if (startChanged) timeline.start = (timeline.start + deltaMs).coerceIn(0, timeline.end - 1)
        else timeline.end = (timeline.end + deltaMs).coerceIn(timeline.start + 1, timeline.duration)
        window.decorView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        updateRange(startChanged)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    private fun addNudgeButton(
        parent: LinearLayout,
        id: Int,
        label: String,
        startChanged: Boolean,
        deltaMs: Int,
    ) {
        val button = tonalButton(label, id)
        button.contentDescription = "$label 0.5 seconds, hold to repeat"
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
        parent.addView(button, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun tonalButton(label: String, id: Int) = MaterialButton(this).apply {
        this.id = id
        text = label
        backgroundTintList = getColorStateList(R.color.primary_container)
        setTextColor(getColor(R.color.on_primary_container))
    }

    private fun outlinedButton(label: String, id: Int, onClick: () -> Unit) = MaterialButton(this).apply {
        this.id = id
        text = label
        backgroundTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        strokeWidth = dp(1)
        strokeColor = getColorStateList(R.color.outline)
        setTextColor(getColor(R.color.text_primary))
        setOnClickListener { onClick() }
    }

    private fun filledButton(label: String, id: Int) = MaterialButton(this).apply {
        this.id = id
        text = label
        backgroundTintList = getColorStateList(R.color.primary)
        setTextColor(getColor(R.color.on_primary))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.data
        val loop = intent.getBooleanExtra(LOOP_EXTRA, false)
        if (source == null) {
            finish()
            return
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.surface))
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
        root.addView(videoContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        val closePlayer = View.OnClickListener {
            videoView.stopPlayback()
            finish()
        }
        videoView.setOnClickListener(closePlayer)
        videoView.contentDescription = getString(R.string.close_video)
        root.setOnClickListener(closePlayer)
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.surface_container))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        rangeLabel = TextView(this).apply {
            id = R.id.importRangeLabel
            setTextColor(getColor(R.color.text_primary))
            textSize = 15f
            gravity = Gravity.CENTER
            background = getDrawable(R.drawable.rounded_search_container)
            setPadding(dp(12), dp(8), dp(12), dp(8))
        }
        timeline = ClipTimeline(this).apply {
            contentDescription = "Clip start and end; use the buttons below for fine adjustment"
            onChange = { updateRange(it) }
        }
        controls.addView(rangeLabel)
        controls.addView(timeline, LinearLayout.LayoutParams(-1, dp(72)).apply {
            topMargin = dp(8)
        })
        val fine = LinearLayout(this)
        addNudgeButton(fine, R.id.importNudgeStartMinus, "Start −", true, -500)
        addNudgeButton(fine, R.id.importNudgeStartPlus, "Start +", true, 500)
        addNudgeButton(fine, R.id.importNudgeEndMinus, "End −", false, -500)
        addNudgeButton(fine, R.id.importNudgeEndPlus, "End +", false, 500)
        controls.addView(fine, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
        })
        val previewRow = LinearLayout(this)
        previewRow.addView(outlinedButton("−5s", R.id.importRewind) { seekBy(-5000) },
            LinearLayout.LayoutParams(0, -2, 1f))
        previewButton = filledButton("Preview selection", R.id.importPreviewButton).apply {
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
        previewRow.addView(previewButton, LinearLayout.LayoutParams(0, -2, 2f).apply {
            marginStart = dp(8)
            marginEnd = dp(8)
        })
        previewRow.addView(outlinedButton("+5s", R.id.importForward) { seekBy(5000) },
            LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(previewRow, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        controls.addView(EditText(this).apply {
            id = R.id.importNameInput
            hint = "Move name"
            setText(savedInstanceState?.getString("name") ?: intent.getStringExtra(NAME_EXTRA).orEmpty())
            tag = "moveName"
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            setSingleLine()
            background = getDrawable(R.drawable.rounded_search_container)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(12)
        })
        controls.addView(TextView(this).apply {
            text = "Fast trim: saved start snaps to the preceding video keyframe."
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
        }, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(8)
        })
        val saveButton = filledButton("Save move", R.id.importSaveButton)
        saveButton.setOnClickListener {
            val name = window.decorView.findViewWithTag<EditText>("moveName")?.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                window.decorView.findViewWithTag<EditText>("moveName")?.error = "Enter a move name"
                return@setOnClickListener
            }
            if (videoView.duration <= 0) return@setOnClickListener
            videoView.pause()
            previewing = false
            updatePreviewButton()
            saveButton.isEnabled = false
            saveButton.text = "Saving…"
            val start = timeline.start
            val end = timeline.end
            Thread {
                val result = runCatching { SavedMoves.save(applicationContext, source, name, start, end) }
                runOnUiThread {
                    if (!isDestroyed) {
                        if (result.isSuccess) finish() else {
                            saveButton.isEnabled = true
                            saveButton.text = "Save move"
                            Toast.makeText(this@ImportActivity, "Could not save this video. Try an MP4 video.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.start()
        }
        val scroll = ScrollView(this)
        scroll.addView(controls)
        root.addView(scroll, LinearLayout.LayoutParams(-1, dp(320)))
        root.addView(saveButton, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(16), dp(8), dp(16), dp(16))
        })
        setContentView(root)
        handler.post(checkRange)
        videoView.setVideoURI(source)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("start", timeline.start)
        outState.putInt("end", timeline.end)
        outState.putString("name", window.decorView.findViewWithTag<EditText>("moveName")?.text?.toString())
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
        const val NAME_EXTRA = "moveName"
        private const val REPEAT_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 120L
    }
}
