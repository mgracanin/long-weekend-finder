package com.longweekend.optimizer.ui.report

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** One saved plan, already worded in the app's language. */
data class ReportPlan(
    val title: String,
    val range: String,
    val summary: String,
    val leaveHeading: String,
    /** Each leave day as text, with whether it has been requested from the employer. */
    val leaveDays: List<Pair<String, Boolean>>,
    val holidays: String?,
    val leaveDates: Set<LocalDate>,
    val holidayDates: Set<LocalDate>
)

/** Everything the yearly report shows; all text is already translated. */
data class YearReport(
    val year: Int,
    val locale: Locale,
    val title: String,
    val subtitle: String,
    val tiles: List<Pair<String, String>>,
    val warnings: List<String>,
    val calendarTitle: String,
    val legend: Triple<String, String, String>,
    val plansTitle: String,
    val plans: List<ReportPlan>,
    /** National public holidays of the year, for the calendar. */
    val publicHolidays: Set<LocalDate>,
    val weekend: Set<DayOfWeek>,
    val firstWorkday: DayOfWeek,
    val footer: String,
    val pageLabel: (Int) -> String
)

/**
 * Draws [YearReport] as an A4 PDF with Android's own PdfDocument: nothing leaves the phone and no
 * extra library is needed. Page 1 holds the summary and the year calendar; plans follow.
 */
object YearReportPdf {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 40f
    private const val FOOTER_H = 36f

    // Same calm palette as the light theme
    private const val INK = 0xFF1C2422.toInt()
    private const val INK_SOFT = 0xFF55605E.toInt()
    private const val TEAL = 0xFF1F5C55.toInt()
    private const val TEAL_SOFT = 0xFFCDEBE5.toInt()
    private const val APRICOT = 0xFFFCE3B4.toInt()
    private const val APRICOT_INK = 0xFF5C3A00.toInt()
    private const val WEEKEND = 0xFFEDF0EF.toInt()
    private const val TILE = 0xFFF3F5F4.toInt()
    private const val LINE = 0xFFE2E6E4.toInt()
    private const val WARN_BG = 0xFFFDF0DA.toInt()

    fun write(report: YearReport, file: File) {
        val doc = PdfDocument()
        val pen = Pen(doc, report)
        pen.header()
        pen.tiles()
        pen.warnings()
        pen.calendar()
        pen.plans()
        pen.finish()
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
    }

    private class Pen(private val doc: PdfDocument, private val r: YearReport) {
        private var pageNo = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var c: Canvas
        private var y = MARGIN

        private fun paint(size: Float, color: Int = INK, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        init { newPage() }

        private fun newPage() {
            if (pageNo > 0) closePage()
            pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas
            y = MARGIN
        }

        private fun closePage() {
            val p = paint(8f, INK_SOFT)
            val fy = PAGE_H - MARGIN / 2
            c.drawLine(MARGIN, fy - 14, PAGE_W - MARGIN, fy - 14, paint(1f, LINE))
            c.drawText(r.footer, MARGIN, fy, p)
            val label = r.pageLabel(pageNo)
            c.drawText(label, PAGE_W - MARGIN - p.measureText(label), fy, p)
            doc.finishPage(page)
        }

        fun finish() = closePage()

        /** Starts a new page when [height] more points won't fit above the footer. */
        private fun ensure(height: Float) {
            if (y + height > PAGE_H - MARGIN - FOOTER_H) newPage()
        }

        private fun wrap(text: String, p: Paint, width: Float): List<String> {
            val lines = mutableListOf<String>()
            var line = ""
            for (word in text.split(" ")) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (p.measureText(candidate) <= width || line.isEmpty()) line = candidate
                else { lines += line; line = word }
            }
            if (line.isNotEmpty()) lines += line
            return lines
        }

        private fun textBlock(text: String, p: Paint, x: Float = MARGIN, width: Float = PAGE_W - 2 * MARGIN, gap: Float = 4f) {
            for (line in wrap(text, p, width)) {
                ensure(p.textSize + gap)
                y += p.textSize
                c.drawText(line, x, y, p)
                y += gap
            }
        }

        fun header() {
            textBlock(r.title, paint(22f, TEAL, bold = true), gap = 6f)
            textBlock(r.subtitle, paint(10f, INK_SOFT))
            y += 14
        }

        fun tiles() {
            val gap = 10f
            val w = (PAGE_W - 2 * MARGIN - gap * (r.tiles.size - 1)) / r.tiles.size
            val h = 58f
            r.tiles.forEachIndexed { i, (label, value) ->
                val x = MARGIN + i * (w + gap)
                fill.color = TILE
                c.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, fill)
                c.drawText(value, x + 10, y + 28, paint(18f, TEAL, bold = true))
                c.drawText(label, x + 10, y + 46, paint(8.5f, INK_SOFT))
            }
            y += h + 16
        }

        fun warnings() {
            if (r.warnings.isEmpty()) return
            val p = paint(9.5f, APRICOT_INK)
            val width = PAGE_W - 2 * MARGIN - 20
            val lines = r.warnings.flatMap { wrap("• $it", p, width) }
            val h = lines.size * (p.textSize + 4) + 14
            fill.color = WARN_BG
            c.drawRoundRect(RectF(MARGIN, y, PAGE_W - MARGIN, y + h), 8f, 8f, fill)
            var ty = y + 7
            lines.forEach { ty += p.textSize; c.drawText(it, MARGIN + 10, ty, p); ty += 4 }
            y += h + 16
        }

        /** Twelve small months, 4 across, so the whole year fits on page 1 under the summary. */
        fun calendar() {
            textBlock(r.calendarTitle, paint(13f, INK, bold = true))
            y += 6
            val cols = 4
            val gapX = 14f
            val monthW = (PAGE_W - 2 * MARGIN - gapX * (cols - 1)) / cols
            val cell = monthW / 7
            val leave = r.plans.flatMap { it.leaveDates }.toSet()
            val days = (0L..6L).map { r.firstWorkday.plus(it) }
            val initials = days.map { it.getDisplayName(TextStyle.NARROW, r.locale).uppercase(r.locale) }
            val monthName = DateTimeFormatter.ofPattern("LLLL", r.locale)
            val dayPaint = paint(7.5f, INK)
            val boldDay = paint(7.5f, INK, bold = true)
            val headPaint = paint(7f, INK_SOFT, bold = true)

            for (row in 0 until 12 / cols) {
                val rowTop = y
                var rowHeight = 0f
                for (col in 0 until cols) {
                    val ym = YearMonth.of(r.year, row * cols + col + 1)
                    val x0 = MARGIN + col * (monthW + gapX)
                    var my = rowTop
                    val name = ym.atDay(1).format(monthName).replaceFirstChar { it.titlecase(r.locale) }
                    my += 10
                    c.drawText(name, x0, my, paint(9.5f, INK, bold = true))
                    my += 6
                    initials.forEachIndexed { i, label ->
                        val tw = headPaint.measureText(label)
                        c.drawText(label, x0 + i * cell + (cell - tw) / 2, my + 8, headPaint)
                    }
                    my += 11
                    val offset = (ym.atDay(1).dayOfWeek.value - r.firstWorkday.value + 7) % 7
                    for (d in 1..ym.lengthOfMonth()) {
                        val idx = offset + d - 1
                        val cx = x0 + (idx % 7) * cell
                        val cy = my + (idx / 7) * cell
                        val date = ym.atDay(d)
                        val bg = when {
                            date in leave -> TEAL_SOFT
                            date in r.publicHolidays -> APRICOT
                            date.dayOfWeek in r.weekend -> WEEKEND
                            else -> null
                        }
                        if (bg != null) {
                            fill.color = bg
                            c.drawRoundRect(RectF(cx + 0.8f, cy + 0.8f, cx + cell - 0.8f, cy + cell - 0.8f), 2.5f, 2.5f, fill)
                        }
                        val p = if (date in leave || date in r.publicHolidays) boldDay else dayPaint
                        val t = "$d"
                        c.drawText(t, cx + (cell - p.measureText(t)) / 2, cy + cell / 2 + 2.7f, p)
                    }
                    val weeks = (offset + ym.lengthOfMonth() + 6) / 7
                    rowHeight = maxOf(rowHeight, my + weeks * cell - rowTop)
                }
                y = rowTop + rowHeight + 12
            }
            legend()
        }

        private fun legend() {
            val p = paint(8.5f, INK_SOFT)
            var x = MARGIN
            listOf(TEAL_SOFT to r.legend.first, APRICOT to r.legend.second, WEEKEND to r.legend.third).forEach { (color, label) ->
                fill.color = color
                c.drawRoundRect(RectF(x, y, x + 10, y + 10), 2f, 2f, fill)
                c.drawText(label, x + 14, y + 8.5f, p)
                x += 14 + p.measureText(label) + 18
            }
            y += 24
        }

        fun plans() {
            if (r.plans.isEmpty()) return
            newPage()
            textBlock(r.plansTitle, paint(16f, TEAL, bold = true), gap = 10f)
            val body = paint(9.5f, INK)
            val soft = paint(9f, INK_SOFT)
            r.plans.forEach { plan ->
                // Keep a plan's heading with at least its first lines
                ensure(70f)
                textBlock(plan.title, paint(12.5f, INK, bold = true), gap = 3f)
                textBlock(plan.range, paint(10f, TEAL, bold = true), gap = 3f)
                textBlock(plan.summary, soft, gap = 8f)
                textBlock(plan.leaveHeading, paint(9.5f, INK, bold = true), gap = 5f)
                plan.leaveDays.forEach { (day, requested) ->
                    ensure(16f)
                    val box = RectF(MARGIN + 4, y + 2, MARGIN + 13, y + 11)
                    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = INK_SOFT }
                    c.drawRoundRect(box, 2f, 2f, stroke)
                    if (requested) {
                        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1.6f; color = TEAL }
                        c.drawLine(box.left + 2, box.centerY(), box.left + 4, box.bottom - 2, tick)
                        c.drawLine(box.left + 4, box.bottom - 2, box.right - 1.5f, box.top + 1.5f, tick)
                    }
                    c.drawText(day, MARGIN + 20, y + 10, body)
                    y += 15
                }
                plan.holidays?.let { y += 3; textBlock(it, soft, gap = 3f) }
                y += 8
                ensure(12f)
                c.drawLine(MARGIN, y, PAGE_W - MARGIN, y, paint(1f, LINE))
                y += 14
            }
        }
    }
}
