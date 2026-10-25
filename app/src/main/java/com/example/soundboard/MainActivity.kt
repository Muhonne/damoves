package com.example.soundboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.util.Log
import java.util.concurrent.Executors
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*

class MainActivity : Activity() {
    private lateinit var allMoves: List<Move>
    private var moves = emptyList<Move>()
    private val adapter = MoveAdapter()
    private lateinit var searchInput: EditText
    private val thumbnails = mutableMapOf<Int, Bitmap>()
    private val thumbnailWorker = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allMoves = discoverMoves(resources)
        moves = allMoves
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(16, 17, 20))
            applySystemInsets()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), 0)
        }
        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(this).apply {
            setText(R.string.app_name)
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, dp(56), 1f))
        val search = EditText(this).apply {
            hint = getString(R.string.search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine()
            textSize = 16f
            setTextColor(Color.WHITE)
            setHintTextColor(Color.rgb(150, 153, 162))
            background = roundedBackground(Color.rgb(29, 31, 37))
            setPadding(dp(16), 0, dp(16), 0)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    moves = filterMoves(allMoves, s.toString())
                    adapter.notifyDataSetChanged()
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        val preferences = getSharedPreferences(SOUND_PREFS, MODE_PRIVATE)
        val toggle = Switch(this).apply {
            isChecked = preferences.getBoolean(KEY_SOUND_ENABLED, true)
            setText(if (isChecked) R.string.sound_on else R.string.sound_off)
            setOnCheckedChangeListener { _, enabled ->
                preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
                setText(if (enabled) R.string.sound_on else R.string.sound_off)
            }
            textSize = 13f
            switchPadding = dp(8)
            setTextColor(Color.rgb(190, 193, 202))
        }
        val grid = GridView(this).apply {
            numColumns = 3
            horizontalSpacing = dp(12)
            verticalSpacing = dp(12)
            clipToPadding = false
            setPadding(0, dp(16), 0, dp(88))
            selector = android.graphics.drawable.ColorDrawable(Color.TRANSPARENT)
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, position, _ ->
                openMove(moves[position])
            }
        }
        header.addView(toggle, LinearLayout.LayoutParams(-2, -2))
        content.addView(header)
        content.addView(search, LinearLayout.LayoutParams(-1, dp(48)))
        content.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        val dice = ImageButton(this).apply {
            setImageResource(R.drawable.ic_dice)
            contentDescription = getString(R.string.play_random_move)
            val circle = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.rgb(166, 180, 214))
            }
            background = android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33000000), circle, null)
            setPadding(dp(16), dp(16), dp(16), dp(16))
            elevation = dp(6).toFloat()
            isEnabled = allMoves.isNotEmpty()
            setOnClickListener { openMove(allMoves.random()) }
        }
        root.addView(dice, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(dp(16), dp(16), dp(16), dp(16))
        })
        setContentView(root)
        search.setText(savedInstanceState?.getString("query").orEmpty())
        searchInput = search
        loadThumbnails()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", searchInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun openMove(move: Move) {
        startActivity(Intent(this, PlayerActivity::class.java)
            .putExtra(PlayerActivity.VIDEO_EXTRA, move.videoResId))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun roundedBackground(color: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(12).toFloat()
    }

    private fun loadThumbnails() {
        thumbnailWorker.execute {
            for (move in allMoves) {
                if (Thread.currentThread().isInterrupted) break
                val retriever = MediaMetadataRetriever()
                try {
                    resources.openRawResourceFd(move.videoResId).use { descriptor ->
                        retriever.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                    }
                    val frame = if (android.os.Build.VERSION.SDK_INT >= 27) {
                        retriever.getScaledFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 360, 640)
                    } else {
                        retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    }
                    if (frame != null) runOnUiThread {
                        if (!isDestroyed) {
                            thumbnails[move.videoResId] = frame
                            adapter.notifyDataSetChanged()
                        }
                    }
                } catch (exception: Exception) {
                    Log.w("Damoves", "Cannot load thumbnail for ${move.name}", exception)
                } finally {
                    retriever.release()
                }
            }
        }
    }

    override fun onDestroy() {
        thumbnailWorker.shutdownNow()
        super.onDestroy()
    }

    private inner class MoveAdapter : BaseAdapter() {
        override fun getCount() = moves.size
        override fun getItem(position: Int) = moves[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tile = convertView as? LinearLayout ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBackground(Color.rgb(29, 31, 37))
                clipToOutline = true
                addView(object : ImageView(context) {
                    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                        val width = View.MeasureSpec.getSize(widthMeasureSpec)
                        val height = (width * 16f / 9f).toInt()
                        super.onMeasure(widthMeasureSpec,
                            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
                    }
                }.apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setBackgroundColor(Color.rgb(37, 40, 48))
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }, LinearLayout.LayoutParams(-1, -2))
                addView(TextView(context).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    textSize = 16f
                    setTextColor(Color.WHITE)
                    setPadding(dp(12), 0, dp(12), 0)
                }, LinearLayout.LayoutParams(-1, dp(48)))
            }
            val move = getItem(position)
            (tile.getChildAt(0) as ImageView).setImageBitmap(thumbnails[move.videoResId])
            (tile.getChildAt(1) as TextView).text = move.name
            return tile
        }
    }

    companion object {
        const val SOUND_PREFS = "soundboard"
        const val KEY_SOUND_ENABLED = "sound_enabled"
    }
}
