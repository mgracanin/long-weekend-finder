package com.longweekend.optimizer.ui.report

import android.graphics.Paint
import android.graphics.Typeface

// A4 in PDF points, shared by every document the app makes
internal const val PAGE_W = 595
internal const val PAGE_H = 842
internal const val MARGIN = 40f
internal const val FOOTER_H = 36f

// Same calm palette as the light theme; it prints well in colour and in greyscale
internal const val INK = 0xFF1C2422.toInt()
internal const val INK_SOFT = 0xFF55605E.toInt()
internal const val TEAL = 0xFF1F5C55.toInt()
internal const val TEAL_SOFT = 0xFFCDEBE5.toInt()
internal const val APRICOT = 0xFFFCE3B4.toInt()
internal const val APRICOT_LINE = 0xFFE0A84A.toInt()
internal const val APRICOT_INK = 0xFF5C3A00.toInt()
internal const val WEEKEND = 0xFFEDF0EF.toInt()
internal const val TILE = 0xFFF3F5F4.toInt()
internal const val LINE = 0xFFE2E6E4.toInt()
internal const val WARN_BG = 0xFFFDF0DA.toInt()

internal fun pdfPaint(size: Float, color: Int = INK, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    textSize = size
    this.color = color
    typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
}

/** [text] cut to fit [width], ending in an ellipsis when it had to be shortened. */
internal fun Paint.fit(text: String, width: Float): String {
    if (measureText(text) <= width) return text
    var end = text.length
    while (end > 0 && measureText(text, 0, end) + measureText("…") > width) end--
    return text.substring(0, end).trimEnd() + "…"
}
