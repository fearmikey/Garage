package com.fearmikey.garage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fearmikey.garage.data.repository.ReminderStatus
import com.fearmikey.garage.ui.theme.GarageTheme
import com.fearmikey.garage.ui.theme.StatusOk
import com.fearmikey.garage.ui.theme.StatusOverdue
import com.fearmikey.garage.ui.theme.StatusUpcoming

/** Small colored pill summarizing a [ReminderStatus] (Overdue / Upcoming / OK / Done). */
@Composable
fun StatusChip(status: ReminderStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        ReminderStatus.OVERDUE -> "Overdue" to StatusOverdue
        ReminderStatus.UPCOMING -> "Upcoming" to StatusUpcoming
        ReminderStatus.OK -> "On track" to StatusOk
        ReminderStatus.COMPLETED -> "Done" to MaterialTheme.colorScheme.outline
    }
    AssistChip(
        onClick = {},
        enabled = false,
        modifier = modifier,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurface,
            disabledLeadingIconContentColor = color,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    GarageTheme {
        Row {
            StatusChip(ReminderStatus.OVERDUE)
            StatusChip(ReminderStatus.UPCOMING)
            StatusChip(ReminderStatus.OK)
            StatusChip(ReminderStatus.COMPLETED)
        }
    }
}
