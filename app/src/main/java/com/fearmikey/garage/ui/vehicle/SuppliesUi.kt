package com.fearmikey.garage.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.R
import com.fearmikey.garage.data.local.entity.SupplyRecord
import com.fearmikey.garage.ui.util.fromUtcDatePickerMillis
import com.fearmikey.garage.ui.util.parseToDoubleOrNull
import com.fearmikey.garage.ui.util.toDisplayDate
import com.fearmikey.garage.ui.util.toUtcDatePickerMillis
import java.util.Locale

/** "2" for whole quantities, otherwise up to two decimals in the current locale ("1.5", "1,5"). */
internal fun formatSupplyQuantity(quantity: Double): String =
    if (quantity % 1.0 == 0.0) {
        quantity.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", quantity).trimEnd('0').trimEnd('.', ',')
    }

/** Editable text for a number field: blank for zero, no trailing ".0" for whole numbers. */
private fun Double.toInputText(): String = when {
    this == 0.0 -> ""
    this % 1.0 == 0.0 -> toLong().toString()
    else -> toString()
}

@Composable
internal fun SupplyCard(
    supply: SupplyRecord,
    currencySymbol: String,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = supply.description,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (supply.cost > 0) {
                    Text(
                        text = "%s%.2f".format(currencySymbol, supply.cost),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val details = listOfNotNull(
                supply.partNumber.takeIf { it.isNotBlank() }?.let { "#$it" },
                supply.supplier.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (details.isNotEmpty()) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.supplies_qty_value, formatSupplyQuantity(supply.quantity)) +
                    " · " + supply.date.toDisplayDate(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (supply.notes.isNotBlank()) {
                Text(
                    text = supply.notes,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Add/edit dialog for a supply. [initial] with id 0 is a new (possibly pre-filled) supply;
 * [onDelete] is offered only for existing ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SupplyEditDialog(
    initial: SupplyRecord,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (SupplyRecord) -> Unit,
    onDelete: (SupplyRecord) -> Unit,
) {
    val isNew = initial.id == 0L
    var description by remember { mutableStateOf(initial.description) }
    var partNumber by remember { mutableStateOf(initial.partNumber) }
    var supplier by remember { mutableStateOf(initial.supplier) }
    var quantity by remember { mutableStateOf(initial.quantity.toInputText()) }
    var cost by remember { mutableStateOf(initial.cost.toInputText()) }
    var notes by remember { mutableStateOf(initial.notes) }
    var date by remember { mutableLongStateOf(initial.date) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.supplies_add_title else R.string.supplies_edit_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.supplies_description_label)) },
                    placeholder = { Text(stringResource(R.string.supplies_description_placeholder)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = partNumber,
                    onValueChange = { partNumber = it },
                    label = { Text(stringResource(R.string.supplies_part_number_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text(stringResource(R.string.supplies_supplier_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text(stringResource(R.string.supplies_quantity_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = cost,
                        onValueChange = { cost = it },
                        label = { Text(stringResource(R.string.supplies_cost_label)) },
                        prefix = { Text(currencySymbol) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = date.toDisplayDate(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.supplies_date_label)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = stringResource(R.string.doc_select_date))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.common_notes)) },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                enabled = description.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            description = description.trim(),
                            partNumber = partNumber.trim(),
                            supplier = supplier.trim(),
                            quantity = quantity.parseToDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                            cost = cost.parseToDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                            notes = notes.trim(),
                            date = date,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.common_save))
            }
        },
        dismissButton = {
            Row {
                if (!isNew) {
                    TextButton(onClick = { onDelete(initial) }) {
                        Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        },
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.toUtcDatePickerMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { date = it.fromUtcDatePickerMillis() }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.common_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
