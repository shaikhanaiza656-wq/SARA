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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourorg.systemcore.core.ui.theme.CyanCore
import com.yourorg.systemcore.core.ui.theme.StatusCritical
import com.yourorg.systemcore.core.ui.theme.TextSecondary
import com.yourorg.systemcore.core.ui.theme.glassPanel
import com.yourorg.systemcore.domain.model.NetworkStatus

@Composable
fun NetworkPanel(
    status: NetworkStatus?,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = status?.isConnected == true

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
                    imageVector = if (connected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                    contentDescription = null,
                    tint = if (connected) CyanCore else StatusCritical
                )
                Text(
                    text = "  NETWORK",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = status?.transport?.name ?: "—",
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (connected) CyanCore else StatusCritical
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
                    DetailRow(label = "Connected", value = if (status.isConnected) "Yes" else "No")
                    DetailRow(label = "Metered", value = if (status.isMetered) "Yes" else "No")
                    DetailRow(label = "Validated", value = if (status.isValidated) "Yes" else "No")
                    DetailRow(label = "Downstream", value = "${status.downstreamKbps} kbps")
                    DetailRow(label = "Upstream", value = "${status.upstreamKbps} kbps")
                }
            }
        }
    }
}
