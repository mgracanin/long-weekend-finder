package com.longweekend.optimizer.ui.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import java.io.File
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

/**
 * Draws [HolidayCalendar] as an A4 PDF. Page 1 is the whole year with each month's holidays named
 * under it, readable on screen and on a printout pinned to the wall; the full list follows.
 */
object HolidayCalendarPdf {
    private const val COLUMNS = 4
    private const val GAP_X = 16f
    private const val GAP_Y = 16f
    private const val NOTE_LINE = 9f
    // Enough for every month of most countries; busier months end in "+N more"
    private const val MAX_NOTES = 5
    private const val ROW_H = 31f
    // Month name and weekday initials above each grid
    private const val MONTH_HEAD = 26.5f
    private const val LEGEND_H = 24f

    fun write(calendar: HolidayCalendar, file: File) {
        val doc = PdfDocument()
        val pen = Pen(doc, calendar)
        pen.header()
        pen.months()
        pen.legend()
        pen.list()
        pen.finish()
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private class Pen(private val doc: PdfDocument, private val r: HolidayCalendar) {
        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var c: Canvas
        private var y = MARGIN
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = APRICOT_LINE }
        private val byDate = r.holidays.groupBy { it.date }

        init { newPage() }

        private fun newPage() {
            if (pageNo > 0) closePage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas
            y = MARGIN
        }

        private fun closePage() {
            val p = pdfPaint(8f, INK_SOFT)
            val fy = PAGE_H - MARGIN / 2
            c.drawLine(MARGIN, fy - 14, PAGE_W - MARGIN, fy - 14, pdfPaint(1f, LINE))
            c.drawText(r.footer, MARGIN, fy, p)
            val label = r.pageLabel(pageNo)
            c.drawText(label, PAGE_W - MARGIN - p.measureText(label), fy, p)
            doc.finishPage(page)
        }

        fun finish() = closePage()

        private fun ensure(height: Float) {
            if (y + height > PAGE_H - MARGIN - FOOTER_H) newPage()
        }

        private val contentW = PAGE_W - 2 * MARGIN

        fun header() {
            val title = pdfPaint(24f, TEAL, bold = true)
            y += title.textSize
            c.drawText(title.fit(r.title, contentW), MARGIN, y, title)
            val sub = pdfPaint(11f, INK_SOFT)
            y += 8 + sub.textSize
            c.drawText(sub.fit(r.subtitle, contentW), MARGIN, y, sub)
            y += 12
            fill.color = TEAL
            c.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 2f, fill)
            y += 16
            r.notice?.let { notice ->
                val p = pdfPaint(9f, APRICOT_INK)
                fill.color = WARN_BG
                c.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + 22), 6f, 6f, fill)
                c.drawText(p.fit(notice, contentW - 20), MARGIN + 10, y + 14.5f, p)
                y += 22 + 14
            }
        }

        /** The twelve months, four across, each with its holidays named under the grid. */
        fun months() {
            val monthW = (contentW - GAP_X * (COLUMNS - 1)) / COLUMNS
            val cell = monthW / 7
            val months = (1..12).map { YearMonth.of(r.year, it) }
            val offsets = months.map { (it.atDay(1).dayOfWeek.value - r.firstWorkday.value + 7) % 7 }
            val weeks = months.indices.map { (offsets[it] + months[it].lengthOfMonth() + 6) / 7 }
            val notes = months.map { monthNotes(r.holidays, it, MAX_NOTES) }
            val noteLines = notes.map { (shown, more) -> shown.size + if (more > 0) 1 else 0 }

            // Days are as tall as the page allows, so a printout of a quiet year isn't half empty
            fun rowHeight(row: Int, cellH: Float) = (0 until COLUMNS).maxOf { col ->
                val m = row * COLUMNS + col
                MONTH_HEAD + weeks[m] * cellH + 6 + noteLines[m] * NOTE_LINE
            }
            val room = PAGE_H - MARGIN - FOOTER_H - y - LEGEND_H
            val cellH = listOf(1.35f, 1.25f, 1.15f, 1.05f).map { cell * it }
                .firstOrNull { h -> (0 until 12 / COLUMNS).map { rowHeight(it, h) + GAP_Y }.sum() <= room } ?: cell
            val scale = minOf(cellH / cell, 1.2f)

            val days = (0L..6L).map { r.firstWorkday.plus(it) }
            val initials = days.map { it.getDisplayName(TextStyle.NARROW, r.locale).uppercase(r.locale) }
            val monthName = DateTimeFormatter.ofPattern("LLLL", r.locale)
            val namePaint = pdfPaint(10.5f, INK, bold = true)
            val headPaint = pdfPaint(6.5f, INK_SOFT, bold = true)
            val dayPaint = pdfPaint(7.5f * scale, INK)
            val holidayDay = pdfPaint(7.5f * scale, APRICOT_INK, bold = true)
            val regionalDay = pdfPaint(7.5f * scale, INK, bold = true)
            val noteDay = pdfPaint(7f, INK, bold = true)
            val noteName = pdfPaint(7f, INK)
            val noteSoft = pdfPaint(7f, INK_SOFT)
            val dayW = noteDay.measureText("31") + 5

            for (row in 0 until 12 / COLUMNS) {
                val rowTop = y
                for (col in 0 until COLUMNS) {
                    val m = row * COLUMNS + col
                    val ym = months[m]
                    val x0 = MARGIN + col * (monthW + GAP_X)
                    var my = rowTop + namePaint.textSize
                    val name = ym.atDay(1).format(monthName).replaceFirstChar { it.titlecase(r.locale) }
                    c.drawText(name, x0 + 1, my, namePaint)
                    my += 5
                    initials.forEachIndexed { i, label ->
                        headPaint.color = if (days[i] in r.weekend) INK_SOFT else INK
                        c.drawText(label, x0 + i * cell + (cell - headPaint.measureText(label)) / 2, my + 8, headPaint)
                    }
                    my += 11

                    for (d in 1..ym.lengthOfMonth()) {
                        val idx = offsets[m] + d - 1
                        val cx = x0 + (idx % 7) * cell
                        val cy = my + (idx / 7) * cellH
                        val date = ym.atDay(d)
                        val holidays = byDate[date].orEmpty()
                        val national = holidays.any { it.national }
                        val box = RectF(cx + 0.9f, cy + 0.9f, cx + cell - 0.9f, cy + cellH - 0.9f)
                        when {
                            national -> { fill.color = APRICOT; c.drawRoundRect(box, 3f, 3f, fill) }
                            date.dayOfWeek in r.weekend -> { fill.color = WEEKEND; c.drawRoundRect(box, 3f, 3f, fill) }
                        }
                        // A regional holiday is outlined, so it reads as "only in some places"
                        if (holidays.isNotEmpty() && !national) {
                            box.inset(0.5f, 0.5f)
                            c.drawRoundRect(box, 3f, 3f, outline)
                        }
                        val p = when {
                            national -> holidayDay
                            holidays.isNotEmpty() -> regionalDay
                            else -> dayPaint
                        }
                        val t = "$d"
                        c.drawText(t, cx + (cell - p.measureText(t)) / 2, cy + cellH / 2 + p.textSize * 0.36f, p)
                    }
                    my += weeks[m] * cellH + 6

                    val (shown, more) = notes[m]
                    for (h in shown) {
                        my += NOTE_LINE
                        noteDay.color = if (h.national) APRICOT_INK else INK_SOFT
                        c.drawText("${h.date.dayOfMonth}", x0 + 1, my, noteDay)
                        val p = if (h.national) noteName else noteSoft
                        c.drawText(p.fit(h.name, monthW - dayW - 1), x0 + 1 + dayW, my, p)
                    }
                    if (more > 0) {
                        my += NOTE_LINE
                        c.drawText(r.moreLabel(more), x0 + 1 + dayW, my, noteSoft)
                    }
                }
                y = rowTop + rowHeight(row, cellH) + GAP_Y
            }
        }

        fun legend() {
            val p = pdfPaint(8.5f, INK_SOFT)
            var x = MARGIN
            val items = listOf(r.legend.first, r.legend.second, r.legend.third)
            items.forEachIndexed { i, label ->
                val box = RectF(x, y, x + 10, y + 10)
                when (i) {
                    0 -> { fill.color = APRICOT; c.drawRoundRect(box, 2f, 2f, fill) }
                    1 -> { box.inset(0.5f, 0.5f); c.drawRoundRect(box, 2f, 2f, outline) }
                    else -> { fill.color = WEEKEND; c.drawRoundRect(box, 2f, 2f, fill) }
                }
                c.drawText(label, x + 14, y + 8.5f, p)
                x += 14 + p.measureText(label) + 18
            }
            y += LEGEND_H
        }

        /** Every holiday with both names, where it applies and whether it falls on a weekend. */
        fun list() {
            if (r.holidays.isEmpty()) return
            newPage()
            val title = pdfPaint(16f, TEAL, bold = true)
            y += title.textSize
            c.drawText(title.fit(r.listTitle, contentW), MARGIN, y, title)
            y += 14

            val monthName = DateTimeFormatter.ofPattern("LLLL", r.locale)
            val monthPaint = pdfPaint(9.5f, TEAL, bold = true)
            val dayNumber = pdfPaint(12f, INK, bold = true)
            val weekday = pdfPaint(6.5f, INK_SOFT, bold = true)
            val namePaint = pdfPaint(10.5f, INK, bold = true)
            val detail = pdfPaint(8.5f, INK_SOFT)
            val tagPaint = pdfPaint(7.5f, INK_SOFT, bold = true)

            r.holidays.sortedWith(compareBy<CalendarHoliday> { it.date }.thenBy { !it.national })
                .groupBy { YearMonth.from(it.date) }
                .forEach { (ym, holidays) ->
                    // Keep a month's heading with its first holiday
                    ensure(20 + ROW_H)
                    y += 6
                    val name = ym.atDay(1).format(monthName).replaceFirstChar { it.titlecase(r.locale) }
                    c.drawText(name, MARGIN, y + monthPaint.textSize, monthPaint)
                    y += monthPaint.textSize + 4
                    c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, pdfPaint(1f, LINE))
                    y += 4

                    holidays.forEach { h ->
                        ensure(ROW_H)
                        val badge = RectF(MARGIN, y + 1, MARGIN + 34, y + ROW_H - 3)
                        if (h.national) {
                            fill.color = APRICOT
                            c.drawRoundRect(badge, 5f, 5f, fill)
                        } else {
                            c.drawRoundRect(badge, 5f, 5f, outline)
                        }
                        val day = "${h.date.dayOfMonth}"
                        dayNumber.color = if (h.national) APRICOT_INK else INK
                        c.drawText(day, badge.centerX() - dayNumber.measureText(day) / 2, badge.top + 13.5f, dayNumber)
                        val wd = h.date.dayOfWeek.getDisplayName(TextStyle.SHORT, r.locale).uppercase(r.locale)
                        c.drawText(wd, badge.centerX() - weekday.measureText(wd) / 2, badge.bottom - 4, weekday)

                        val tagW = h.tag?.let { tagPaint.measureText(it) + 14 } ?: 0f
                        val textX = MARGIN + 46
                        val textW = PAGE_W - MARGIN - textX - tagW - 8
                        c.drawText(namePaint.fit(h.name, textW), textX, y + 12.5f, namePaint)
                        val second = listOfNotNull(h.otherName, h.regions).joinToString(" · ")
                        if (second.isNotEmpty()) c.drawText(detail.fit(second, textW), textX, y + 24.5f, detail)

                        h.tag?.let { tag ->
                            val pill = RectF(PAGE_W - MARGIN - tagW, y + 5, PAGE_W - MARGIN, y + 19)
                            fill.color = TILE
                            c.drawRoundRect(pill, 7f, 7f, fill)
                            c.drawText(tag, pill.left + 7, pill.bottom - 4.5f, tagPaint)
                        }
                        y += ROW_H
                    }
                }
        }
    }
}
