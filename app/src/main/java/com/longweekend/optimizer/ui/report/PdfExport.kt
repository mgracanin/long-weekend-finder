package com.longweekend.optimizer.ui.report

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.FileProvider
import com.longweekend.optimizer.R
import com.longweekend.optimizer.ui.theme.TealPrimary
import com.longweekend.optimizer.ui.theme.TextOnSurfaceVariant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** A PDF written to the cache, with the subject used when it is shared. */
data class ReadyPdf(val file: File, val subject: String)

/** Makes a PDF in the background, then offers to open, save or share it in [PdfExportDialog]. */
@Stable
class PdfExport internal constructor(
    private val scope: CoroutineScope,
    private val onFailed: () -> Unit
) {
    internal var ready by mutableStateOf<ReadyPdf?>(null)

    /** [write] runs off the main thread and returns the finished file. */
    fun start(write: () -> ReadyPdf) {
        scope.launch {
            runCatching { withContext(Dispatchers.IO) { write() } }
                .onSuccess { ready = it }
                .onFailure { onFailed() }
        }
    }
}

@Composable
fun rememberPdfExport(): PdfExport {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val failedMessage = stringResource(R.string.pdf_failed)
    return remember(scope) {
        PdfExport(scope) { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
    }
}

/** Shown while a finished PDF waits for the user to open, save or share it. */
@Composable
fun PdfExportDialog(export: PdfExport) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.pdf_chooser_title)
    val savedMessage = stringResource(R.string.pdf_saved)
    val failedMessage = stringResource(R.string.pdf_failed)
    val noViewerMessage = stringResource(R.string.pdf_no_viewer)
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { target ->
        val file = export.ready?.file
        if (target != null && file != null) {
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { context.savePdf(file, target) } }
                    .onSuccess { Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show() }
            }
        }
        export.ready = null
    }

    val pdf = export.ready ?: return
    AlertDialog(
        onDismissRequest = { export.ready = null },
        icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = TealPrimary) },
        title = { Text(stringResource(R.string.pdf_ready_title)) },
        text = { Text(pdf.file.name, style = MaterialTheme.typography.bodyMedium, color = TextOnSurfaceVariant) },
        confirmButton = {
            TextButton(onClick = {
                export.ready = null
                if (!context.openPdf(pdf.file)) {
                    Toast.makeText(context, noViewerMessage, Toast.LENGTH_LONG).show()
                    context.sharePdf(pdf, chooserTitle)
                }
            }) { Text(stringResource(R.string.pdf_open), fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { saveLauncher.launch(pdf.file.name) }) { Text(stringResource(R.string.pdf_save)) }
                TextButton(onClick = {
                    export.ready = null
                    context.sharePdf(pdf, chooserTitle)
                }) { Text(stringResource(R.string.action_share)) }
            }
        }
    )
}

/** Where a PDF called [name] is written; the only folder shared through the FileProvider. */
fun Context.pdfCacheFile(name: String): File = File(File(cacheDir, "reports").apply { mkdirs() }, name)

private fun Context.pdfUri(file: File): Uri = FileProvider.getUriForFile(this, "$packageName.reports", file)

/**
 * Opens the share sheet for a written PDF (e-mail, Drive, print…). The file is also attached
 * as ClipData: some share targets only receive the read permission that way.
 */
private fun Context.sharePdf(pdf: ReadyPdf, chooserTitle: String) {
    val uri = pdfUri(pdf.file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, pdf.subject)
        clipData = ClipData.newRawUri(pdf.file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
}

/** Opens the PDF in the phone's PDF viewer; false when no app can show a PDF. */
private fun Context.openPdf(file: File): Boolean {
    val uri = pdfUri(file)
    val view = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        startActivity(view)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

/** Copies the PDF to a place the user picked (e.g. Downloads), where any app can open it. */
private fun Context.savePdf(file: File, target: Uri) {
    contentResolver.openOutputStream(target)?.use { out -> file.inputStream().use { it.copyTo(out) } }
        ?: error("Can't write to $target")
}
