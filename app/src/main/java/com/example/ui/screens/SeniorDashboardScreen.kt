package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AssignmentInd
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BlockerAlert
import com.example.data.model.BlockerStatus
import com.example.data.model.SHARED_WORKSPACE_ID
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.domain.IntegrationAndReminderManager
import com.example.domain.TreeDependencyEngine
import com.example.ui.RoleViewMode
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.CrimsonBlocker
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldPulse

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeniorDashboardScreen(
  tasks: List<TeamTask>,
  blockers: List<BlockerAlert>,
  members: List<TeamMember>,
  currentUserEmail: String,
  currentUserDisplayName: String,
  roleMode: RoleViewMode,
  selectedAssigneeFilter: String?,
  isBusy: Boolean,
  onRoleModeChange: (RoleViewMode) -> Unit,
  onSelectAssigneeFilter: (String?) -> Unit,
  onSeedSampleSprint: () -> Unit,
  onOpenCreateTask: () -> Unit,
  onAssignTaskToMember: (TeamMember) -> Unit,
  onOpenTaskDetail: (TeamTask) -> Unit,
  onQuickAdvanceTask: (TeamTask) -> Unit,
  onOpenReportBlocker: (TeamTask) -> Unit,
  onEscalateBlocker: (BlockerAlert, String, String) -> Unit,
  onResolveBlocker: (BlockerAlert, String) -> Unit,
  onAddTeamMember: (String, String, String, String, Boolean, Long) -> Unit,
  onNavigateToTree: () -> Unit,
  onPostMessage: (String) -> Unit
) {
  val context = LocalContext.current
  var showAddMemberDialog by remember { mutableStateOf(false) }
  var escalatingAlert by remember { mutableStateOf<BlockerAlert?>(null) }

  val treeRoots = remember(tasks) { TreeDependencyEngine.buildTaskTree(tasks) }
  val overallRollUp = remember(treeRoots, tasks) {
    if (treeRoots.isNotEmpty()) {
      treeRoots.map { it.rolledUpProgress }.average().toInt()
    } else if (tasks.isNotEmpty()) {
      tasks.map { it.progressPercent }.average().toInt()
    } else {
      0
    }
  }

  val activeBlockers = remember(blockers) {
    blockers.filter { it.status != BlockerStatus.RESOLVED.name }
  }
  val blockedOrCascadedTasks = remember(tasks) {
    tasks.filter {
      it.status == TaskStatus.BLOCKED.name || it.status == TaskStatus.CASCADED_BLOCKED.name
    }
  }
  val doneCount = remember(tasks) { tasks.count { it.status == TaskStatus.DONE.name } }
  val inProgressCount = remember(tasks) {
    tasks.count { it.status == TaskStatus.IN_PROGRESS.name || it.status == TaskStatus.IN_REVIEW.name }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("dashboard_lazy_column"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Role Switcher & Shared Gmail Workspace Banner
    item {
      Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Box(modifier = Modifier.fillMaxWidth()) {
          Image(
            painter = painterResource(id = R.drawable.img_command_hero),
            contentDescription = "Command Header",
            modifier = Modifier
              .fillMaxWidth()
              .height(172.dp),
            contentScale = ContentScale.Crop
          )
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(172.dp)
              .background(
                Brush.horizontalGradient(
                  listOf(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    MaterialTheme.colorScheme.background.copy(alpha = 0.68f)
                  )
                )
              )
          )
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = if (roleMode == RoleViewMode.SENIOR_COMMAND) {
                    "SENIOR EXECUTIVE DASHBOARD • WORKSPACE $SHARED_WORKSPACE_ID"
                  } else {
                    "JUNIOR / MEMBER WORKSPACE • WORKSPACE $SHARED_WORKSPACE_ID"
                  },
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = if (roleMode == RoleViewMode.SENIOR_COMMAND) {
                    "Assign Tasks by Gmail & Track Live Tree"
                  } else {
                    "My Gmail Deliverables & Upstream Sync"
                  },
                  style = MaterialTheme.typography.headlineSmall,
                  color = MaterialTheme.colorScheme.onBackground
                )
                if (currentUserEmail.isNotBlank()) {
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "Connected Gmail: $currentUserEmail • All Gmail team members synced live",
                    style = MaterialTheme.typography.bodySmall,
                    color = EmeraldPulse
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Role-Based View Mode Switcher
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f))
                .padding(4.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              RoleViewMode.entries.forEach { mode ->
                val selected = roleMode == mode
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                  modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { onRoleModeChange(mode) }
                    .testTag("role_mode_${mode.name.lowercase()}")
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Text(
                      text = mode.label,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 2. Empty Workspace Zero-State Card
    if (tasks.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.AutoAwesomeMotion,
              contentDescription = "Initialize Workspace",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Launch Your Shared Gmail Team Tree",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Everyone who signs in with their Gmail automatically joins this shared workspace ($SHARED_WORKSPACE_ID). Load a sample sprint or assign a new task to any Junior's Gmail.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = onSeedSampleSprint,
                enabled = !isBusy,
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("seed_sample_sprint_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text(if (isBusy) "Seeding..." else "Load Sample Sprint")
              }
              OutlinedButton(
                onClick = onOpenCreateTask,
                modifier = Modifier
                  .weight(1f)
                  .height(48.dp)
                  .testTag("empty_create_task_button"),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Assign Task")
              }
            }
          }
        }
      }
    } else {
      // 3. Executive KPI Telemetry Strip
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          KpiMetricCard(
            title = "TREE ROLL-UP",
            value = "$overallRollUp%",
            subtitle = "$doneCount/${tasks.size} nodes done",
            accentColor = EmeraldPulse,
            progressRing = overallRollUp / 100f,
            modifier = Modifier.weight(1f)
          )
          KpiMetricCard(
            title = "BLOCKERS",
            value = "${activeBlockers.size}",
            subtitle = "${blockedOrCascadedTasks.size} nodes locked",
            accentColor = if (blockedOrCascadedTasks.isNotEmpty()) CrimsonBlocker else EmeraldPulse,
            progressRing = null,
            modifier = Modifier.weight(1f)
          )
          KpiMetricCard(
            title = "ACTIVE FLOW",
            value = "$inProgressCount",
            subtitle = "${members.size.coerceAtLeast(1)} Gmail members",
            accentColor = CyanTelemetry,
            progressRing = null,
            modifier = Modifier.weight(1f)
          )
        }
      }

      if (roleMode == RoleViewMode.SENIOR_COMMAND) {
        // 4. Senior Executive View: Real-Time Blocker Escalation Feed
        item {
          SectionHeaderRow(
            title = "Real-Time Blocker & Cascade Feed",
            badgeText = "${activeBlockers.size} ACTIVE",
            badgeColor = if (activeBlockers.isNotEmpty()) CrimsonBlocker else EmeraldPulse,
            actionLabel = "View Tree",
            onActionClick = onNavigateToTree
          )
        }

        if (activeBlockers.isEmpty()) {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, EmeraldPulse.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            ) {
              Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Zero Blockers",
                  tint = EmeraldPulse,
                  modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                  Text(
                    text = "Zero Active Blockers Across Dependency Tree",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = "All milestones and downstream branches are flowing without obstruction.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        } else {
          items(activeBlockers, key = { it.id }) { alert ->
            ExecutiveBlockerCard(
              alert = alert,
              onNotifyDevice = {
                val sent = IntegrationAndReminderManager.sendBlockerNotification(context, alert)
                onPostMessage(
                  if (sent) "Dispatched high-priority blocker notification for ${alert.taskKey}"
                  else "Notification queued (grant notification permission in system settings)"
                )
              },
              onEscalateClick = { escalatingAlert = alert },
              onResolveClick = {
                onResolveBlocker(alert, "Resolved from Senior Executive Command Dashboard")
              }
            )
          }
        }

        // 5. Senior Executive View: Team Roster, Gmail Assignment & Workload Charts
        item {
          WorkloadAndVelocityCard(
            tasks = tasks,
            members = members,
            selectedAssignee = selectedAssigneeFilter,
            onSelectAssignee = onSelectAssigneeFilter,
            onAddMemberClick = { showAddMemberDialog = true },
            onAssignTaskToMember = onAssignTaskToMember
          )
        }
      } else {
        // 4B. Junior / Member Focus View: Filter by My Gmail or Any Team Member
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              text = "FILTER BY GMAIL / TEAM MEMBER FOCUS",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary
            )
            val allAssignees = remember(tasks, members) {
              (members.map { it.displayName } + tasks.map { it.assigneeName })
                .filter { it.isNotBlank() }
                .distinct()
            }
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              FilterChip(
                selected = selectedAssigneeFilter == null,
                onClick = { onSelectAssigneeFilter(null) },
                label = { Text("All Team (${tasks.size})") }
              )
              if (currentUserEmail.isNotBlank()) {
                val myEmailKey = "gmail:$currentUserEmail"
                FilterChip(
                  selected = selectedAssigneeFilter == myEmailKey,
                  onClick = { onSelectAssigneeFilter(myEmailKey) },
                  label = { Text("Assigned to My Gmail ($currentUserEmail)") }
                )
              }
              allAssignees.forEach { name ->
                FilterChip(
                  selected = selectedAssigneeFilter == name,
                  onClick = { onSelectAssigneeFilter(name) },
                  label = { Text(name) }
                )
              }
            }
          }
        }
      }

      // 6. Live Task Status & Dependency Stream
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (selectedAssigneeFilter != null) {
                "TASKS ASSIGNED TO ${selectedAssigneeFilter.removePrefix("gmail:").uppercase()}"
              } else {
                "LIVE TEAM TASK & DEPENDENCY STREAM"
              },
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary
            )
            Text(
              text = "Tap any task to inspect tree dependencies, reassign by Gmail, or log a blocker",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          if (selectedAssigneeFilter != null) {
            TextButton(onClick = { onSelectAssigneeFilter(null) }) {
              Icon(Icons.Default.FilterAltOff, contentDescription = "Clear Filter", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear")
            }
          }
        }
      }

      val filteredTasks = if (selectedAssigneeFilter != null) {
        if (selectedAssigneeFilter.startsWith("gmail:")) {
          val targetEmail = selectedAssigneeFilter.removePrefix("gmail:")
          tasks.filter {
            it.assigneeEmail.equals(targetEmail, ignoreCase = true) ||
              (currentUserDisplayName.isNotBlank() && it.assigneeName.equals(currentUserDisplayName, ignoreCase = true))
          }
        } else {
          tasks.filter { it.assigneeName == selectedAssigneeFilter }
        }
      } else {
        tasks
      }

      items(filteredTasks, key = { it.id }) { task ->
        DashboardTaskCard(
          task = task,
          allTasks = tasks,
          onCardClick = { onOpenTaskDetail(task) },
          onQuickAdvance = { onQuickAdvanceTask(task) },
          onReportBlocker = { onOpenReportBlocker(task) },
          onRemindClick = {
            IntegrationAndReminderManager.sendTaskReminderNotification(context, task)
            onPostMessage("Reminder notification triggered for [${task.taskKey}]")
          }
        )
      }
    }

    item { Spacer(modifier = Modifier.height(72.dp)) }
  }

  if (showAddMemberDialog) {
    AddTeamMemberDialog(
      onDismiss = { showAddMemberDialog = false },
      onConfirm = { name, email, role, dept, isSenior, hours ->
        onAddTeamMember(name, email, role, dept, isSenior, hours)
        showAddMemberDialog = false
      }
    )
  }

  if (escalatingAlert != null) {
    val alert = escalatingAlert!!
    EscalateBlockerDialog(
      alert = alert,
      members = members,
      onDismiss = { escalatingAlert = null },
      onConfirm = { newOwner, note ->
        onEscalateBlocker(alert, newOwner, note)
        escalatingAlert = null
      }
    )
  }
}

@Composable
private fun KpiMetricCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  progressRing: Float?,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    modifier = modifier.border(
      1.dp,
      accentColor.copy(alpha = 0.35f),
      RoundedCornerShape(18.dp)
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = accentColor
        )
        if (progressRing != null) {
          Canvas(modifier = Modifier.size(22.dp)) {
            drawArc(
              color = accentColor.copy(alpha = 0.2f),
              startAngle = 0f,
              sweepAngle = 360f,
              useCenter = false,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
              color = accentColor,
              startAngle = -90f,
              sweepAngle = 360f * progressRing.coerceIn(0f, 1f),
              useCenter = false,
              style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SectionHeaderRow(
  title: String,
  badgeText: String,
  badgeColor: Color,
  actionLabel: String,
  onActionClick: () -> Unit
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.width(8.dp))
      Surface(
        shape = CircleShape,
        color = badgeColor.copy(alpha = 0.18f),
        modifier = Modifier.border(1.dp, badgeColor.copy(alpha = 0.5f), CircleShape)
      ) {
        Text(
          text = badgeText,
          style = MaterialTheme.typography.labelSmall,
          color = badgeColor,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }
    TextButton(onClick = onActionClick) {
      Icon(Icons.Default.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(actionLabel, style = MaterialTheme.typography.labelLarge)
    }
  }
}

@Composable
private fun ExecutiveBlockerCard(
  alert: BlockerAlert,
  onNotifyDevice: () -> Unit,
  onEscalateClick: () -> Unit,
  onResolveClick: () -> Unit
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, CrimsonBlocker.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
      .testTag("blocker_card_${alert.taskKey}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Blocker Alert",
            tint = CrimsonBlocker,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "${alert.taskKey} • ${alert.severity}",
            style = MaterialTheme.typography.labelLarge,
            color = CrimsonBlocker
          )
          if (alert.status == BlockerStatus.ESCALATED.name) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
              shape = CircleShape,
              color = CrimsonBlocker
            ) {
              Text(
                text = "ESCALATED",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
              )
            }
          }
        }
        TextButton(onClick = onNotifyDevice) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = "Send Alert Notification",
            tint = CrimsonBlocker,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Ping Alert", color = CrimsonBlocker, style = MaterialTheme.typography.labelMedium)
        }
      }

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
          color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.WarningAmber,
              contentDescription = "Cascading Impact",
              tint = AmberProgress,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Cascaded Lock to Downstream: ${alert.impactedTaskKeys.joinToString(", ")}",
              style = MaterialTheme.typography.labelMedium,
              color = AmberProgress
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Reported by ${alert.reportedByName} • Owner: ${alert.assigneeName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))
      Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedButton(
          onClick = onEscalateClick,
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Escalate / Reassign")
        }
        Button(
          onClick = onResolveClick,
          colors = ButtonDefaults.buttonColors(containerColor = EmeraldPulse),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text("Resolve & Unlock", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun WorkloadAndVelocityCard(
  tasks: List<TeamTask>,
  members: List<TeamMember>,
  selectedAssignee: String?,
  onSelectAssignee: (String?) -> Unit,
  onAddMemberClick: () -> Unit,
  onAssignTaskToMember: (TeamMember) -> Unit
) {
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Icon(
            imageVector = Icons.Default.TrendingUp,
            contentDescription = "Workload & Velocity",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Gmail Team Roster & Junior Task Assignment",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        TextButton(onClick = onAddMemberClick) {
          Icon(Icons.Default.GroupAdd, contentDescription = "Add Junior by Gmail", modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("+ Add Gmail")
        }
      }

      Text(
        text = "Anyone joining with Gmail appears here automatically. Tap 'Assign Task' on any Junior to set their tasks.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(12.dp))

      val groupedByMember = remember(tasks, members) {
        val names = (members.map { it.displayName } + tasks.map { it.assigneeName })
          .filter { it.isNotBlank() }
          .distinct()
        names.map { name ->
          val memberObj = members.find { it.displayName == name }
          val memberTasks = tasks.filter {
            it.assigneeName == name ||
              (memberObj != null && memberObj.email.isNotBlank() && it.assigneeEmail.equals(memberObj.email, ignoreCase = true))
          }
          val done = memberTasks.count { it.status == TaskStatus.DONE.name }
          val active = memberTasks.count {
            it.status == TaskStatus.IN_PROGRESS.name ||
              it.status == TaskStatus.IN_REVIEW.name ||
              it.status == TaskStatus.TODO.name
          }
          val blocked = memberTasks.count {
            it.status == TaskStatus.BLOCKED.name || it.status == TaskStatus.CASCADED_BLOCKED.name
          }
          val role = memberObj?.roleTitle ?: memberTasks.firstOrNull()?.assigneeRole.orEmpty()
          val email = memberObj?.email ?: memberTasks.firstOrNull()?.assigneeEmail.orEmpty()
          val isSenior = memberObj?.isSenior ?: false
          val resolvedMember = memberObj ?: TeamMember(
            displayName = name,
            email = email,
            roleTitle = role.ifBlank { "Junior Engineer" },
            isSenior = isSenior
          )
          MemberWorkloadStat(name, email, role, isSenior, done, active, blocked, memberTasks.size, resolvedMember)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        groupedByMember.forEach { stat ->
          val isSelected = selectedAssignee == stat.name
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isSelected) {
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            } else {
              MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectAssignee(stat.name) }
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      text = stat.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                      shape = CircleShape,
                      color = if (stat.isSenior) ElectricIndigo.copy(alpha = 0.22f) else EmeraldPulse.copy(alpha = 0.22f)
                    ) {
                      Text(
                        text = if (stat.isSenior) "SENIOR" else "JUNIOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stat.isSenior) ElectricIndigo else EmeraldPulse,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }
                  if (stat.email.isNotBlank()) {
                    Text(
                      text = stat.email,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.secondary
                    )
                  }
                  if (stat.role.isNotBlank()) {
                    Text(
                      text = stat.role,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
                OutlinedButton(
                  onClick = { onAssignTaskToMember(stat.member) },
                  shape = RoundedCornerShape(10.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Icon(Icons.Default.AssignmentInd, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Assign Task", style = MaterialTheme.typography.labelMedium)
                }
              }
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = "${stat.done} Done • ${stat.active} Active • ${stat.blocked} Blocked",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
              Spacer(modifier = Modifier.height(6.dp))
              Canvas(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(10.dp)
                  .clip(RoundedCornerShape(5.dp))
              ) {
                val total = stat.total.coerceAtLeast(1).toFloat()
                val doneW = size.width * (stat.done / total)
                val activeW = size.width * (stat.active / total)
                val blockedW = size.width * (stat.blocked / total)

                drawRoundRect(
                  color = Color(0xFF1E293B),
                  size = size,
                  cornerRadius = CornerRadius(5.dp.toPx())
                )
                if (doneW > 0f) {
                  drawRect(
                    color = EmeraldPulse,
                    topLeft = Offset(0f, 0f),
                    size = Size(doneW, size.height)
                  )
                }
                if (activeW > 0f) {
                  drawRect(
                    color = ElectricIndigo,
                    topLeft = Offset(doneW, 0f),
                    size = Size(activeW, size.height)
                  )
                }
                if (blockedW > 0f) {
                  drawRect(
                    color = CrimsonBlocker,
                    topLeft = Offset(doneW + activeW, 0f),
                    size = Size(blockedW, size.height)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class MemberWorkloadStat(
  val name: String,
  val email: String,
  val role: String,
  val isSenior: Boolean,
  val done: Int,
  val active: Int,
  val blocked: Int,
  val total: Int,
  val member: TeamMember
)

@Composable
private fun DashboardTaskCard(
  task: TeamTask,
  allTasks: List<TeamTask>,
  onCardClick: () -> Unit,
  onQuickAdvance: () -> Unit,
  onReportBlocker: () -> Unit,
  onRemindClick: () -> Unit
) {
  val statusColor = statusColorFor(task.status)
  val parentTask = remember(task.parentTaskId, allTasks) {
    allTasks.find { it.id == task.parentTaskId }
  }
  val prereqKeys = remember(task.prerequisiteIds, allTasks) {
    task.prerequisiteIds.mapNotNull { id -> allTasks.find { it.id == id }?.taskKey }
  }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
      .clickable { onCardClick() }
      .testTag("task_item_${task.taskKey}")
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.16f)
          ) {
            Text(
              text = task.taskKey,
              style = MaterialTheme.typography.labelLarge,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
          ) {
            Text(
              text = task.status.replace("_", " "),
              style = MaterialTheme.typography.labelSmall,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
          }
          if (task.externalRef.isNotBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "• ${task.externalRef}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
        Text(
          text = "${task.progressPercent}%",
          style = MaterialTheme.typography.labelLarge,
          color = statusColor
        )
      }

      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = task.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
      )

      if (parentTask != null || prereqKeys.isNotEmpty()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = buildString {
            if (parentTask != null) append("Parent: ${parentTask.taskKey} ")
            if (prereqKeys.isNotEmpty()) append("• Depends on: ${prereqKeys.joinToString(", ")}")
          },
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.secondary
        )
      }

      AnimatedVisibility(visible = task.blockerReason.isNotBlank()) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = CrimsonBlocker.copy(alpha = 0.15f),
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
        ) {
          Text(
            text = "⚠ ${task.blockerReason}",
            style = MaterialTheme.typography.bodySmall,
            color = CrimsonBlocker,
            modifier = Modifier.padding(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      LinearProgressIndicator(
        progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
        color = statusColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(CircleShape)
      )

      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Assignee: ${task.assigneeName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
          if (task.assigneeEmail.isNotBlank()) {
            Text(
              text = task.assigneeEmail,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          TextButton(onClick = onRemindClick) {
            Icon(Icons.Default.Alarm, contentDescription = "Remind", modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Remind", style = MaterialTheme.typography.labelMedium)
          }
          if (task.status != TaskStatus.BLOCKED.name) {
            TextButton(onClick = onReportBlocker) {
              Icon(
                Icons.Default.WarningAmber,
                contentDescription = "Flag Blocker",
                tint = CrimsonBlocker,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Blocker", color = CrimsonBlocker, style = MaterialTheme.typography.labelMedium)
            }
          }
          if (task.status != TaskStatus.DONE.name) {
            TextButton(onClick = onQuickAdvance) {
              Icon(Icons.Default.PlayArrow, contentDescription = "Advance", modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Advance", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }
  }
}

fun statusColorFor(status: String): Color = when (status) {
  TaskStatus.DONE.name -> EmeraldPulse
  TaskStatus.IN_PROGRESS.name -> ElectricIndigo
  TaskStatus.IN_REVIEW.name -> CyanTelemetry
  TaskStatus.BLOCKED.name -> CrimsonBlocker
  TaskStatus.CASCADED_BLOCKED.name -> AmberProgress
  else -> Color(0xFF94A3B8)
}

@Composable
private fun AddTeamMemberDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String, String, String, Boolean, Long) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var role by remember { mutableStateOf("Junior Software Engineer") }
  var department by remember { mutableStateOf("Core Engineering") }
  var isSenior by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Junior / Team Member by Gmail") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = email,
          onValueChange = { email = it },
          label = { Text("Gmail Address (e.g. junior@gmail.com)") },
          leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = role,
          onValueChange = { role = it },
          label = { Text("Role Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = department,
          onValueChange = { department = it },
          label = { Text("Department / Squad") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(checked = isSenior, onCheckedChange = { isSenior = it })
          Text("Senior / Executive Lead Privileges")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            onConfirm(name, email, role, department, isSenior, 40L)
          }
        },
        enabled = name.isNotBlank()
      ) {
        Text("Add to Team Roster")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}

@Composable
private fun EscalateBlockerDialog(
  alert: BlockerAlert,
  members: List<TeamMember>,
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit
) {
  var newOwner by remember {
    mutableStateOf(
      members.firstOrNull { it.isSenior }?.displayName ?: alert.assigneeName
    )
  }
  var note by remember {
    mutableStateOf("Escalated to Senior Command priority queue for immediate unblock")
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Escalate Blocker [${alert.taskKey}]") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = newOwner,
          onValueChange = { newOwner = it },
          label = { Text("Escalate / Reassign To") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Escalation Directive") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(newOwner, note) },
        colors = ButtonDefaults.buttonColors(containerColor = CrimsonBlocker)
      ) {
        Text("Escalate Now", color = Color.White)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    }
  )
}
