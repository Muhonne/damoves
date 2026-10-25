package com.example.soundboard

import android.app.Activity
import android.content.Intent
import android.graphics.Color
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
    private val allSounds = listOf(
        Sound("Kick", R.drawable.kick, R.raw.kick),
        Sound("Snare", R.drawable.snare, R.raw.snare),
        Sound("Horn", R.drawable.horn, R.raw.horn),
        Sound("Bell", R.drawable.bell, R.raw.bell)
    )
    private var sounds = allSounds
    private val adapter = SoundAdapter()
    private lateinit var searchInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 248, 250))
            applySystemInsets()
        }
        val search = EditText(this).apply {
            hint = getString(R.string.search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine()
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val query = s.toString().trim()
                    sounds = allSounds.filter { it.name.contains(query, ignoreCase = true) }
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
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        val grid = GridView(this).apply {
            numColumns = 2
            stretchMode = GridView.STRETCH_COLUMN_WIDTH
            adapter = this@MainActivity.adapter
            setOnItemClickListener { _, _, position, _ ->
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java)
                    .putExtra(PlayerActivity.VIDEO_EXTRA, sounds[position].videoResId))
            }
        }
        root.addView(search, LinearLayout.LayoutParams(-1, -2))
        root.addView(toggle, LinearLayout.LayoutParams(-1, -2))
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        search.setText(savedInstanceState?.getString("query").orEmpty())
        searchInput = search
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", searchInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class SoundAdapter : BaseAdapter() {
        override fun getCount() = sounds.size
        override fun getItem(position: Int) = sounds[position]
        override fun getItemId(position: Int) = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tile = convertView as? FrameLayout ?: FrameLayout(this@MainActivity).apply {
                addView(ImageView(context).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                }, FrameLayout.LayoutParams(-1, dp(150)))
                addView(TextView(context).apply {
                    gravity = Gravity.CENTER
                    setTextColor(Color.WHITE)
                    setBackgroundColor(0x99000000.toInt())
                    setPadding(dp(4), dp(4), dp(4), dp(4))
                }, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            }
            val sound = getItem(position)
            (tile.getChildAt(0) as ImageView).setImageResource(sound.thumbnailResId)
            (tile.getChildAt(1) as TextView).text = sound.name
            return tile
        }
    }

    companion object {
        const val SOUND_PREFS = "soundboard"
        const val KEY_SOUND_ENABLED = "sound_enabled"
    }
}
