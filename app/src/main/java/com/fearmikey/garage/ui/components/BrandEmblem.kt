package com.fearmikey.garage.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fearmikey.garage.util.BrandEmblems

@Composable
fun BrandEmblem(
    make: String,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
) {
    val trimmedMake = make.trim()
    if (trimmedMake.isEmpty()) return

    val iconRes = BrandEmblems.forMake(trimmedMake)

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
        modifier = modifier.size(size),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size),
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = trimmedMake,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(size * 0.65f)
                        .padding(1.dp),
                )
            } else {
                Text(
                    text = trimmedMake.first().uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (size.value * 0.45f).sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
