package com.fearmikey.garage.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.ui.util.AppCurrency

@Composable
fun CurrencySelectionDialog(
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            AppCurrency.SUPPORTED_CURRENCIES
        } else {
            AppCurrency.SUPPORTED_CURRENCIES.filter { currency ->
                currency.code.contains(searchQuery, ignoreCase = true) ||
                    currency.name.contains(searchQuery, ignoreCase = true) ||
                    currency.symbol.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Currency") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search currencies...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                ) {
                    if (filteredCurrencies.isEmpty()) {
                        item {
                            Text(
                                text = "No matching currencies found.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        }
                    } else {
                        items(filteredCurrencies, key = { it.code }) { currency ->
                            val isSelected = currency.code.equals(selectedCurrencyCode, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCurrencySelected(currency.code)
                                        onDismissRequest()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onCurrencySelected(currency.code)
                                        onDismissRequest()
                                    },
                                )
                                Spacer(modifier = Modifier.padding(start = 8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${currency.code} (${currency.symbol})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = currency.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        },
    )
}
