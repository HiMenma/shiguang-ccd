package app.shiguang.ccd.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun LcdText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = app.shiguang.ccd.ui.theme.CreamDim,
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    val amber = app.shiguang.ccd.ui.theme.Amber
    val ink = app.shiguang.ccd.ui.theme.Ink
    val cream = app.shiguang.ccd.ui.theme.Cream
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(if (enabled) cream else cream.copy(alpha = 0.25f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (enabled) amber else amber.copy(alpha = 0.25f)),
        )
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(2.dp, ink.copy(alpha = 0.75f), CircleShape),
        )
    }
}

@Composable
fun PresetChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val amber = app.shiguang.ccd.ui.theme.Amber
    val ink = app.shiguang.ccd.ui.theme.Ink
    val cream = app.shiguang.ccd.ui.theme.Cream
    val dim = app.shiguang.ccd.ui.theme.CreamDim
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) amber else Color.Transparent)
            .border(
                1.dp,
                if (selected) amber else dim.copy(alpha = 0.45f),
                RoundedCornerShape(6.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) ink else cream,
        )
    }
}

@Composable
fun ToolbarButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val cream = app.shiguang.ccd.ui.theme.Cream
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) cream else cream.copy(alpha = 0.35f),
        )
    }
}

@Composable
fun LabeledRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LcdText(label)
        LcdText(value, color = app.shiguang.ccd.ui.theme.Cream)
    }
}
