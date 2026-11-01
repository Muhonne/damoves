package com.example.soundboard

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private lateinit var allMoves: List<Move>
    private var moves = emptyList<Move>()
    private lateinit var adapter: MoveAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchInput: EditText
    private lateinit var clearSearch: ImageButton
    private lateinit var soundToggle: ImageButton
    private lateinit var emptyState: View
    private val thumbnails = mutableMapOf<Move, Bitmap>()
    private val thumbnailsLoading = java.util.Collections.synchronizedSet(mutableSetOf<Move>())
    private val thumbnailWorker = Executors.newFixedThreadPool(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allMoves = loadMoves()
        moves = allMoves
        adapter = MoveAdapter()
        val root = FrameLayout(this).apply {
            setBackgroundColor(getColor(R.color.surface))
            applySystemInsets()
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val headerSection = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), 0, dp(16), dp(12))
            setBackgroundColor(getColor(R.color.surface))
        }
        val appBar = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
        }
        appBar.addView(TextView(this).apply {
            setText(R.string.app_name)
            textSize = 20f
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(getColor(R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        soundToggle = ImageButton(this).apply {
            id = R.id.soundToggle
            background = borderlessRipple()
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setOnClickListener { toggleSound() }
        }
        updateSoundIcon(getSharedPreferences(SOUND_PREFS, MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true))
        appBar.addView(soundToggle, LinearLayout.LayoutParams(dp(48), dp(48)))
        appBar.addView(iconButton(R.drawable.ic_add, R.string.add_clip_content_description) {
            openDocumentPicker()
        }.apply { id = R.id.addClip }, LinearLayout.LayoutParams(dp(48), dp(48)))
        val searchContainer = LinearLayout(this).apply {
            id = R.id.searchContainer
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = getDrawable(R.drawable.rounded_search_container)
        }
        searchContainer.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_search)
            imageTintList = getColorStateList(R.color.text_secondary)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(24), dp(24)).apply {
            marginStart = dp(16)
            marginEnd = dp(8)
        })
        searchInput = EditText(this).apply {
            id = R.id.searchInput
            hint = getString(R.string.search_hint)
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setSingleLine()
            textSize = 16f
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            background = null
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    moves = filterMoves(allMoves, s.toString())
                    adapter.submitList(moves)
                    updateEmptyState()
                    clearSearch.visibility = if (s.isNullOrBlank()) View.GONE else View.VISIBLE
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        searchContainer.addView(searchInput, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        clearSearch = iconButton(R.drawable.ic_clear, R.string.clear_search) {
            searchInput.setText("")
        }.apply {
            id = R.id.clearSearch
            visibility = View.GONE
        }
        searchContainer.addView(clearSearch, LinearLayout.LayoutParams(dp(40), dp(40)))
        headerSection.addView(appBar)
        headerSection.addView(searchContainer, LinearLayout.LayoutParams(-1, dp(48)).apply {
            topMargin = dp(12)
        })
        content.addView(headerSection, LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(View(this).apply {
            setBackgroundColor(getColor(R.color.outline))
        }, LinearLayout.LayoutParams(-1, 1))
        val gridContainer = FrameLayout(this).apply {
            clipChildren = true
        }
        recyclerView = RecyclerView(this).apply {
            id = R.id.moveGrid
            layoutManager = GridLayoutManager(this@MainActivity, 3)
            adapter = this@MainActivity.adapter
            setPadding(dp(10), dp(6), dp(10), dp(88))
            clipToPadding = false
        }
        gridContainer.addView(recyclerView, FrameLayout.LayoutParams(-1, -1))
        emptyState = LinearLayout(this).apply {
            id = R.id.emptyState
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            visibility = View.GONE
            addView(ImageView(this@MainActivity).apply {
                setImageResource(R.drawable.ic_search)
                imageTintList = getColorStateList(R.color.text_secondary)
                alpha = 0.4f
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(64), dp(64)))
            addView(TextView(this@MainActivity).apply {
                setText(R.string.no_moves_found)
                textSize = 16f
                gravity = Gravity.CENTER
                setTextColor(getColor(R.color.text_secondary))
            }, LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(12)
            })
            addView(MaterialButton(this@MainActivity).apply {
                id = R.id.clearSearchButton
                setText(R.string.clear_search)
                setOnClickListener { searchInput.setText("") }
            }, LinearLayout.LayoutParams(-2, -2).apply {
                topMargin = dp(16)
            })
        }
        gridContainer.addView(emptyState, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        content.addView(gridContainer, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(content, FrameLayout.LayoutParams(-1, -1))
        val dice = FloatingActionButton(this).apply {
            id = R.id.dice
            setImageDrawable(getDrawable(R.drawable.ic_dice))
            imageTintList = getColorStateList(R.color.on_primary_container)
            backgroundTintList = getColorStateList(R.color.primary_container)
            contentDescription = getString(R.string.play_random_move)
            setOnClickListener { allMoves.randomOrNull()?.let { openMove(it, loop = true) } }
        }
        root.addView(dice, FrameLayout.LayoutParams(dp(56), dp(56), Gravity.BOTTOM or Gravity.END).apply {
            setMargins(dp(16), dp(16), dp(16), dp(16))
        })
        setContentView(root)
        searchInput.setText(savedInstanceState?.getString("query").orEmpty())
    }

    override fun onResume() {
        super.onResume()
        if (::searchInput.isInitialized) reloadMoves()
    }

    private fun reloadMoves() {
        allMoves = loadMoves()
        moves = filterMoves(allMoves, searchInput.text.toString())
        adapter.submitList(moves)
        updateEmptyState()
        loadThumbnails()
    }

    private fun updateEmptyState() {
        emptyState.visibility = if (moves.isEmpty()) View.VISIBLE else View.GONE
    }

    @Deprecated("Platform callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data?.data != null) {
            startActivity(Intent(this, ImportActivity::class.java).setData(data.data)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("query", searchInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    private fun openMove(move: Move, loop: Boolean = false) {
        startActivity(Intent(this, PlayerActivity::class.java)
            .putExtra("uri", move.uri)
            .putExtra(PlayerActivity.VIDEO_EXTRA, move.videoResId)
            .putExtra(PlayerActivity.LOOP_EXTRA, loop))
    }

    private fun openDocumentPicker() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }, 100)
    }

    private fun toggleSound() {
        val preferences = getSharedPreferences(SOUND_PREFS, MODE_PRIVATE)
        val enabled = !preferences.getBoolean(KEY_SOUND_ENABLED, true)
        preferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        updateSoundIcon(enabled)
    }

    private fun updateSoundIcon(enabled: Boolean) {
        soundToggle.setImageResource(if (enabled) R.drawable.ic_volume_up else R.drawable.ic_volume_off)
        soundToggle.contentDescription = getString(if (enabled) R.string.sound_on else R.string.sound_off)
    }

    private fun loadMoves(): List<Move> {
        val renames = getSharedPreferences(RENAMES_PREFS, MODE_PRIVATE)
        val bundled = discoverMoves(resources).map { move ->
            val renamed = renames.getString(move.videoResId.toString(), null)
            if (renamed != null) Move(renamed, move.videoResId) else move
        }
        return bundled + SavedMoves.load(this)
    }

    private fun showMoveOptions(move: Move) {
        val sheet = BottomSheetDialog(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(24))
        }
        content.addView(TextView(this).apply {
            setText(move.name)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(getColor(R.color.text_primary))
            setPadding(dp(8), dp(16), dp(8), dp(8))
        })
        content.addView(sheetRow(R.drawable.ic_edit, R.string.rename, R.color.text_primary) {
            sheet.dismiss()
            showRenameDialog(move)
        })
        val uri = move.uri
        if (uri != null) {
            content.addView(sheetRow(R.drawable.ic_cut, R.string.edit_clip, R.color.text_primary) {
                sheet.dismiss()
                startActivity(Intent(this, ImportActivity::class.java)
                    .setData(Uri.parse(uri))
                    .putExtra(ImportActivity.NAME_EXTRA, move.name))
            })
            content.addView(sheetRow(R.drawable.ic_delete, R.string.delete_from_damoves, R.color.danger) {
                sheet.dismiss()
                confirmDelete(move)
            })
        }
        sheet.setContentView(content)
        sheet.show()
    }

    private fun showRenameDialog(move: Move) {
        val input = EditText(this).apply {
            id = R.id.renameInput
            setText(move.name)
            setSelection(text.length)
            setSingleLine()
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
        }
        val container = FrameLayout(this).apply {
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(input, FrameLayout.LayoutParams(-1, -2))
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.rename_clip)
            .setView(container)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) renameMove(move, name)
            }
            .show()
    }

    private fun renameMove(move: Move, name: String) {
        val uri = move.uri
        if (uri != null) {
            val id = importedMoveId(uri) ?: return
            getSharedPreferences("imports", MODE_PRIVATE).edit().putString(id, name).apply()
        } else {
            getSharedPreferences(RENAMES_PREFS, MODE_PRIVATE).edit()
                .putString(move.videoResId.toString(), name)
                .apply()
        }
        reloadMoves()
    }

    private fun sheetRow(icon: Int, label: Int, tint: Int, onClick: () -> Unit) =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(8), 0, dp(8), 0)
            background = boundedRipple()
            isClickable = true
            addView(ImageView(this@MainActivity).apply {
                setImageResource(icon)
                imageTintList = getColorStateList(tint)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }, LinearLayout.LayoutParams(dp(24), dp(24)).apply {
                marginEnd = dp(16)
            })
            addView(TextView(this@MainActivity).apply {
                setText(label)
                textSize = 16f
                setTextColor(getColor(tint))
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            setOnClickListener { onClick() }
        }

    private fun confirmDelete(move: Move) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_clip_title)
            .setMessage(R.string.delete_clip_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete_clip) { _, _ -> deleteMove(move) }
            .show()
    }

    private fun deleteMove(move: Move) {
        val uri = move.uri ?: return
        val deleted = SavedMoves.delete(this, uri)
        window.decorView.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.LONG_PRESS)
        Toast.makeText(this,
            if (deleted) "Clip deleted" else "Could not delete this clip",
            Toast.LENGTH_SHORT).show()
        reloadMoves()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun borderlessRipple(): Drawable {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, value, true)
        return getDrawable(value.resourceId)!!
    }

    private fun boundedRipple(): Drawable {
        val value = TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, value, true)
        return getDrawable(value.resourceId)!!
    }

    private fun iconButton(icon: Int, description: Int, onClick: () -> Unit) = ImageButton(this).apply {
        setImageResource(icon)
        imageTintList = getColorStateList(R.color.text_primary)
        background = borderlessRipple()
        scaleType = ImageView.ScaleType.CENTER_INSIDE
        contentDescription = getString(description)
        setOnClickListener { onClick() }
    }

    private fun loadThumbnails() {
        for (move in allMoves) {
            if (thumbnails.containsKey(move) || !thumbnailsLoading.add(move)) continue
            thumbnailWorker.execute { loadOrExtractThumbnail(move) }
        }
    }

    private fun loadOrExtractThumbnail(move: Move) {
        if (Thread.currentThread().isInterrupted) return
        try {
            val cached = ThumbnailStore.load(this, move)
            if (cached != null) {
                publishThumbnail(move, cached)
                return
            }
            val retriever = MediaMetadataRetriever()
            try {
                if (move.uri != null) retriever.setDataSource(this, Uri.parse(move.uri))
                else resources.openRawResourceFd(move.videoResId).use { descriptor ->
                    retriever.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
                }
                val frame = if (Build.VERSION.SDK_INT >= 27) {
                    retriever.getScaledFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 360, 640)
                } else {
                    retriever.getFrameAtTime(500_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }
                if (frame != null) {
                    ThumbnailStore.save(this, move, frame)
                    publishThumbnail(move, frame)
                }
            } finally {
                retriever.release()
            }
        } catch (exception: Exception) {
            Log.w("Damoves", "Cannot load thumbnail for ${move.name}", exception)
        } finally {
            thumbnailsLoading.remove(move)
        }
    }

    private fun publishThumbnail(move: Move, bitmap: Bitmap) {
        runOnUiThread {
            if (isDestroyed) return@runOnUiThread
            thumbnails[move] = bitmap
            val position = adapter.currentList.indexOf(move)
            if (position >= 0) adapter.notifyItemChanged(position)
        }
    }

    override fun onDestroy() {
        thumbnailWorker.shutdownNow()
        super.onDestroy()
    }

    private inner class MoveAdapter : ListAdapter<Move, MoveAdapter.MoveTileViewHolder>(MoveDiff) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoveTileViewHolder {
            val tile = MoveTileView(parent.context)
            tile.layoutParams = RecyclerView.LayoutParams(-1, -2).apply {
                setMargins(dp(6), dp(6), dp(6), dp(6))
            }
            return MoveTileViewHolder(tile)
        }

        override fun onBindViewHolder(holder: MoveTileViewHolder, position: Int) {
            val move = getItem(position)
            holder.tile.bind(thumbnails[move], move.name, move.uri != null)
            holder.tile.setOnClickListener { openMove(move) }
            holder.tile.setOnLongClickListener {
                showMoveOptions(move)
                true
            }
        }

        inner class MoveTileViewHolder(val tile: MoveTileView) : RecyclerView.ViewHolder(tile)
    }

    companion object {
        const val SOUND_PREFS = "soundboard"
        const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val RENAMES_PREFS = "renames"

        private val MoveDiff = object : DiffUtil.ItemCallback<Move>() {
            override fun areItemsTheSame(oldItem: Move, newItem: Move) =
                oldItem.videoResId == newItem.videoResId && oldItem.uri == newItem.uri
            override fun areContentsTheSame(oldItem: Move, newItem: Move) = oldItem == newItem
        }
    }
}
