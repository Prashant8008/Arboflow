package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BlockerAlert
import com.example.data.model.IntegrationConfig
import com.example.data.model.IntegrationProvider
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.domain.IntegrationAndReminderManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarAndIntegrationsScreen(
  tasks: List<TeamTask>,
  blockers: List<BlockerAlert>,
  members: List<TeamMember>,
  integrations: List<IntegrationConfig>,
  onImportExternalTasks: (rawPayload: String, provider: String, projectKey: String) -> Unit,
  onSaveAndTestIntegration: (
    existingConfig: IntegrationConfig?,
    provider: String,
    name: String,
    webhookUrl: String,
    projectKey: String,
    autoDispatchBlockers: Boolean,
    autoSyncCalendar: Boolean,
    testDispatch: Boolean
  ) -> Unit,
  onPostMessage: (String) -> Unit
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val databaseId = remember(context) {
    runCatching { context.getString(R.string.firestore_database_id) }
      .getOrDefault("ai-studio-android-arborflo-9c053938-150a-49a4-9c8c-dc2a7d40028f")
  }
  val firebaseProjectId = "gen-lang-client-0242699036"

  val existingConnector = integrations.firstOrNull()
  var selectedProvider by remember(existingConnector) {
    mutableStateOf(existingConnector?.provider ?: IntegrationProvider.JIRA.name)
  }
  var connectorName by remember(existingConnector) {
    mutableStateOf(existingConnector?.name ?: "Jira / Linear Cloud Bridge")
  }
  var webhookUrl by remember(existingConnector) {
    mutableStateOf(existingConnector?.webhookUrl ?: "https://httpbin.org/post")
  }
  var projectKey by remember(existingConnector) {
    mutableStateOf(existingConnector?.projectKey ?: "ARB")
  }
  var autoDispatchBlockers by remember(existingConnector) {
    mutableStateOf(existingConnector?.autoDispatchBlockers ?: true)
  }
  var autoSyncCalendar by remember(existingConnector) {
    mutableStateOf(existingConnector?.autoSyncCalendar ?: true)
  }

  var importFormat by remember { mutableStateOf("CSV") }
  var importPayload by remember {
    mutableStateOf(
      "Issue Key,Summary,Status,Priority,Assignee\n" +
        "JIRA-301,Deploy Global Edge Cache,IN_PROGRESS,HIGH,Marcus Chen\n" +
        "LIN-402,Audit OAuth2 Scopes & RBAC,TODO,CRITICAL,Priya Sharma"
    )
  }
  var generatedExportPreview by remember { mutableStateOf<String?>(null) }
  var modalPreviewTitle by remember { mutableStateOf<String?>(null) }
  var modalPreviewBody by remember { mutableStateOf<String?>(null) }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      tasks.firstOrNull()?.let {
        IntegrationAndReminderManager.sendTaskReminderNotification(context, it)
      }
      onPostMessage("Notifications enabled & reminder triggered!")
    } else {
      onPostMessage("Notification permission declined.")
    }
  }

  val dateFormat = remember { SimpleDateFormat("EEE, MMM d • HH:mm", Locale.US) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("calendar_integrations_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Column {
        Text(
          text = "CALENDAR, iOS BRIDGE & PM ECOSYSTEM",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary
        )
        Text(
          text = "Cross-Platform Sync & Connectors",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
    }

    // 1. iOS & Cross-Platform Companion Suite Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), RoundedCornerShape(20.dp))
          .testTag("ios_companion_suite_card")
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.PhoneIphone,
                contentDescription = "iOS Companion Suite",
                tint = MaterialTheme.colorScheme.secondary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "iOS & Web Companion Suite",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Support iPhone / iPad teammates & executives in real time",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.secondaryContainer
            ) {
              Text(
                text = "iOS READY",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
              )
            }
          }

          // Feature A: Shareable HTML5 Dashboard Report for iOS Safari & Web Browsers
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Public,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "1. Shareable HTML5 Dashboard for iOS Safari & Web",
                  style = MaterialTheme.typography.titleSmall
                )
              }
              Text(
                text = "Generates a standalone mobile-responsive HTML5 executive report with collapsible dependency trees, roll-up metrics, and active blocker cascades.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Button(
                  onClick = {
                    val html = IntegrationAndReminderManager.generateIosWebDashboardHtml(
                      tasks = tasks,
                      blockers = blockers,
                      members = members
                    )
                    clipboardManager.setText(AnnotatedString(html))
                    modalPreviewTitle = "iOS & Web HTML5 Dashboard Report"
                    modalPreviewBody = html
                    onPostMessage("Generated iOS/Web HTML5 Dashboard & copied to clipboard!")
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("generate_ios_html_report_button"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Preview HTML")
                }
                OutlinedButton(
                  onClick = {
                    val html = IntegrationAndReminderManager.generateIosWebDashboardHtml(
                      tasks = tasks,
                      blockers = blockers,
                      members = members
                    )
                    clipboardManager.setText(AnnotatedString(html))
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                      type = "text/html"
                      putExtra(Intent.EXTRA_SUBJECT, "ArborFlow Live Executive & Dependency Tree Report")
                      putExtra(Intent.EXTRA_TEXT, html)
                    }
                    runCatching {
                      context.startActivity(Intent.createChooser(shareIntent, "Share iOS / Web Dashboard"))
                    }
                    onPostMessage("Sharing iOS/Web HTML Dashboard report")
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("share_ios_html_report_button"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Share to iOS")
                }
              }
            }
          }

          // Feature B: Apple Calendar .ICS Invite & WebCal Link Generator
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Link,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "2. Apple Calendar (.ICS) & WebCal Link Generator",
                  style = MaterialTheme.typography.titleSmall
                )
              }
              Text(
                text = "Includes Apple Calendar X-WR-CALNAME headers, native iOS lock-screen VALARM audio/display triggers, and webcal:// subscription links.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Button(
                  onClick = {
                    val sampleTask = tasks.firstOrNull() ?: TeamTask(
                      taskKey = "ARB-100",
                      title = "ArborFlow Sprint Milestone",
                      assigneeName = "Team"
                    )
                    val links = IntegrationAndReminderManager.generateAppleCalendarWebCalAndUniversalLink(sampleTask)
                    clipboardManager.setText(AnnotatedString(links))
                    modalPreviewTitle = "Apple Calendar WebCal & Universal Links"
                    modalPreviewBody = links + "\n\n---\n\n" + IntegrationAndReminderManager.generateIcsSchedule(tasks)
                    onPostMessage("Copied Apple Calendar WebCal & Universal links to clipboard!")
                  },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("generate_webcal_link_button"),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("WebCal & .ICS")
                }
              }
            }
          }

          // Feature C: Cross-Platform Firebase Config & iOS SwiftUI Setup Exporter
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Code,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "3. Cross-Platform iOS SwiftUI Firebase Kit",
                  style = MaterialTheme.typography.titleSmall
                )
              }
              Text(
                text = "Project ID: $firebaseProjectId\nDatabase ID: ${databaseId.take(36)}…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
              )
              Text(
                text = "Export ready-to-compile Swift Codable models (TeamTask, BlockerAlert) and real-time Firestore listeners wired to your shared backend.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Button(
                onClick = {
                  val swiftCode = IntegrationAndReminderManager.generateSwiftUiFirebaseBridgeCode(
                    projectId = firebaseProjectId,
                    databaseId = databaseId
                  )
                  clipboardManager.setText(AnnotatedString(swiftCode))
                  modalPreviewTitle = "ArborFlowiOSBridge.swift (SwiftUI + Firestore)"
                  modalPreviewBody = swiftCode
                  onPostMessage("Copied iOS SwiftUI Firebase Bridge code to clipboard!")
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("export_swiftui_bridge_button"),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export iOS SwiftUI Firebase Kit")
              }
            }
          }
        }
      }
    }

    // 2. Built-In Team Calendar Schedule & ICS Export + Device Reminders
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Team Milestone Calendar & Reminders",
                style = MaterialTheme.typography.titleMedium
              )
            }
          }

          Text(
            text = "Sync task deadlines with your Android device calendar, trigger local reminder alerts, or export an Apple/Google Calendar .ics feed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Button(
              onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  tasks.firstOrNull()?.let {
                    IntegrationAndReminderManager.sendTaskReminderNotification(context, it)
                  }
                  onPostMessage("Triggered reminder notification!")
                }
              },
              modifier = Modifier
                .weight(1f)
                .testTag("trigger_all_reminders_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Test Reminder")
            }

            OutlinedButton(
              onClick = {
                val ics = IntegrationAndReminderManager.generateIcsSchedule(tasks)
                generatedExportPreview = ics
                clipboardManager.setText(AnnotatedString(ics))
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "text/calendar"
                  putExtra(Intent.EXTRA_SUBJECT, "ArborFlow Team Schedule (.ics)")
                  putExtra(Intent.EXTRA_TEXT, ics)
                }
                runCatching {
                  context.startActivity(Intent.createChooser(shareIntent, "Export .ICS Schedule"))
                }
                onPostMessage("Generated Apple & Google Calendar .ICS schedule (${tasks.size} events)")
              },
              modifier = Modifier
                .weight(1f)
                .testTag("export_ics_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export .ICS")
            }
          }

          val scheduledTasks = remember(tasks) {
            tasks.sortedBy { if (it.dueDateMillis > 0L) it.dueDateMillis else Long.MAX_VALUE }
          }
          scheduledTasks.take(5).forEach { task ->
            val statusColor = statusColorFor(task.status)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "[${task.taskKey}] ${task.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  val dueStr = if (task.dueDateMillis > 0L) {
                    dateFormat.format(Date(task.dueDateMillis))
                  } else {
                    "Unscheduled"
                  }
                  Text(
                    text = "$dueStr • Reminder: ${task.reminderOffsetMinutes}m before • ${task.assigneeName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                  )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  OutlinedButton(
                    onClick = {
                      val links = IntegrationAndReminderManager.generateAppleCalendarWebCalAndUniversalLink(task)
                      clipboardManager.setText(AnnotatedString(links))
                      onPostMessage("Copied iOS WebCal & Universal calendar link for ${task.taskKey}")
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.Link, contentDescription = "Copy iOS Calendar Link", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("iOS Link", style = MaterialTheme.typography.labelSmall)
                  }
                  OutlinedButton(
                    onClick = {
                      IntegrationAndReminderManager.openSystemCalendarSync(context, task)
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp)
                  ) {
                    Icon(Icons.Default.Alarm, contentDescription = "Add to Android Calendar", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Sync Cal", style = MaterialTheme.typography.labelSmall)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Jira, Linear & Trello Import / Export Studio
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Jira, Linear & Trello Task Import / Export",
            style = MaterialTheme.typography.titleMedium
          )
          Text(
            text = "Import sprint issues from CSV or JSON directly into your ArborFlow dependency tree, or export the current tree.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("CSV", "JSON").forEach { fmt ->
              FilterChip(
                selected = importFormat == fmt,
                onClick = { importFormat = fmt },
                label = { Text("$fmt Format") }
              )
            }
          }

          OutlinedTextField(
            value = importPayload,
            onValueChange = { importPayload = it },
            label = { Text("Paste Jira / Linear / Trello CSV or JSON Payload") },
            minLines = 3,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pm_import_payload_input")
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Button(
              onClick = {
                onImportExternalTasks(importPayload, selectedProvider, projectKey)
              },
              modifier = Modifier
                .weight(1f)
                .testTag("import_tasks_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Import into Tree")
            }

            OutlinedButton(
              onClick = {
                val exported = IntegrationAndReminderManager.exportTasksForPmTool(tasks, importFormat)
                generatedExportPreview = exported
                clipboardManager.setText(AnnotatedString(exported))
                onPostMessage("Exported ${tasks.size} tasks as $importFormat & copied to clipboard")
              },
              modifier = Modifier
                .weight(1f)
                .testTag("export_tasks_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Export $importFormat")
            }
          }

          if (generatedExportPreview != null) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = "EXPORT PREVIEW (COPIED TO CLIPBOARD)",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = generatedExportPreview!!.take(600),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurface
                )
              }
            }
          }
        }
      }
    }

    // 4. Two-Way Webhook & REST API Connector Configuration
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CloudSync,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Two-Way Webhook & REST API Connector",
              style = MaterialTheme.typography.titleMedium
            )
          }

          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntegrationProvider.entries.forEach { provider ->
              FilterChip(
                selected = selectedProvider == provider.name,
                onClick = { selectedProvider = provider.name },
                label = { Text(provider.label) }
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = connectorName,
              onValueChange = { connectorName = it },
              label = { Text("Connector Label") },
              singleLine = true,
              modifier = Modifier.weight(0.65f)
            )
            OutlinedTextField(
              value = projectKey,
              onValueChange = { projectKey = it },
              label = { Text("Project Key") },
              singleLine = true,
              modifier = Modifier.weight(0.35f)
            )
          }

          OutlinedTextField(
            value = webhookUrl,
            onValueChange = { webhookUrl = it },
            label = { Text("REST Webhook Endpoint URL (HTTPS)") },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("webhook_url_input")
          )

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = autoDispatchBlockers,
              onCheckedChange = { autoDispatchBlockers = it }
            )
            Text(
              text = "Auto-dispatch JSON webhook payload when blockers cascade",
              style = MaterialTheme.typography.bodySmall
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = autoSyncCalendar,
              onCheckedChange = { autoSyncCalendar = it }
            )
            Text(
              text = "Auto-attach reminder alarms to exported calendar schedules",
              style = MaterialTheme.typography.bodySmall
            )
          }

          if (existingConnector != null) {
            Text(
              text = "Last Status: ${existingConnector.lastSyncSummary}",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary
            )
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedButton(
              onClick = {
                onSaveAndTestIntegration(
                  existingConnector,
                  selectedProvider,
                  connectorName,
                  webhookUrl,
                  projectKey,
                  autoDispatchBlockers,
                  autoSyncCalendar,
                  false
                )
              },
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save Config")
            }

            Button(
              onClick = {
                onSaveAndTestIntegration(
                  existingConnector,
                  selectedProvider,
                  connectorName,
                  webhookUrl,
                  projectKey,
                  autoDispatchBlockers,
                  autoSyncCalendar,
                  true
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("test_webhook_button"),
              shape = RoundedCornerShape(12.dp)
            ) {
              Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save & Ping Webhook")
            }
          }
        }
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }

  if (modalPreviewTitle != null && modalPreviewBody != null) {
    AlertDialog(
      onDismissRequest = {
        modalPreviewTitle = null
        modalPreviewBody = null
      },
      title = {
        Text(
          text = modalPreviewTitle!!,
          style = MaterialTheme.typography.titleMedium
        )
      },
      text = {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .padding(12.dp)
              .height(320.dp)
              .verticalScroll(rememberScrollState())
          ) {
            Text(
              text = modalPreviewBody!!,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val body = modalPreviewBody ?: ""
            clipboardManager.setText(AnnotatedString(body))
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
              type = "text/plain"
              putExtra(Intent.EXTRA_SUBJECT, modalPreviewTitle)
              putExtra(Intent.EXTRA_TEXT, body)
            }
            runCatching {
              context.startActivity(Intent.createChooser(shareIntent, modalPreviewTitle))
            }
            modalPreviewTitle = null
            modalPreviewBody = null
          }
        ) {
          Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Copy & Share")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            modalPreviewTitle = null
            modalPreviewBody = null
          }
        ) {
          Text("Close")
        }
      }
    )
  }
}
