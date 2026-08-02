package com.yourorg.systemcore.presentation.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.systemcore.core.ui.theme.CyanCore
import com.yourorg.systemcore.core.ui.theme.TextSecondary
import com.yourorg.systemcore.core.ui.theme.glassPanel
import com.yourorg.systemcore.domain.model.BatteryStatus

@Composable
fun BatteryPanel(
    status: BatteryStatus?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .glassPanel()
            .clickable(onClick = onToggle)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.BatteryChargingFull,
                    contentDescription = null,
                    tint = CyanCore
                )
                Text(
                    text = "  BATTERY",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status?.let { "${it.percentage}%" } ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = CyanCore
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = TextSecondary
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 14.dp)) {
                if (status == null) {
                    DetailRow(label = "Status", value = "Awaiting first reading…")
                } else {
                    DetailRow(label = "Charging", value = if (status.isCharging) "Yes" else "No")
                    DetailRow(label = "Power source", value = status.chargePlug.name)
                    DetailRow(label = "Health", value = status.health.name)
                    DetailRow(label = "Temperature", value = "${status.temperatureCelsius}°C")
                    DetailRow(label = "Voltage", value = "${status.voltageMillivolts} mV")
                    status.technology?.let { DetailRow(label = "Technology", value = it) }
                }
            }
        }
    }
}

