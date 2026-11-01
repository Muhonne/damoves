package com.example.soundboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.card.MaterialCardView

internal class MoveTileView(context: Context) : MaterialCardView(context) {
    private val thumbnailView: ImageView
    private val labelView: TextView
    private val badgeView: TextView

    init {
        radius = dp(12).toFloat()
        setCardBackgroundColor(context.getColor(R.color.surface_container))
        strokeWidth = dp(1)
        strokeColor = context.getColor(R.color.outline)
        cardElevation = 0f

        thumbnailView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(context.getColor(R.color.surface_container_high))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(thumbnailView, FrameLayout.LayoutParams(-1, -1))

        labelView = TextView(context).apply {
            background = context.getDrawable(R.drawable.scrim_bottom)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(context.getColor(R.color.text_primary))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setPadding(dp(10), dp(48), dp(10), dp(10))
            gravity = Gravity.CENTER_VERTICAL
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(labelView, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))

        badgeView = TextView(context).apply {
            setText(R.string.imported_badge)
            textSize = 12f
            setTextColor(context.getColor(R.color.on_primary_container))
            background = context.getDrawable(R.drawable.rounded_badge)
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(8), dp(2), dp(8), dp(2))
            visibility = View.GONE
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        addView(badgeView, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            setMargins(dp(8), dp(8), dp(8), 0)
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(
            widthMeasureSpec,
            View.MeasureSpec.makeMeasureSpec((width * 12f / 9f).toInt(), View.MeasureSpec.EXACTLY),
        )
    }

    fun bind(thumbnail: Bitmap?, name: String, imported: Boolean) {
        thumbnailView.setImageBitmap(thumbnail)
        labelView.text = name
        badgeView.visibility = if (imported) View.VISIBLE else View.GONE
        contentDescription =
            if (imported) "$name, ${context.getString(R.string.imported_badge)}" else name
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
