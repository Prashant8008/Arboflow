package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BlockerSeverity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.domain.IntegrationAndReminderManager
import com.example.domain.TreeDependencyEngine
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.CrimsonBlocker

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorDialog(
  existingTask: TeamTask?,
  preselectedMember: TeamMember? = null,
  allTasks: List<TeamTask>,
  members: List<TeamMember>,
  onDismiss: () -> Unit,
  onSave: (
    taskKey: String,
    title: String,
    description: String,
    assigneeName: String,
    assigneeEmail: String,
    assigneeRole: String,
    parentTaskId: String,
    prerequisiteIds: List<String>,
    status: String,
    priority: String,
    progressPercent: Long,
    dueDateMillis: Long,
    reminderOffsetMinutes: Long,
    externalRef: String
  ) -> Unit,
  onDelete: ((TeamTask) -> Unit)? = null,
  onOpenReportBlocker: ((TeamTask) -> Unit)? = null,
  onPostMessage: (String) -> Unit
) {
  val context = LocalContext.current
  val nextKeyNumber = remember(allTasks) { 101 + allTasks.size }
  val defaultMember = preselectedMember ?: members.firstOrNull { !it.isSenior } ?: members.firstOrNull()

  var taskKey by remember { mutableStateOf(existingTask?.taskKey ?: "ARB-$nextKeyNumber") }
  var title by remember { mutableStateOf(existingTask?.title ?: "") }
  var description by remember { mutableStateOf(existingTask?.description ?: "") }
  var assigneeName by remember {
    mutableStateOf(
      existingTask?.assigneeName ?: defaultMember?.displayName ?: "Priya Sharma"
    )
  }
  var assigneeEmail by remember {
    mutableStateOf(
      existingTask?.assigneeEmail ?: defaultMember?.email ?: "priya.junior@gmail.com"
    )
  }
  var assigneeRole by remember {
    mutableStateOf(
      existingTask?.assigneeRole ?: defaultMember?.roleTitle ?: "Junior Engineer"
    )
  }
  var parentTaskId by remember { mutableStateOf(existingTask?.parentTaskId ?: "") }
  var prerequisiteIds by remember { mutableStateOf(existingTask?.prerequisiteIds ?: emptyList()) }
  var status by remember { mutableStateOf(existingTask?.status ?: TaskStatus.TODO.name) }
  var priority by remember { mutableStateOf(existingTask?.priority ?: TaskPriority.HIGH.name) }
  var progress by remember { mutableFloatStateOf((existingTask?.progressPercent ?: 0L).toFloat()) }
  var dueDaysFromNow by remember {
    val defaultDays = if (existingTask != null && existingTask.dueDateMillis > System.currentTimeMillis()) {
      ((existingTask.dueDateMillis - System.currentTimeMillis()) / 86_400_000L).coerceIn(1L, 30L)
    } else {
      3L
    }
    mutableLongStateOf(defaultDays)
  }
  var reminderOffset by remember { mutableLongStateOf(existingTask?.reminderOffsetMinutes ?: 60L) }
  var externalRef by remember { mutableStateOf(existingTask?.externalRef ?: "") }
  var cycleWarning by remember { mutableStateOf<String?>(null) }

  val candidateParents = remember(allTasks, existingTask) {
    allTasks.filter { it.id != existingTask?.id }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existingTask == null) "Assign Tree Task to Junior / Member" else "Edit Node [${existingTask.taskKey}]",
        style = MaterialTheme.typography.titleLarge
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = taskKey,
            onValueChange = { taskKey = it },
            label = { Text("Task Key") },
            singleLine = true,
            modifier = Modifier.weight(0.4f)
          )
          OutlinedTextField(
            value = externalRef,
            onValueChange = { externalRef = it },
            label = { Text("External PM Key (Jira/Linear)") },
            singleLine = true,
            modifier = Modifier.weight(0.6f)
          )
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Task / Milestone Title") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_title_input")
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Scope & Deliverable Details") },
          modifier = Modifier.fillMaxWidth()
        )

        // Assignee Picker with Gmail Identity
        Text(
          "SELECT JUNIOR / TEAM MEMBER (GMAIL SYNC)",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary
        )
        if (members.isNotEmpty()) {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            members.forEach { member ->
              val isSelected = assigneeName == member.displayName ||
                (member.email.isNotBlank() && assigneeEmail.equals(member.email, ignoreCase = true))
              val roleTag = if (member.isSenior) "Senior" else "Junior"
              FilterChip(
                selected = isSelected,
                onClick = {
                  assigneeName = member.displayName
                  assigneeEmail = member.email
                  assigneeRole = member.roleTitle
                },
                label = {
                  Text(
                    if (member.email.isNotBlank()) {
                      "${member.displayName} • $roleTag (${member.email})"
                    } else {
                      "${member.displayName} • $roleTag"
                    }
                  )
                }
              )
            }
          }
        }
        OutlinedTextField(
          value = assigneeName,
          onValueChange = { assigneeName = it },
          label = { Text("Assigned Junior / Member Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = assigneeEmail,
          onValueChange = { assigneeEmail = it },
          label = { Text("Assignee Gmail Address (e.g. junior@gmail.com)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("task_assignee_email_input")
        )

        // Parent Task in Tree Hierarchy
        if (candidateParents.isNotEmpty()) {
          Text(
            "PARENT NODE IN PROGRESS TREE",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
              selected = parentTaskId.isBlank(),
              onClick = { parentTaskId = "" },
              label = { Text("Root Milestone") }
            )
            candidateParents.forEach { parent ->
              FilterChip(
                selected = parentTaskId == parent.id,
                onClick = {
                  val selfId = existingTask?.id ?: "new_task"
                  if (TreeDependencyEngine.wouldCreateCycle(selfId, parent.id, allTasks)) {
                    cycleWarning = "Cannot set ${parent.taskKey} as parent (circular dependency)"
                  } else {
                    cycleWarning = null
                    parentTaskId = parent.id
                  }
                },
                label = { Text(parent.taskKey) }
              )
            }
          }
        }

        // Prerequisite Dependencies
        if (candidateParents.isNotEmpty()) {
          Text(
            "UPSTREAM PREREQUISITES (BLOCKERS IF DELAYED)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
          )
          FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            candidateParents.forEach { candidate ->
              val isSelected = candidate.id in prerequisiteIds
              FilterChip(
                selected = isSelected,
                onClick = {
                  val selfId = existingTask?.id ?: "new_task"
                  if (isSelected) {
                    prerequisiteIds = prerequisiteIds - candidate.id
                    cycleWarning = null
                  } else if (TreeDependencyEngine.wouldCreateCycle(selfId, candidate.id, allTasks)) {
                    cycleWarning = "Circular dependency prevented with ${candidate.taskKey}"
                  } else {
                    cycleWarning = null
                    prerequisiteIds = prerequisiteIds + candidate.id
                  }
                },
                label = { Text(candidate.taskKey) }
              )
            }
          }
        }

        if (cycleWarning != null) {
          Text(
            text = cycleWarning!!,
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonBlocker
          )
        }

        // Status & Priority
        Text("STATUS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TaskStatus.entries.forEach { st ->
            FilterChip(
              selected = status == st.name,
              onClick = {
                status = st.name
                if (st == TaskStatus.DONE) progress = 100f
              },
              label = { Text(st.label) }
            )
          }
        }

        Text("PRIORITY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TaskPriority.entries.forEach { pr ->
            FilterChip(
              selected = priority == pr.name,
              onClick = { priority = pr.name },
              label = { Text(pr.label) }
            )
          }
        }

        Text(
          text = "Completion Progress: ${progress.toInt()}%",
          style = MaterialTheme.typography.labelMedium
        )
        Slider(
          value = progress,
          onValueChange = { progress = it },
          valueRange = 0f..100f
        )

        // Deadline & Reminder Lead Time
        Text(
          text = "DEADLINE & REMINDER LEAD TIME",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(1L to "Tomorrow", 3L to "In 3 Days", 7L to "In 1 Week").forEach { (days, label) ->
            FilterChip(
              selected = dueDaysFromNow == days,
              onClick = { dueDaysFromNow = days },
              label = { Text(label) }
            )
          }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(15L to "15m Before", 60L to "1h Before", 1440L to "1d Before").forEach { (mins, label) ->
            FilterChip(
              selected = reminderOffset == mins,
              onClick = { reminderOffset = mins },
              label = { Text(label) }
            )
          }
        }

        if (existingTask != null) {
          Spacer(modifier = Modifier.height(6.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedButton(
              onClick = {
                IntegrationAndReminderManager.openSystemCalendarSync(context, existingTask)
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Calendar")
            }
            OutlinedButton(
              onClick = {
                IntegrationAndReminderManager.sendTaskReminderNotification(context, existingTask)
                onPostMessage("Sent local reminder for ${existingTask.taskKey}")
              },
              modifier = Modifier.weight(1f)
            ) {
              Icon(Icons.Default.NotificationsActive, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text("Remind")
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (onOpenReportBlocker != null) {
              OutlinedButton(
                onClick = {
                  onDismiss()
                  onOpenReportBlocker(existingTask)
                },
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = CrimsonBlocker)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Flag Blocker", color = CrimsonBlocker)
              }
            }
            if (onDelete != null) {
              OutlinedButton(
                onClick = {
                  onDelete(existingTask)
                  onDismiss()
                },
                modifier = Modifier.weight(1f)
              ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonBlocker)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete", color = CrimsonBlocker)
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            val dueMillis = System.currentTimeMillis() + (dueDaysFromNow * 86_400_000L)
            onSave(
              taskKey,
              title,
              description,
              assigneeName,
              assigneeEmail,
              assigneeRole,
              parentTaskId,
              prerequisiteIds,
              status,
              priority,
              progress.toLong(),
              dueMillis,
              reminderOffset,
              externalRef
            )
            onDismiss()
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("save_task_button")
      ) {
        Text(if (existingTask == null) "Assign Task" else "Save Changes")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReportBlockerDialog(
  targetTask: TeamTask,
  allTasks: List<TeamTask>,
  onDismiss: () -> Unit,
  onSubmitBlocker: (reason: String, severity: String, reporterName: String) -> Unit
) {
  var reason by remember {
    mutableStateOf(
      targetTask.blockerReason.ifBlank {
        "Upstream API contract mismatch blocking integration testing"
      }
    )
  }
  var severity by remember { mutableStateOf(BlockerSeverity.CRITICAL.name) }
  var reporter by remember { mutableStateOf(targetTask.assigneeName.ifBlank { "Team Member" }) }

  val impactedDownstream = remember(targetTask.id, allTasks) {
    TreeDependencyEngine.findCascadingImpactedTasks(targetTask.id, allTasks)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = "Flag Blocker on [${targetTask.taskKey}]",
        color = CrimsonBlocker
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = targetTask.title,
          style = MaterialTheme.typography.titleSmall
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Blocker Cause & Required Unblock Action") },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("blocker_reason_input")
        )
        OutlinedTextField(
          value = reporter,
          onValueChange = { reporter = it },
          label = { Text("Reported By") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        Text("SEVERITY", style = MaterialTheme.typography.labelSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          BlockerSeverity.entries.forEach { sev ->
            FilterChip(
              selected = severity == sev.name,
              onClick = { severity = sev.name },
              label = { Text(sev.label) }
            )
          }
        }

        // Blast-Radius Preview
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = AmberProgress.copy(alpha = 0.14f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "AUTOMATED DOWNSTREAM CASCADE PREVIEW",
              style = MaterialTheme.typography.labelSmall,
              color = AmberProgress
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (impactedDownstream.isEmpty()) {
              Text(
                text = "Leaf task node — no downstream dependent tasks will be locked.",
                style = MaterialTheme.typography.bodySmall
              )
            } else {
              Text(
                text = "${impactedDownstream.size} downstream task(s) will automatically transition to CASCADED_BLOCKED and alert Senior Command:",
                style = MaterialTheme.typography.bodySmall
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = impactedDownstream.joinToString(", ") { "${it.taskKey} (${it.title.take(20)})" },
                style = MaterialTheme.typography.labelMedium,
                color = CrimsonBlocker
              )
            }
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSubmitBlocker(reason, severity, reporter)
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonBlocker),
        modifier = Modifier.testTag("confirm_report_blocker_button")
      ) {
        Text("Trigger Blocker & Cascade", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
