package com.fearmikey.garage.ui.maintenance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R
import com.fearmikey.garage.ui.util.label
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.local.entity.MaintenanceCategory
import com.fearmikey.garage.ui.theme.GarageTheme

/**
 * Search field + horizontally scrolling filter chips shown above the maintenance Timeline.
 */
@Composable
internal fun TimelineFilterBar(
    filter: TimelineFilter,
    shownCount: Int,
    totalCount: Int,
    onQueryChanged: (String) -> Unit,
    onCategoryToggled: (MaintenanceCategory) -> Unit,
    onDateRangeSelected: (TimelineDateRange) -> Unit,
    onReceiptsOnlyToggled: () -> Unit,
    onHideAutoEntriesToggled: () -> Unit,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchVisible by rememberSaveable { mutableStateOf(filter.query.isNotEmpty()) }
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (searchVisible) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = onQueryChanged,
                placeholder = { Text(stringResource(R.string.maint_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = {
                        onQueryChanged("")
                        searchVisible = false
                    }) {
                        Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.maint_close_search))
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!searchVisible) {
                FilterChip(
                    selected = false,
                    onClick = { searchVisible = true },
                    label = { Text(stringResource(R.string.maint_search)) },
                    leadingIcon = { ChipIcon(Icons.Filled.Search) },
                )
            }
            CategoryFilterChip(filter.categories, onCategoryToggled)
            DateRangeFilterChip(filter.dateRange, onDateRangeSelected)
            FilterChip(
                selected = filter.receiptsOnly,
                onClick = onReceiptsOnlyToggled,
                label = { Text(stringResource(R.string.maint_has_receipt)) },
                leadingIcon = { ChipIcon(Icons.Filled.Receipt) },
            )
            FilterChip(
                selected = filter.hideAutoEntries,
                onClick = onHideAutoEntriesToggled,
                label = { Text(stringResource(R.string.maint_hide_auto)) },
                leadingIcon = { ChipIcon(Icons.Filled.VisibilityOff) },
            )
        }

        if (filter.isActive) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.maint_showing_count, shownCount, totalCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = {
                    onClearFilters()
                    searchVisible = false
                }) { Text(stringResource(R.string.maint_clear_filters)) }
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    selected: Set<MaintenanceCategory>,
    onCategoryToggled: (MaintenanceCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (selected.size) {
        0 -> stringResource(R.string.mods_category_label)
        1 -> selected.first().label()
        else -> pluralStringResource(R.plurals.maint_categories_count, selected.size, selected.size)
    }
    Box {
        FilterChip(
            selected = selected.isNotEmpty(),
            onClick = { expanded = true },
            label = { Text(label) },
            leadingIcon = { ChipIcon(Icons.Filled.Category) },
            trailingIcon = { ChipIcon(Icons.Filled.ArrowDropDown) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MaintenanceCategory.entries.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.label()) },
                    leadingIcon = {
                        Checkbox(checked = category in selected, onCheckedChange = null)
                    },
                    // Keep the menu open so several categories can be toggled in one go.
                    onClick = { onCategoryToggled(category) },
                )
            }
        }
    }
}

@Composable
private fun DateRangeFilterChip(
    selected: TimelineDateRange,
    onDateRangeSelected: (TimelineDateRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected != TimelineDateRange.ALL,
            onClick = { expanded = true },
            label = { Text(if (selected == TimelineDateRange.ALL) stringResource(R.string.maint_date) else selected.label()) },
            leadingIcon = { ChipIcon(Icons.Filled.DateRange) },
            trailingIcon = { ChipIcon(Icons.Filled.ArrowDropDown) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TimelineDateRange.entries.forEach { range ->
                DropdownMenuItem(
                    text = { Text(range.label()) },
                    leadingIcon = { RadioButton(selected = range == selected, onClick = null) },
                    onClick = {
                        onDateRangeSelected(range)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ChipIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
}

@Preview(showBackground = true)
@Composable
private fun TimelineFilterBarPreview() {
    GarageTheme {
        TimelineFilterBar(
            filter = TimelineFilter(
                categories = setOf(MaintenanceCategory.FLUIDS),
                dateRange = TimelineDateRange.LAST_12_MONTHS,
            ),
            shownCount = 3,
            totalCount = 12,
            onQueryChanged = {},
            onCategoryToggled = {},
            onDateRangeSelected = {},
            onReceiptsOnlyToggled = {},
            onHideAutoEntriesToggled = {},
            onClearFilters = {},
        )
    }
}
