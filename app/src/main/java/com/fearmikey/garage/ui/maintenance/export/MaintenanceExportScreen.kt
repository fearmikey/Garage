package com.fearmikey.garage.ui.maintenance.export

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.withTranslation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fearmikey.garage.data.local.entity.ChargingRecord
import com.fearmikey.garage.data.local.entity.FuelRecord
import com.fearmikey.garage.data.local.entity.MaintenanceRecord
import com.fearmikey.garage.data.local.entity.ModificationRecord
import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs
import com.fearmikey.garage.data.repository.VehicleRecall
import com.fearmikey.garage.ui.util.UnitConverter
import com.fearmikey.garage.ui.util.UnitSystem
import com.fearmikey.garage.ui.util.toDisplayDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private data class FuelEntryItem(
    val date: Long,
    val mileage: Int,
    val type: String,
    val details: String,
    val cost: Double,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceExportScreen(
    onBack: () -> Unit,
    viewModel: MaintenanceExportViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingFileName by remember { mutableStateOf("") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = generatePdf(context, uri, uiState)
                if (success) {
                    viewModel.pdfExportNotifier.notifyPdfExported(uri, pendingFileName)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seller Report Generator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val vehicle = uiState.vehicle
        if (vehicle == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Banner Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Vehicle Service & History Book",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Generate a comprehensive, beautifully formatted report designed to highlight your vehicle's care investment, maintenance log, parts replaced, and safety status to maximize resale value.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }

                // Summary Preview Card
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "REPORT SUMMARY PREVIEW",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model} ${vehicle.trim}".trim(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        if (vehicle.vin.isNotBlank()) {
                            Text(
                                text = "VIN: ${vehicle.vin}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Total Care Investment:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "%s%.2f".format(uiState.currencySymbol, uiState.totalCareInvestment),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Service Records:", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${uiState.maintenanceRecords.size} logged",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                        }

                        if (uiState.includeMods && uiState.modificationRecords.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Upgrades & Modifications:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${uiState.modificationRecords.size} items (%s%.2f)".format(uiState.currencySymbol, uiState.totalModificationCost),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        if (uiState.includeRecalls) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Safety Recall Status:", style = MaterialTheme.typography.bodyMedium)
                                if (uiState.isCheckingRecalls) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Checking...", style = MaterialTheme.typography.bodySmall)
                                    }
                                } else if (uiState.openRecalls.isEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (uiState.recalls.isNotEmpty()) "Clear (0 Open / ${uiState.recalls.size} Serviced)" else "Clear (0 Open)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "${uiState.openRecalls.size} Open / ${uiState.recalls.size} Total",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Inclusions & Options Card
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "INCLUDED SECTIONS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Upgrades & Modifications", style = MaterialTheme.typography.bodyLarge)
                                Text("Include list of custom parts, upgrades, and accessories", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.includeMods,
                                onCheckedChange = viewModel::toggleIncludeMods,
                            )
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Parts & Fluid Specifications", style = MaterialTheme.typography.bodyLarge)
                                Text("Include oil viscosity, spark plug, tire size & wiper cheat sheet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.includePartsSpecs,
                                onCheckedChange = viewModel::toggleIncludePartsSpecs,
                            )
                        }

                        HorizontalDivider()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Safety Recalls Status", style = MaterialTheme.typography.bodyLarge)
                                    Text("Include NHTSA official safety recall verification status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = uiState.includeRecalls,
                                    onCheckedChange = viewModel::toggleIncludeRecalls,
                                )
                            }

                            // Interactive Recall Campaign Checklist
                            AnimatedVisibility(visible = uiState.includeRecalls && uiState.recalls.isNotEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    HorizontalDivider()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = "Select Open / Active Campaigns (${uiState.recalls.size} total):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        Row {
                                            TextButton(
                                                onClick = viewModel::markAllRecallsResolved,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                            ) {
                                                Text("All Serviced", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(
                                                onClick = viewModel::markAllRecallsOpen,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                            ) {
                                                Text("All Open", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    uiState.recalls.forEach { recall ->
                                        val isOpen = recall.campaignNumber in uiState.openRecallCampaignNumbers
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isOpen) {
                                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                },
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                                    .fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Checkbox(
                                                    checked = isOpen,
                                                    onCheckedChange = { checked ->
                                                        viewModel.toggleRecallCampaignOpen(recall.campaignNumber, checked)
                                                    },
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                    ) {
                                                        Text(
                                                            text = "Campaign #${recall.campaignNumber}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                        )
                                                        Text(
                                                            text = if (isOpen) "OPEN / UNADDRESSED" else "SERVICED / CLEAR",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isOpen) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                        )
                                                    }
                                                    Text(
                                                        text = "Component: ${recall.component}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                    if (recall.summary.isNotBlank()) {
                                                        Text(
                                                            text = recall.summary,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Fuel & Charging Logs", style = MaterialTheme.typography.bodyLarge)
                                Text("Include fuel fill-ups and charging session records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.includeFuel,
                                onCheckedChange = viewModel::toggleIncludeFuel,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val fileName = "${vehicle.year ?: ""}_${vehicle.make}_${vehicle.model}_Service_Book.pdf".trim().replace(" ", "_")
                        pendingFileName = fileName
                        createDocumentLauncher.launch(fileName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Generate Vehicle Service Book (PDF)")
                }
            }
        }
    }
}

suspend fun generatePdf(
    context: Context,
    uri: Uri,
    uiState: ExportUiState,
): Boolean {
    val vehicle = uiState.vehicle ?: return false
    return generatePdf(
        context = context,
        uri = uri,
        vehicle = vehicle,
        specs = uiState.specs,
        parts = uiState.parts,
        maintenanceRecords = uiState.maintenanceRecords,
        modificationRecords = uiState.modificationRecords,
        fuelRecords = uiState.fuelRecords,
        chargingRecords = uiState.chargingRecords,
        recalls = uiState.recalls,
        openRecallCampaignNumbers = uiState.openRecallCampaignNumbers,
        currencySymbol = uiState.currencySymbol,
        unitSystem = uiState.unitSystem,
        includeMods = uiState.includeMods,
        includePartsSpecs = uiState.includePartsSpecs,
        includeRecalls = uiState.includeRecalls,
        includeFuel = uiState.includeFuel,
        vehiclePhotoFile = uiState.vehiclePhotoFile,
    )
}

suspend fun generatePdf(
    context: Context,
    uri: Uri,
    vehicle: Vehicle,
    specs: VehicleSpecs? = null,
    parts: VehiclePartsInfo? = null,
    maintenanceRecords: List<MaintenanceRecord> = emptyList(),
    modificationRecords: List<ModificationRecord> = emptyList(),
    fuelRecords: List<FuelRecord> = emptyList(),
    chargingRecords: List<ChargingRecord> = emptyList(),
    recalls: List<VehicleRecall> = emptyList(),
    openRecallCampaignNumbers: Set<String> = emptySet(),
    currencySymbol: String = "$",
    unitSystem: UnitSystem = UnitSystem.IMPERIAL,
    includeMods: Boolean = true,
    includePartsSpecs: Boolean = true,
    includeRecalls: Boolean = true,
    includeFuel: Boolean = false,
    vehiclePhotoFile: File? = null,
): Boolean = withContext(Dispatchers.IO) {
    val document = PdfDocument()

    val pageWidth = 612 // 8.5" x 11" at 72 dpi
    val pageHeight = 792
    val marginLeft = 36f
    val marginRight = 576f
    val marginTop = 36f
    val marginBottom = 752f
    val printWidth = 540f

    val vehicleTitle = "${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model} ${vehicle.trim}".trim()

    val colorSlate800 = Color.rgb(30, 41, 59)
    val colorBlue600 = Color.rgb(37, 99, 235)
    val colorSlate900 = Color.rgb(15, 23, 42)
    val colorSlate700 = Color.rgb(51, 65, 85)
    val colorSlate500 = Color.rgb(100, 116, 139)
    val colorSlate200 = Color.rgb(226, 232, 240)
    val colorSlate100 = Color.rgb(241, 245, 249)
    val colorSlate50 = Color.rgb(248, 250, 252)

    val fillPaint = Paint().apply { style = Paint.Style.FILL }
    val strokePaint = Paint().apply { style = Paint.Style.STROKE; strokeWidth = 1f; color = colorSlate200 }

    val bannerTitlePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 15f
        color = Color.WHITE
        isAntiAlias = true
    }
    val bannerSubtitlePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        textSize = 9f
        color = Color.rgb(203, 213, 225)
        isAntiAlias = true
    }
    val bannerDatePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 8.5f
        color = Color.rgb(203, 213, 225)
        isAntiAlias = true
        textAlign = Paint.Align.RIGHT
    }

    val sectionTitlePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 11f
        color = colorSlate900
        isAntiAlias = true
    }
    val labelPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 8.5f
        color = colorSlate500
        isAntiAlias = true
    }
    val valueBoldPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9.5f
        color = colorSlate900
        isAntiAlias = true
    }
    val valueNormalPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textSize = 9f
        color = colorSlate700
        isAntiAlias = true
    }

    // --- PASS 1: Calculate total pages ---
    var totalPages = 1
    var calcY = marginTop

    fun checkCalcOverflow(needed: Float) {
        if (calcY + needed > marginBottom) {
            totalPages++
            calcY = marginTop + 34f
        }
    }

    calcY += 56f + 12f // Cover banner
    calcY += 108f + 12f // Overview card
    calcY += 68f + 12f // Care investment card

    if (includeRecalls) {
        val openRecalls = recalls.filter { it.campaignNumber in openRecallCampaignNumbers }
        if (openRecalls.isEmpty()) {
            checkCalcOverflow(44f)
            calcY += 38f + 12f
        } else {
            checkCalcOverflow(60f)
            calcY += 16f
            val warnTextPaintCalc = TextPaint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
            }
            for (recall in openRecalls) {
                val summaryText = recall.summary.ifBlank { "No summary details provided." }
                val staticLayout = StaticLayout.Builder.obtain(summaryText, 0, summaryText.length, warnTextPaintCalc, 510)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()
                val itemHeight = maxOf(32f, staticLayout.height.toFloat() + 24f)
                checkCalcOverflow(itemHeight + 6f)
                calcY += itemHeight + 6f
            }
            calcY += 8f
        }
    }

    if (includePartsSpecs && parts != null && !parts.isEmpty()) {
        checkCalcOverflow(80f)
        calcY += 16f
        val partsItemsCount = listOfNotNull(
            parts.oilViscosity, parts.oilCapacity, parts.oilFilterPartNumber,
            parts.sparkPlugPartNumber, parts.sparkPlugGap, parts.tireSizeFront,
            parts.tireSizeRear, parts.tirePsiFront, parts.tirePsiRear,
            parts.wiperBladeSizeDriver, parts.wiperBladeSizePassenger, parts.wiperBladeSizeRear,
        ).count { it.isNotBlank() }

        if (partsItemsCount > 0) {
            val gridCols = 3
            var itemIdx = 0
            while (itemIdx < partsItemsCount) {
                checkCalcOverflow(20f)
                if ((itemIdx % gridCols == gridCols - 1) || (itemIdx == partsItemsCount - 1)) {
                    calcY += 24f
                }
                itemIdx++
            }
            calcY += 8f
        }
    }

    if (includeMods && modificationRecords.isNotEmpty()) {
        checkCalcOverflow(80f)
        calcY += 16f
        checkCalcOverflow(22f)
        calcY += 20f
        repeat(modificationRecords.size) {
            checkCalcOverflow(20f)
            calcY += 18f
        }
        calcY += 12f
    }

    if (includeFuel && (fuelRecords.isNotEmpty() || chargingRecords.isNotEmpty())) {
        checkCalcOverflow(80f)
        calcY += 16f
        checkCalcOverflow(22f)
        calcY += 20f
        val fuelCount = fuelRecords.size + chargingRecords.size
        repeat(fuelCount) {
            checkCalcOverflow(20f)
            calcY += 18f
        }
        calcY += 12f
    }

    checkCalcOverflow(80f)
    calcY += 16f
    checkCalcOverflow(20f)
    calcY += 20f

    if (maintenanceRecords.isEmpty()) {
        calcY += 24f
    } else {
        val bodyTextPaintCalc = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8.5f
        }
        for (record in maintenanceRecords) {
            val taskText = when {
                (!record.taskName.isNullOrBlank()) && record.description.isNotBlank() && (record.taskName != record.description) ->
                    "${record.taskName}: ${record.description}"
                record.description.isNotBlank() -> record.description
                else -> record.taskName.orEmpty()
            }

            val staticLayout = StaticLayout.Builder.obtain(taskText, 0, taskText.length, bodyTextPaintCalc, 260)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val rowHeight = maxOf(18f, staticLayout.height.toFloat() + 6f)
            checkCalcOverflow(rowHeight)
            calcY += rowHeight
        }
    }

    // --- PASS 2: Render PDF pages ---
    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = document.startPage(pageInfo)
    var canvas = page.canvas

    var yPos = marginTop

    fun drawFooter(c: Canvas, pNum: Int) {
        val linePaint = Paint().apply { color = colorSlate200; strokeWidth = 0.8f }
        c.drawLine(marginLeft, 760f, marginRight, 760f, linePaint)

        val fText = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            color = colorSlate500
            isAntiAlias = true
        }
        c.drawText("Garage — Vehicle Service & History Book", marginLeft, 772f, fText)

        val pText = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8f
            color = colorSlate500
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        c.drawText("Page $pNum of $totalPages", marginRight, 772f, pText)
    }

    fun drawSubpageHeader(c: Canvas) {
        fillPaint.color = colorSlate800
        c.drawRect(marginLeft, marginTop, marginRight, marginTop + 24f, fillPaint)

        val hText = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 9f
            color = Color.WHITE
            isAntiAlias = true
        }
        c.drawText("VEHICLE SERVICE & HISTORY BOOK — $vehicleTitle", marginLeft + 8f, marginTop + 16f, hText)

        val cText = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8f
            color = Color.rgb(203, 213, 225)
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        c.drawText("Continued", marginRight - 8f, marginTop + 16f, cText)

        val lineP = Paint().apply { color = colorBlue600; strokeWidth = 2f }
        c.drawLine(marginLeft, marginTop + 25f, marginRight, marginTop + 25f, lineP)
    }

    fun checkPageOverflow(needed: Float): Boolean {
        if (yPos + needed > marginBottom) {
            drawFooter(canvas, pageNumber)
            document.finishPage(page)

            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = marginTop

            drawSubpageHeader(canvas)
            yPos += 34f
            return true
        }
        return false
    }

    // --- 1. COVER BANNER ---
    fillPaint.color = colorSlate800
    val bannerRect = RectF(marginLeft, yPos, marginRight, yPos + 54f)
    canvas.drawRoundRect(bannerRect, 6f, 6f, fillPaint)

    canvas.drawText("VEHICLE SERVICE & HISTORY BOOK", marginLeft + 12f, yPos + 24f, bannerTitlePaint)
    canvas.drawText("Official Resale & Continuous Care Report", marginLeft + 12f, yPos + 40f, bannerSubtitlePaint)
    val formattedToday = System.currentTimeMillis().toDisplayDate()
    canvas.drawText("Generated $formattedToday", marginRight - 12f, yPos + 24f, bannerDatePaint)

    yPos += 56f
    val accentLinePaint = Paint().apply { color = colorBlue600; strokeWidth = 2.5f }
    canvas.drawLine(marginLeft, yPos, marginRight, yPos, accentLinePaint)
    yPos += 12f

    // --- 2. VEHICLE OVERVIEW CARD ---
    val cardTop = yPos
    val cardHeight = 108f
    val cardRect = RectF(marginLeft, cardTop, marginRight, cardTop + cardHeight)

    fillPaint.color = colorSlate50
    canvas.drawRoundRect(cardRect, 6f, 6f, fillPaint)
    strokePaint.color = colorSlate200
    canvas.drawRoundRect(cardRect, 6f, 6f, strokePaint)

    var contentX = marginLeft + 12f

    if (vehiclePhotoFile != null && vehiclePhotoFile.exists()) {
        try {
            val bitmap = BitmapFactory.decodeFile(vehiclePhotoFile.absolutePath)
            if (bitmap != null) {
                val imgFrame = RectF(marginLeft + 10f, cardTop + 10f, marginLeft + 140f, cardTop + 98f)
                canvas.drawRoundRect(imgFrame, 4f, 4f, strokePaint)
                val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
                canvas.drawBitmap(bitmap, srcRect, imgFrame, null)
                bitmap.recycle()
                contentX = marginLeft + 150f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val titlePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 13.5f
        color = colorSlate900
        isAntiAlias = true
    }
    canvas.drawText(vehicleTitle, contentX, cardTop + 24f, titlePaint)

    val allMileages = (maintenanceRecords.map { it.mileage } + fuelRecords.map { it.mileage } + chargingRecords.map { it.mileage })
        .filter { it > 0 }
    val maxMileage = allMileages.maxOrNull()
    val minMileage = allMileages.minOrNull()

    val distUnit = unitSystem.distanceUnit
    val formattedMileage = maxMileage?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "Not recorded"

    val col1X = contentX
    val col2X = contentX + (marginRight - contentX) / 2f + 10f

    canvas.drawText("VIN:", col1X, cardTop + 44f, labelPaint)
    val vinDisplay = vehicle.vin.ifBlank { "Not specified" }
    canvas.drawText(vinDisplay, col1X + 28f, cardTop + 44f, valueBoldPaint)

    canvas.drawText("CURRENT MILEAGE:", col2X, cardTop + 44f, labelPaint)
    canvas.drawText(formattedMileage, col2X + 88f, cardTop + 44f, valueBoldPaint)

    canvas.drawText("DRIVETRAIN:", col1X, cardTop + 62f, labelPaint)
    canvas.drawText(vehicle.drivetrain.displayName, col1X + 58f, cardTop + 62f, valueNormalPaint)

    canvas.drawText("TRANSMISSION:", col2X, cardTop + 62f, labelPaint)
    val transDisplay = listOfNotNull(specs?.transmissionStyle, specs?.transmissionSpeeds?.let { "$it-Spd" }).joinToString(" ")
        .ifBlank { "Standard" }
    canvas.drawText(transDisplay, col2X + 78f, cardTop + 62f, valueNormalPaint)

    val engineDisplay = listOfNotNull(specs?.displacementL?.let { "${it}L" }, specs?.engineCylinders?.let { "$it-Cyl" }, specs?.engineHp?.let { "($it hp)" }).joinToString(" ")
        .ifBlank { "Standard Engine" }
    canvas.drawText("ENGINE:", col1X, cardTop + 80f, labelPaint)
    canvas.drawText(engineDisplay, col1X + 42f, cardTop + 80f, valueNormalPaint)

    canvas.drawText("FUEL TYPE:", col2X, cardTop + 80f, labelPaint)
    val fuelDisplay = specs?.fuelType ?: "Gasoline"
    canvas.drawText(fuelDisplay, col2X + 54f, cardTop + 80f, valueNormalPaint)

    if (specs != null && (specs.bodyClass != null || specs.manufacturer != null)) {
        val extraDisplay = listOfNotNull(specs.bodyClass, specs.manufacturer).joinToString(" • ")
        canvas.drawText("SPECS:", col1X, cardTop + 98f, labelPaint)
        canvas.drawText(extraDisplay, col1X + 36f, cardTop + 98f, valueNormalPaint)
    }

    yPos = cardTop + cardHeight + 12f

    // --- 3. TOTAL CARE INVESTMENT SUMMARY CARD ---
    val totalMaintCost = maintenanceRecords.sumOf { it.cost }
    val totalModCost = if (includeMods) modificationRecords.sumOf { it.cost } else 0.0
    val totalCareInvestment = totalMaintCost + totalModCost

    val summaryHeight = 68f
    val summaryRect = RectF(marginLeft, yPos, marginRight, yPos + summaryHeight)

    fillPaint.color = colorSlate50
    canvas.drawRoundRect(summaryRect, 6f, 6f, fillPaint)
    strokePaint.color = colorBlue600
    strokePaint.strokeWidth = 1.2f
    canvas.drawRoundRect(summaryRect, 6f, 6f, strokePaint)
    strokePaint.strokeWidth = 1f

    val summaryTitlePaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 9.5f
        color = colorBlue600
        isAntiAlias = true
    }
    canvas.drawText("TOTAL CARE INVESTMENT SUMMARY", marginLeft + 12f, yPos + 18f, summaryTitlePaint)

    val metricWidth = (printWidth - 24f) / 4f

    var mX = marginLeft + 12f
    canvas.drawText("TOTAL INVESTMENT", mX, yPos + 32f, labelPaint)
    val totalValPaint = TextPaint().apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 12.5f
        color = colorSlate900
        isAntiAlias = true
    }
    canvas.drawText("%s%.2f".format(currencySymbol, totalCareInvestment), mX, yPos + 48f, totalValPaint)

    mX += metricWidth
    canvas.drawText("MAINTENANCE & CARE", mX, yPos + 32f, labelPaint)
    canvas.drawText("%s%.2f".format(currencySymbol, totalMaintCost), mX, yPos + 47f, valueBoldPaint)
    canvas.drawText("%d records logged".format(maintenanceRecords.size), mX, yPos + 58f, bannerSubtitlePaint.apply { color = colorSlate500 })

    mX += metricWidth
    canvas.drawText("UPGRADES & MODS", mX, yPos + 32f, labelPaint)
    canvas.drawText("%s%.2f".format(currencySymbol, totalModCost), mX, yPos + 47f, valueBoldPaint)
    canvas.drawText("%d installed items".format(modificationRecords.size), mX, yPos + 58f, bannerSubtitlePaint.apply { color = colorSlate500 })

    mX += metricWidth
    canvas.drawText("LOGGED RANGE", mX, yPos + 32f, labelPaint)
    val startStr = minMileage?.let { UnitConverter.formatDistance(it, unitSystem) } ?: "0 $distUnit"
    canvas.drawText(startStr, mX, yPos + 47f, valueBoldPaint)
    canvas.drawText("to $formattedMileage", mX, yPos + 58f, bannerSubtitlePaint.apply { color = colorSlate500 })

    yPos += summaryHeight + 12f

    // --- 4. SAFETY RECALLS STATUS ---
    if (includeRecalls) {
        val openRecalls = recalls.filter { it.campaignNumber in openRecallCampaignNumbers }

        if (openRecalls.isEmpty()) {
            checkPageOverflow(44f)
            val recallHeight = 38f
            val recallRect = RectF(marginLeft, yPos, marginRight, yPos + recallHeight)

            val clearBgPaint = Paint().apply { color = Color.rgb(236, 253, 245); style = Paint.Style.FILL }
            val clearStrokePaint = Paint().apply { color = Color.rgb(4, 120, 87); style = Paint.Style.STROKE; strokeWidth = 1f }

            canvas.drawRoundRect(recallRect, 6f, 6f, clearBgPaint)
            canvas.drawRoundRect(recallRect, 6f, 6f, clearStrokePaint)

            val clearTextPaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = Color.rgb(4, 120, 87)
                isAntiAlias = true
            }
            canvas.drawText("OFFICIAL SAFETY RECALL STATUS: CLEAR (0 OPEN RECALLS)", marginLeft + 12f, yPos + 16f, clearTextPaint)

            val subClearPaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
                color = Color.rgb(6, 95, 70)
                isAntiAlias = true
            }
            val detailMsg = if (recalls.isNotEmpty()) {
                "Verified via NHTSA database for ${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model} (${recalls.size} total campaign(s) checked — 0 active open recalls)."
            } else {
                "Verified via NHTSA database for ${vehicle.year ?: ""} ${vehicle.make} ${vehicle.model}. No active safety recall campaigns found."
            }
            canvas.drawText(detailMsg, marginLeft + 12f, yPos + 28f, subClearPaint)

            yPos += recallHeight + 12f
        } else {
            checkPageOverflow(60f)

            canvas.drawText("SAFETY RECALL STATUS: ${openRecalls.size} OPEN RECALL(S)", marginLeft, yPos, sectionTitlePaint.apply { color = Color.rgb(185, 28, 28) })
            yPos += 4f
            val lineP = Paint().apply { color = Color.rgb(185, 28, 28); strokeWidth = 1f }
            canvas.drawLine(marginLeft, yPos, marginRight, yPos, lineP)
            yPos += 12f

            val warnBgPaint = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
            val warnStrokePaint = Paint().apply { color = Color.rgb(185, 28, 28); style = Paint.Style.STROKE; strokeWidth = 1f }

            val warnTitlePaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = Color.rgb(153, 27, 27)
                isAntiAlias = true
            }

            val warnTextPaint = TextPaint().apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8.5f
                color = Color.rgb(127, 29, 29)
                isAntiAlias = true
            }

            for (recall in openRecalls) {
                val titleStr = "• Campaign #${recall.campaignNumber} — Component: ${recall.component}"
                val summaryText = recall.summary.ifBlank { "No summary details provided." }

                val staticLayout = StaticLayout.Builder.obtain(summaryText, 0, summaryText.length, warnTextPaint, 510)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build()

                val itemHeight = maxOf(32f, staticLayout.height.toFloat() + 24f)

                checkPageOverflow(itemHeight + 6f)

                val itemRect = RectF(marginLeft, yPos, marginRight, yPos + itemHeight)
                canvas.drawRoundRect(itemRect, 4f, 4f, warnBgPaint)
                canvas.drawRoundRect(itemRect, 4f, 4f, warnStrokePaint)

                canvas.drawText(titleStr, marginLeft + 10f, yPos + 14f, warnTitlePaint)

                canvas.withTranslation(marginLeft + 18f, yPos + 20f) {
                    staticLayout.draw(this)
                }

                yPos += itemHeight + 6f
            }
            yPos += 8f
        }
    }

    // --- 5. PARTS & FLUID SPECIFICATIONS ---
    if (includePartsSpecs && parts != null && !parts.isEmpty()) {
        checkPageOverflow(80f)

        canvas.drawText("PARTS & FLUID SPECIFICATIONS", marginLeft, yPos, sectionTitlePaint)
        yPos += 4f
        val lineP = Paint().apply { color = colorSlate200; strokeWidth = 1f }
        canvas.drawLine(marginLeft, yPos, marginRight, yPos, lineP)
        yPos += 12f

        val partsItems = buildList {
            parts.oilViscosity?.takeIf { it.isNotBlank() }?.let { add("Oil Viscosity" to it) }
            parts.oilCapacity?.takeIf { it.isNotBlank() }?.let { add("Oil Capacity" to it) }
            parts.oilFilterPartNumber?.takeIf { it.isNotBlank() }?.let { add("Oil Filter Part #" to it) }
            parts.sparkPlugPartNumber?.takeIf { it.isNotBlank() }?.let { add("Spark Plug Part #" to it) }
            parts.sparkPlugGap?.takeIf { it.isNotBlank() }?.let { add("Spark Plug Gap" to it) }
            parts.tireSizeFront?.takeIf { it.isNotBlank() }?.let { add("Front Tire Size" to it) }
            parts.tireSizeRear?.takeIf { it.isNotBlank() }?.let { add("Rear Tire Size" to it) }
            parts.tirePsiFront?.takeIf { it.isNotBlank() }?.let { add("Front Tire Pressure" to "$it PSI") }
            parts.tirePsiRear?.takeIf { it.isNotBlank() }?.let { add("Rear Tire Pressure" to "$it PSI") }
            parts.wiperBladeSizeDriver?.takeIf { it.isNotBlank() }?.let { add("Wiper Driver" to it) }
            parts.wiperBladeSizePassenger?.takeIf { it.isNotBlank() }?.let { add("Wiper Passenger" to it) }
            parts.wiperBladeSizeRear?.takeIf { it.isNotBlank() }?.let { add("Wiper Rear" to it) }
        }

        if (partsItems.isNotEmpty()) {
            val gridCols = 3
            val colW = printWidth / gridCols
            var itemIdx = 0
            while (itemIdx < partsItems.size) {
                checkPageOverflow(20f)
                val cX = marginLeft + (itemIdx % gridCols) * colW
                val (lbl, valStr) = partsItems[itemIdx]

                canvas.drawText(lbl.uppercase(), cX, yPos, labelPaint)
                canvas.drawText(valStr, cX, yPos + 12f, valueBoldPaint)

                if ((itemIdx % gridCols == gridCols - 1) || (itemIdx == partsItems.size - 1)) {
                    yPos += 24f
                }
                itemIdx++
            }
            yPos += 8f
        }
    }

    // --- 6. UPGRADES & MODIFICATIONS TABLE ---
    if (includeMods && modificationRecords.isNotEmpty()) {
        checkPageOverflow(80f)

        canvas.drawText("VEHICLE UPGRADES & MODIFICATIONS", marginLeft, yPos, sectionTitlePaint)
        yPos += 4f
        val lineP = Paint().apply { color = colorSlate200; strokeWidth = 1f }
        canvas.drawLine(marginLeft, yPos, marginRight, yPos, lineP)
        yPos += 12f

        checkPageOverflow(22f)
        fillPaint.color = colorSlate800
        canvas.drawRect(marginLeft, yPos, marginRight, yPos + 18f, fillPaint)

        val tblHeaderPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawText("DATE", marginLeft + 8f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("UPGRADE / ITEM", marginLeft + 70f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("CATEGORY", marginLeft + 230f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("DESCRIPTION / NOTES", marginLeft + 320f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("COST", marginRight - 48f, yPos + 12f, tblHeaderPaint)

        yPos += 20f

        for ((idx, mod) in modificationRecords.sortedByDescending { it.date }.withIndex()) {
            checkPageOverflow(20f)

            if (idx % 2 == 1) {
                fillPaint.color = colorSlate100
                canvas.drawRect(marginLeft, yPos - 2f, marginRight, yPos + 16f, fillPaint)
            }

            canvas.drawText(mod.date.toDisplayDate(), marginLeft + 8f, yPos + 10f, valueNormalPaint)
            val titleTrunc = mod.title.take(28)
            canvas.drawText(titleTrunc, marginLeft + 70f, yPos + 10f, valueBoldPaint)
            canvas.drawText(mod.category.displayName, marginLeft + 230f, yPos + 10f, valueNormalPaint)
            val descTrunc = mod.description.take(35)
            canvas.drawText(descTrunc, marginLeft + 320f, yPos + 10f, valueNormalPaint)
            val costStr = "%s%.2f".format(currencySymbol, mod.cost)
            canvas.drawText(costStr, marginRight - 48f, yPos + 10f, valueBoldPaint)

            yPos += 18f
        }
        yPos += 12f
    }

    // --- 6.5. FUEL & CHARGING HISTORY LOG ---
    if (includeFuel && (fuelRecords.isNotEmpty() || chargingRecords.isNotEmpty())) {
        checkPageOverflow(80f)

        canvas.drawText("FUEL & CHARGING HISTORY LOG", marginLeft, yPos, sectionTitlePaint)
        yPos += 4f
        val lineP = Paint().apply { color = colorSlate200; strokeWidth = 1f }
        canvas.drawLine(marginLeft, yPos, marginRight, yPos, lineP)
        yPos += 12f

        checkPageOverflow(22f)
        fillPaint.color = colorSlate800
        canvas.drawRect(marginLeft, yPos, marginRight, yPos + 18f, fillPaint)

        val tblHeaderPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawText("DATE", marginLeft + 8f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("MILEAGE", marginLeft + 72f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("TYPE", marginLeft + 138f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("DETAILS", marginLeft + 225f, yPos + 12f, tblHeaderPaint)

        val rightHeaderPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("COST", marginRight - 8f, yPos + 12f, rightHeaderPaint)
        yPos += 20f

        val combinedFuelEntries = buildList {
            fuelRecords.forEach { add(FuelEntryItem(it.date, it.mileage, "Fuel Fill-Up", UnitConverter.formatVolume(it.gallons, unitSystem), it.totalCost)) }
            chargingRecords.forEach { add(FuelEntryItem(it.date, it.mileage, "EV Charging", "%.1f kWh".format(it.kwhAdded), it.totalCost)) }
        }.sortedByDescending { it.date }

        val rightCostPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = colorSlate900
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        for ((idx, entry) in combinedFuelEntries.withIndex()) {
            checkPageOverflow(20f)
            if (idx % 2 == 1) {
                fillPaint.color = colorSlate100
                canvas.drawRect(marginLeft, yPos - 1f, marginRight, yPos + 16f, fillPaint)
            }
            canvas.drawText(entry.date.toDisplayDate(), marginLeft + 8f, yPos + 10f, valueNormalPaint)
            canvas.drawText(UnitConverter.formatDistance(entry.mileage, unitSystem), marginLeft + 72f, yPos + 10f, valueNormalPaint)
            canvas.drawText(entry.type, marginLeft + 138f, yPos + 10f, valueNormalPaint)
            canvas.drawText(entry.details, marginLeft + 225f, yPos + 10f, valueNormalPaint)
            val costStr = "%s%.2f".format(currencySymbol, entry.cost)
            canvas.drawText(costStr, marginRight - 8f, yPos + 10f, rightCostPaint)
            yPos += 18f
        }
        yPos += 12f
    }

    // --- 7. CONTINUOUS SERVICE HISTORY LOG ---
    checkPageOverflow(80f)

    canvas.drawText("CONTINUOUS SERVICE HISTORY LOG", marginLeft, yPos, sectionTitlePaint)
    yPos += 4f
    val lineP = Paint().apply { color = colorSlate200; strokeWidth = 1f }
    canvas.drawLine(marginLeft, yPos, marginRight, yPos, lineP)
    yPos += 12f

    fun drawMaintenanceTableHeader() {
        fillPaint.color = colorSlate800
        canvas.drawRect(marginLeft, yPos, marginRight, yPos + 18f, fillPaint)

        val tblHeaderPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
            isAntiAlias = true
        }
        canvas.drawText("DATE", marginLeft + 8f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("MILEAGE", marginLeft + 72f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("CATEGORY", marginLeft + 138f, yPos + 12f, tblHeaderPaint)
        canvas.drawText("SERVICE & TASK DESCRIPTION", marginLeft + 225f, yPos + 12f, tblHeaderPaint)

        val rightHeaderPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = Color.WHITE
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("COST", marginRight - 8f, yPos + 12f, rightHeaderPaint)
        yPos += 20f
    }

    drawMaintenanceTableHeader()

    if (maintenanceRecords.isEmpty()) {
        canvas.drawText("No maintenance records logged for this vehicle.", marginLeft + 8f, yPos + 12f, valueNormalPaint)
        yPos += 24f
    } else {
        val bodyTextPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 8.5f
            color = colorSlate700
            isAntiAlias = true
        }

        val rightCostPaint = TextPaint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            color = colorSlate900
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        val descWidth = 260

        for ((idx, record) in maintenanceRecords.sortedByDescending { it.date }.withIndex()) {
            val taskText = when {
                (!record.taskName.isNullOrBlank()) && record.description.isNotBlank() && (record.taskName != record.description) ->
                    "${record.taskName}: ${record.description}"
                record.description.isNotBlank() -> record.description
                else -> record.taskName.orEmpty()
            }

            val staticLayout = StaticLayout.Builder.obtain(taskText, 0, taskText.length, bodyTextPaint, descWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .setIncludePad(false)
                .build()

            val rowHeight = maxOf(18f, staticLayout.height.toFloat() + 6f)

            if (yPos + rowHeight > marginBottom) {
                drawFooter(canvas, pageNumber)
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = marginTop

                drawSubpageHeader(canvas)
                yPos += 34f
                drawMaintenanceTableHeader()
            }

            if (idx % 2 == 1) {
                fillPaint.color = colorSlate100
                canvas.drawRect(marginLeft, yPos - 1f, marginRight, yPos + rowHeight - 1f, fillPaint)
            }

            canvas.drawText(record.date.toDisplayDate(), marginLeft + 8f, yPos + 11f, valueNormalPaint)
            val miStr = UnitConverter.formatDistance(record.mileage, unitSystem)
            canvas.drawText(miStr, marginLeft + 72f, yPos + 11f, valueNormalPaint)
            canvas.drawText(record.category.displayName, marginLeft + 138f, yPos + 11f, valueNormalPaint)

            canvas.withTranslation(marginLeft + 225f, yPos + 3f) {
                staticLayout.draw(this)
            }

            val costStr = "%s%.2f".format(currencySymbol, record.cost)
            canvas.drawText(costStr, marginRight - 8f, yPos + 11f, rightCostPaint)

            yPos += rowHeight
        }
    }

    drawFooter(canvas, pageNumber)
    document.finishPage(page)

    var success = false
    try {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            document.writeTo(output)
            success = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        document.close()
    }
    success
}
