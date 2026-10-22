package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockerAlert
import com.example.data.model.BlockerStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.domain.IntegrationAndReminderManager
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.CrimsonBlocker
import com.example.ui.theme.EmeraldPulse

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BlockersAndAlertsScreen(
  tasks: List<TeamTask>,
  blockers: List<BlockerAlert>,
  members: List<TeamMember>,
  onOpenReportBlocker: (TeamTask) -> Unit,
  onEscalateBlocker: (BlockerAlert, String, String) -> Unit,
  onResolveBlocker: (BlockerAlert, String) -> Unit,
  onPostMessage: (String) -> Unit
) {
  val context = LocalContext.current
  var filterStatus by remember { mutableStateOf("ALL") }

  val filteredBlockers = remember(blockers, filterStatus) {
    when (filterStatus) {
      "ACTIVE" -> blockers.filter { it.status != BlockerStatus.RESOLVED.name }
      "RESOLVED" -> blockers.filter { it.status == BlockerStatus.RESOLVED.name }
      else -> blockers
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("blockers_screen_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "AUTOMATED BLOCKER & CASCADE HUB",
            style = MaterialTheme.typography.labelMedium,
            color = CrimsonBlocker
          )
          Text(
            text = "Blocker Escalations & Audit Log",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
          )
        }
        if (tasks.isNotEmpty()) {
          Button(
            onClick = { onOpenReportBlocker(tasks.first()) },
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonBlocker),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("log_blocker_top_button")
          ) {
            Icon(Icons.Default.AddAlert, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Flag Blocker", color = Color.White)
          }
        }
      }
    }

    // Quick task selector for flagging a blocker on any node
    if (tasks.isNotEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "ONE-TAP BLOCKER TRIGGER BY NODE",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Select any task below to simulate or report a blocker and automatically cascade locks to downstream dependents:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              tasks.forEach { task ->
                FilterChip(
                  selected = task.status == "BLOCKED",
                  onClick = { onOpenReportBlocker(task) },
                  label = { Text("${task.taskKey}: ${task.title.take(18)}") }
                )
              }
            }
          }
        }
      }
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
          "ALL" to "All Alerts (${blockers.size})",
          "ACTIVE" to "Active / Escalated (${blockers.count { it.status != BlockerStatus.RESOLVED.name }})",
          "RESOLVED" to "Resolved (${blockers.count { it.status == BlockerStatus.RESOLVED.name }})"
        ).forEach { (key, label) ->
          FilterChip(
            selected = filterStatus == key,
            onClick = { filterStatus = key },
            label = { Text(label) }
          )
        }
      }
    }

    if (filteredBlockers.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, EmeraldPulse.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        ) {
          Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldPulse, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = "No Blocker Alerts in Current Filter",
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = "All dependencies are currently clear or resolved.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    } else {
      items(filteredBlockers, key = { it.id }) { alert ->
        val isResolved = alert.status == BlockerStatus.RESOLVED.name
        val accent = if (isResolved) EmeraldPulse else CrimsonBlocker
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                  contentDescription = null,
                  tint = accent,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "${alert.taskKey} • ${alert.severity}",
                  style = MaterialTheme.typography.labelLarge,
                  color = accent
                )
              }
              Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.16f)
              ) {
                Text(
                  text = alert.status,
                  style = MaterialTheme.typography.labelSmall,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = alert.taskTitle,
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = alert.reason,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (alert.impactedTaskKeys.isNotEmpty()) {
              Spacer(modifier = Modifier.height(8.dp))
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = AmberProgress.copy(alpha = 0.14f)
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Default.WarningAmber, contentDescription = null, tint = AmberProgress, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "Cascaded Downstream Lock: ${alert.impactedTaskKeys.joinToString(", ")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = AmberProgress
                  )
                }
              }
            }

            if (alert.resolutionNote.isNotBlank()) {
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "Command Note: ${alert.resolutionNote}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
              )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Reported by ${alert.reportedByName} • Assigned: ${alert.assigneeName}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isResolved) {
              Spacer(modifier = Modifier.height(12.dp))
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                OutlinedButton(
                  onClick = {
                    IntegrationAndReminderManager.sendBlockerNotification(context, alert)
                    onPostMessage("Sent local device notification for blocker [${alert.taskKey}]")
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Notify")
                }
                OutlinedButton(
                  onClick = {
                    val seniorName = members.firstOrNull { it.isSenior }?.displayName ?: "Elena Vance (VP Eng)"
                    onEscalateBlocker(
                      alert,
                      seniorName,
                      "Escalated to Senior Executive Command ($seniorName)"
                    )
                  },
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Text("Escalate")
                }
                Button(
                  onClick = {
                    onResolveBlocker(alert, "Resolved & downstream nodes unlocked")
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = EmeraldPulse),
                  modifier = Modifier.weight(1f),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Text("Resolve", color = Color.Black, fontWeight = FontWeight.Bold)
                }
              }
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }
}
