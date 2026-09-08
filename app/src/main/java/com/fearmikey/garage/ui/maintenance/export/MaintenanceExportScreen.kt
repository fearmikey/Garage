package com.fearmikey.garage.ui.maintenance.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withTranslation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.ui.util.toDisplayDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceExportScreen(
    onBack: () -> Unit,
    viewModel: MaintenanceExportViewModel = hiltViewModel(),
) {
    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingFileName by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val v = vehicle ?: return@launch
                val success = generatePdf(context, uri, v, records)
                if (success) {
                    viewModel.pdfExportNotifier.notifyPdfExported(uri, pendingFileName)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export Maintenance Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                Icon(
                    imageVector = Icons.Filled.PictureAsPdf,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Generate a PDF report of all maintenance records.",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This report is perfect for keeping physical records or providing maintenance history to a buyer when selling your vehicle.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        val v = vehicle
                        if (v != null) {
                            val fileName = "${v.year ?: ""}_${v.make}_${v.model}_Maintenance.pdf".trim().replace(" ", "_")
                            pendingFileName = fileName
                            createDocumentLauncher.launch(fileName)
                        }
                    },
                    enabled = (vehicle != null) && records.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(if (records.isEmpty()) "No Records to Export" else "Export PDF")
                }
            }
        }
    }
}

suspend fun generatePdf(
    context: Context,
    uri: Uri,
    vehicle: Vehicle,
    records: List<MaintenanceRecord>,
): Boolean {
    return withContext(Dispatchers.IO) {
        val document = PdfDocument()

        val pageWidth = 612 // Standard 8.5 x 11 inch at 72 PPI
        val pageHeight = 792

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = Color.BLACK
        }

        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 14f
            color = Color.BLACK
        }

        val tableHeaderPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 12f
            color = Color.BLACK
        }

        val textPaint = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.DKGRAY
        }

        val bodyTextPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.DKGRAY
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var yPos = 50f

        // Draw Title
        canvas.drawText("Maintenance Report", 50f, yPos, titlePaint)
        yPos += 40f

        // Draw Vehicle Info
        canvas.drawText("Vehicle: ${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model}".trim(), 50f, yPos, headerPaint)
        yPos += 20f
        if (vehicle.vin.isNotBlank()) {
            canvas.drawText("VIN: ${vehicle.vin}", 50f, yPos, headerPaint)
            yPos += 20f
        }
        yPos += 20f

        // Draw Table Header
        canvas.drawLine(50f, yPos - 15f, pageWidth - 50f, yPos - 15f, linePaint)

        val colDate = 50f
        val colCategory = 135f
        val colTask = 220f
        val colMileage = 440f
        val colCost = 520f

        canvas.drawText("Date", colDate, yPos, tableHeaderPaint)
        canvas.drawText("Category", colCategory, yPos, tableHeaderPaint)
        canvas.drawText("Task / Description", colTask, yPos, tableHeaderPaint)
        canvas.drawText("Mileage", colMileage, yPos, tableHeaderPaint)
        canvas.drawText("Cost", colCost, yPos, tableHeaderPaint)
        yPos += 10f
        canvas.drawLine(50f, yPos, pageWidth - 50f, yPos, linePaint)
        yPos += 20f

        // Draw Records
        for (record in records) {
            val taskText = when {
                (!record.taskName.isNullOrBlank()) && record.description.isNotBlank() && (record.taskName != record.description) ->
                    "${record.taskName}: ${record.description}"
                record.description.isNotBlank() -> record.description
                else -> record.taskName.orEmpty()
            }

            val descWidth = (colMileage - colTask - 15f).toInt()
            val staticLayout = StaticLayout.Builder.obtain(taskText, 0, taskText.length, bodyTextPaint, descWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val rowHeight = maxOf(20f, staticLayout.height.toFloat() + 8f)

            if ((yPos + rowHeight) > (pageHeight - 50f)) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 50f

                // Redraw table header on new page
                canvas.drawText("Date", colDate, yPos, tableHeaderPaint)
                canvas.drawText("Category", colCategory, yPos, tableHeaderPaint)
                canvas.drawText("Task / Description", colTask, yPos, tableHeaderPaint)
                canvas.drawText("Mileage", colMileage, yPos, tableHeaderPaint)
                canvas.drawText("Cost", colCost, yPos, tableHeaderPaint)
                yPos += 10f
                canvas.drawLine(50f, yPos, pageWidth - 50f, yPos, linePaint)
                yPos += 20f
            }

            canvas.drawText(record.date.toDisplayDate(), colDate, yPos, textPaint)
            canvas.drawText(record.category.displayName, colCategory, yPos, textPaint)
            canvas.drawText("%,d".format(record.mileage), colMileage, yPos, textPaint)
            canvas.drawText("$%.2f".format(record.cost), colCost, yPos, textPaint)

            canvas.withTranslation(colTask, yPos - 9f) {
                staticLayout.draw(this)
            }

            yPos += rowHeight
        }

        document.finishPage(page)

        var success = false
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
                success = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            document.close()
        }
        success
    }
}
