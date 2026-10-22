package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.BlockerStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.data.repository.ArborFlowRepository
import com.example.domain.IntegrationAndReminderManager
import com.example.ui.ArborFlowViewModel
import com.example.ui.AuthScreen
import com.example.ui.UiState
import com.example.ui.screens.BlockersAndAlertsScreen
import com.example.ui.screens.CalendarAndIntegrationsScreen
import com.example.ui.screens.ReportBlockerDialog
import com.example.ui.screens.SeniorDashboardScreen
import com.example.ui.screens.TaskEditorDialog
import com.example.ui.screens.TaskTreeScreen
import com.example.ui.screens.TeamChatAndDocsScreen
import com.example.ui.signOutUser
import com.example.ui.theme.ArborFlowTheme
import com.example.ui.theme.CrimsonBlocker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    IntegrationAndReminderManager.ensureNotificationChannels(this)
    setContent {
      ArborFlowTheme {
        AppNavigation()
      }
    }
  }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
  val listener = FirebaseAuth.AuthStateListener { auth ->
    trySend(auth.currentUser)
  }
  addAuthStateListener(listener)
  awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
  val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  if (user == null) {
    AuthScreen(onAuthSuccess = {})
  } else {
    ArborFlowWorkspaceApp(
      currentUserId = user.uid,
      userDisplayName = user.displayName ?: "Team Member",
      userEmail = user.email ?: ""
    )
  }
}

enum class MainNavDestination(val label: String, val icon: ImageVector) {
  DASHBOARD("Command", Icons.Default.Dashboard),
  TREE("Task Tree", Icons.Default.AccountTree),
  CHAT_DOCS("Chat & Docs", Icons.Default.Chat),
  BLOCKERS("Blockers", Icons.Default.WarningAmber),
  INTEGRATIONS("Sync & Cal", Icons.Default.CalendarMonth)
}

@Composable
fun ArborFlowWorkspaceApp(
  currentUserId: String,
  userDisplayName: String,
  userEmail: String = ""
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  val viewModel: ArborFlowViewModel = viewModel(
    key = "$currentUserId:$userEmail",
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        ArborFlowViewModel(
          repository = ArborFlowRepository(db),
          currentUserId = currentUserId,
          currentUserEmail = userEmail,
          currentUserDisplayName = userDisplayName
        )
      }
    }
  )

  val tasksState by viewModel.tasksState.collectAsStateWithLifecycle()
  val blockersState by viewModel.blockersState.collectAsStateWithLifecycle()
  val membersState by viewModel.membersState.collectAsStateWithLifecycle()
  val integrationsState by viewModel.integrationsState.collectAsStateWithLifecycle()
  val messagesState by viewModel.messagesState.collectAsStateWithLifecycle()
  val documentsState by viewModel.documentsState.collectAsStateWithLifecycle()
  val roleMode by viewModel.roleMode.collectAsStateWithLifecycle()
  val treeDisplayMode by viewModel.treeDisplayMode.collectAsStateWithLifecycle()
  val selectedAssigneeFilter by viewModel.selectedAssigneeFilter.collectAsStateWithLifecycle()
  val collapsedNodeIds by viewModel.collapsedNodeIds.collectAsStateWithLifecycle()
  val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var currentDestination by remember { mutableStateOf(MainNavDestination.DASHBOARD) }
  var editingTask by remember { mutableStateOf<TeamTask?>(null) }
  var preselectedAssigneeMember by remember { mutableStateOf<TeamMember?>(null) }
  var showCreateTaskDialog by remember { mutableStateOf(false) }
  var reportingBlockerForTask by remember { mutableStateOf<TeamTask?>(null) }

  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(bannerMessage) {
    val msg = bannerMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearBannerMessage()
    }
  }

  // Handle back navigation on secondary tabs
  BackHandler(enabled = currentDestination != MainNavDestination.DASHBOARD) {
    currentDestination = MainNavDestination.DASHBOARD
  }

  val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
  val blockers = (blockersState as? UiState.Success)?.data.orEmpty()
  val members = (membersState as? UiState.Success)?.data.orEmpty()
  val integrations = (integrationsState as? UiState.Success)?.data.orEmpty()
  val messages = (messagesState as? UiState.Success)?.data.orEmpty()
  val documents = (documentsState as? UiState.Success)?.data.orEmpty()
  val activeBlockerCount = remember(blockers) {
    blockers.count { it.status != BlockerStatus.RESOLVED.name }
  }

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val isExpandedScreen = maxWidth >= 720.dp

    Scaffold(
      contentWindowInsets = WindowInsets.safeDrawing,
      snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
      topBar = {
        Surface(
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline)
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsPadding(WindowInsets(top = 8.dp))
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "ArborFlow Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = "ArborFlow",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = if (userEmail.isNotBlank()) "$userDisplayName ($userEmail)" else "$userDisplayName • Live Cloud Sync",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.secondary
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (activeBlockerCount > 0) {
                Surface(
                  shape = CircleShape,
                  color = CrimsonBlocker.copy(alpha = 0.2f),
                  modifier = Modifier
                    .padding(end = 8.dp)
                    .border(1.dp, CrimsonBlocker, CircleShape)
                ) {
                  Text(
                    text = "$activeBlockerCount BLOCKER${if (activeBlockerCount > 1) "S" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = CrimsonBlocker,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                  )
                }
              }
              IconButton(
                onClick = {
                  signOutUser(
                    credentialManager = credentialManager,
                    onSignOutComplete = {},
                    scope = scope
                  )
                },
                modifier = Modifier.testTag("sign_out_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.Logout,
                  contentDescription = "Sign Out",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      },
      bottomBar = {
        if (!isExpandedScreen) {
          NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface
          ) {
            MainNavDestination.entries.forEach { dest ->
              NavigationBarItem(
                selected = currentDestination == dest,
                onClick = { currentDestination = dest },
                icon = {
                  if (dest == MainNavDestination.BLOCKERS && activeBlockerCount > 0) {
                    BadgedBox(
                      badge = {
                        Badge(containerColor = CrimsonBlocker) {
                          Text("$activeBlockerCount")
                        }
                      }
                    ) {
                      Icon(dest.icon, contentDescription = dest.label)
                    }
                  } else {
                    Icon(dest.icon, contentDescription = dest.label)
                  }
                },
                label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
              )
            }
          }
        }
      },
      floatingActionButton = {
        if (currentDestination == MainNavDestination.DASHBOARD || currentDestination == MainNavDestination.TREE) {
          FloatingActionButton(
            onClick = {
              preselectedAssigneeMember = null
              showCreateTaskDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("fab_create_task")
          ) {
            Icon(Icons.Default.Add, contentDescription = "Create Task Node")
          }
        }
      }
    ) { innerPadding ->
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .background(MaterialTheme.colorScheme.background)
      ) {
        if (isExpandedScreen) {
          NavigationRail(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxHeight()
          ) {
            MainNavDestination.entries.forEach { dest ->
              NavigationRailItem(
                selected = currentDestination == dest,
                onClick = { currentDestination = dest },
                icon = { Icon(dest.icon, contentDescription = dest.label) },
                label = { Text(dest.label) },
                modifier = Modifier.testTag("rail_${dest.name.lowercase()}")
              )
            }
          }
        }

        Box(modifier = Modifier.fillMaxSize()) {
          if (tasksState is UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          } else {
            when (currentDestination) {
              MainNavDestination.DASHBOARD -> {
                SeniorDashboardScreen(
                  tasks = tasks,
                  blockers = blockers,
                  members = members,
                  currentUserEmail = userEmail,
                  currentUserDisplayName = userDisplayName,
                  roleMode = roleMode,
                  selectedAssigneeFilter = selectedAssigneeFilter,
                  isBusy = isBusy,
                  onRoleModeChange = viewModel::setRoleMode,
                  onSelectAssigneeFilter = viewModel::setAssigneeFilter,
                  onSeedSampleSprint = viewModel::seedSampleSprint,
                  onOpenCreateTask = {
                    preselectedAssigneeMember = null
                    showCreateTaskDialog = true
                  },
                  onAssignTaskToMember = { member ->
                    preselectedAssigneeMember = member
                    showCreateTaskDialog = true
                  },
                  onOpenTaskDetail = { editingTask = it },
                  onQuickAdvanceTask = { viewModel.quickAdvanceStatus(it, tasks) },
                  onOpenReportBlocker = { reportingBlockerForTask = it },
                  onEscalateBlocker = viewModel::escalateBlocker,
                  onResolveBlocker = { alert, note ->
                    viewModel.resolveBlockerAndUnblockTree(alert, note, tasks)
                  },
                  onAddTeamMember = viewModel::addTeamMember,
                  onNavigateToTree = { currentDestination = MainNavDestination.TREE },
                  onPostMessage = viewModel::postMessage
                )
              }

              MainNavDestination.TREE -> {
                TaskTreeScreen(
                  tasks = tasks,
                  displayMode = treeDisplayMode,
                  collapsedNodeIds = collapsedNodeIds,
                  onDisplayModeChange = viewModel::setTreeDisplayMode,
                  onToggleCollapse = viewModel::toggleNodeCollapsed,
                  onTaskClick = { editingTask = it },
                  onQuickAdvance = { viewModel.quickAdvanceStatus(it, tasks) },
                  onReportBlocker = { reportingBlockerForTask = it },
                  onCreateTaskClick = {
                    preselectedAssigneeMember = null
                    showCreateTaskDialog = true
                  }
                )
              }

              MainNavDestination.CHAT_DOCS -> {
                TeamChatAndDocsScreen(
                  messages = messages,
                  documents = documents,
                  tasks = tasks,
                  currentUserEmail = userEmail,
                  currentUserDisplayName = userDisplayName,
                  onSendMessage = viewModel::sendTeamMessage,
                  onShareDocument = viewModel::shareTeamDocument,
                  onDeleteDocument = viewModel::deleteTeamDocument,
                  onPostMessage = viewModel::postMessage
                )
              }

              MainNavDestination.BLOCKERS -> {
                BlockersAndAlertsScreen(
                  tasks = tasks,
                  blockers = blockers,
                  members = members,
                  onOpenReportBlocker = { reportingBlockerForTask = it },
                  onEscalateBlocker = viewModel::escalateBlocker,
                  onResolveBlocker = { alert, note ->
                    viewModel.resolveBlockerAndUnblockTree(alert, note, tasks)
                  },
                  onPostMessage = viewModel::postMessage
                )
              }

              MainNavDestination.INTEGRATIONS -> {
                CalendarAndIntegrationsScreen(
                  tasks = tasks,
                  blockers = blockers,
                  members = members,
                  integrations = integrations,
                  onImportExternalTasks = { raw, provider, key ->
                    viewModel.importExternalTasks(raw, provider, key, tasks)
                  },
                  onSaveAndTestIntegration = viewModel::saveAndTestIntegration,
                  onPostMessage = viewModel::postMessage
                )
              }
            }
          }
        }
      }
    }
  }

  if (showCreateTaskDialog || editingTask != null) {
    TaskEditorDialog(
      existingTask = editingTask,
      preselectedMember = preselectedAssigneeMember,
      allTasks = tasks,
      members = members,
      onDismiss = {
        showCreateTaskDialog = false
        editingTask = null
        preselectedAssigneeMember = null
      },
      onSave = { key, title, desc, assignee, assigneeEmail, role, parentId, prereqs, status, priority, progress, dueMillis, remOffset, extRef ->
        viewModel.saveTask(
          existingTask = editingTask,
          taskKey = key,
          title = title,
          description = desc,
          assigneeName = assignee,
          assigneeEmail = assigneeEmail,
          assigneeRole = role,
          parentTaskId = parentId,
          prerequisiteIds = prereqs,
          status = status,
          priority = priority,
          progressPercent = progress,
          dueDateMillis = dueMillis,
          reminderOffsetMinutes = remOffset,
          externalRef = extRef
        )
      },
      onDelete = { task -> viewModel.deleteTask(task) },
      onOpenReportBlocker = { task -> reportingBlockerForTask = task },
      onPostMessage = viewModel::postMessage
    )
  }

  if (reportingBlockerForTask != null) {
    val target = reportingBlockerForTask!!
    ReportBlockerDialog(
      targetTask = target,
      allTasks = tasks,
      onDismiss = { reportingBlockerForTask = null },
      onSubmitBlocker = { reason, severity, reporter ->
        viewModel.reportBlockerAndCascade(
          targetTask = target,
          reason = reason,
          severity = severity,
          reportedByName = reporter,
          allTasks = tasks,
          onAlertCreated = { alert ->
            IntegrationAndReminderManager.sendBlockerNotification(context, alert)
          }
        )
      }
    )
  }
}
