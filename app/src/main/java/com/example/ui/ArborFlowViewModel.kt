package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BlockerAlert
import com.example.data.model.BlockerSeverity
import com.example.data.model.BlockerStatus
import com.example.data.model.IntegrationConfig
import com.example.data.model.SHARED_WORKSPACE_ID
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamDocument
import com.example.data.model.TeamMember
import com.example.data.model.TeamMessage
import com.example.data.model.TeamTask
import com.example.data.repository.ArborFlowRepository
import com.example.domain.IntegrationAndReminderManager
import com.example.domain.TreeDependencyEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>
  data class Success<T>(val data: T) : UiState<T>
  data class Error(val message: String) : UiState<Nothing>
}

enum class RoleViewMode(val label: String) {
  SENIOR_COMMAND("Senior Command"),
  MEMBER_FOCUS("Junior / Member View")
}

enum class TreeDisplayMode(val label: String) {
  NODE_GRAPH("2D Node Graph"),
  HIERARCHY_LIST("Hierarchy Tree")
}

class ArborFlowViewModel(
  private val repository: ArborFlowRepository,
  private val currentUserId: String,
  private val currentUserEmail: String = "",
  private val currentUserDisplayName: String = ""
) : ViewModel() {

  init {
    if (currentUserEmail.isNotBlank() || currentUserDisplayName.isNotBlank()) {
      viewModelScope.launch {
        repository.ensureGmailMemberRegistered(
          displayName = currentUserDisplayName,
          email = currentUserEmail
        )
      }
    }
  }

  val tasksState: StateFlow<UiState<List<TeamTask>>> = repository.observeTasks(currentUserId)
    .map<List<TeamTask>, UiState<List<TeamTask>>> { list ->
      UiState.Success(list.sortedBy { it.taskKey })
    }
    .catch { error ->
      Log.w(TAG, "Error observing tasks", error)
      emit(UiState.Error(error.message ?: "Failed to observe tasks"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val blockersState: StateFlow<UiState<List<BlockerAlert>>> = repository.observeBlockers(currentUserId)
    .map<List<BlockerAlert>, UiState<List<BlockerAlert>>> { list ->
      UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
    }
    .catch { error ->
      Log.w(TAG, "Error observing blockers", error)
      emit(UiState.Error(error.message ?: "Failed to observe blockers"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val membersState: StateFlow<UiState<List<TeamMember>>> = repository.observeMembers(currentUserId)
    .map<List<TeamMember>, UiState<List<TeamMember>>> { list ->
      UiState.Success(list.sortedBy { it.displayName })
    }
    .catch { error ->
      Log.w(TAG, "Error observing members", error)
      emit(UiState.Error(error.message ?: "Failed to observe members"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
      initialValue = UiState.Loading
    )

  val integrationsState: StateFlow<UiState<List<IntegrationConfig>>> =
    repository.observeIntegrations(currentUserId)
      .map<List<IntegrationConfig>, UiState<List<IntegrationConfig>>> { list ->
        UiState.Success(list)
      }
      .catch { error ->
        Log.w(TAG, "Error observing integrations", error)
        emit(UiState.Error(error.message ?: "Failed to observe integrations"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  val messagesState: StateFlow<UiState<List<TeamMessage>>> =
    repository.observeMessages(currentUserId)
      .map<List<TeamMessage>, UiState<List<TeamMessage>>> { list ->
        UiState.Success(list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE })
      }
      .catch { error ->
        Log.w(TAG, "Error observing messages", error)
        emit(UiState.Error(error.message ?: "Failed to observe messages"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  val documentsState: StateFlow<UiState<List<TeamDocument>>> =
    repository.observeDocuments(currentUserId)
      .map<List<TeamDocument>, UiState<List<TeamDocument>>> { list ->
        UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
      }
      .catch { error ->
        Log.w(TAG, "Error observing documents", error)
        emit(UiState.Error(error.message ?: "Failed to observe documents"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
      )

  private val _roleMode = MutableStateFlow(RoleViewMode.SENIOR_COMMAND)
  val roleMode: StateFlow<RoleViewMode> = _roleMode.asStateFlow()

  private val _treeDisplayMode = MutableStateFlow(TreeDisplayMode.NODE_GRAPH)
  val treeDisplayMode: StateFlow<TreeDisplayMode> = _treeDisplayMode.asStateFlow()

  private val _selectedAssigneeFilter = MutableStateFlow<String?>(null)
  val selectedAssigneeFilter: StateFlow<String?> = _selectedAssigneeFilter.asStateFlow()

  private val _collapsedNodeIds = MutableStateFlow<Set<String>>(emptySet())
  val collapsedNodeIds: StateFlow<Set<String>> = _collapsedNodeIds.asStateFlow()

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  fun setRoleMode(mode: RoleViewMode) {
    _roleMode.value = mode
  }

  fun setTreeDisplayMode(mode: TreeDisplayMode) {
    _treeDisplayMode.value = mode
  }

  fun setAssigneeFilter(assigneeName: String?) {
    _selectedAssigneeFilter.value = if (_selectedAssigneeFilter.value == assigneeName) {
      null
    } else {
      assigneeName
    }
  }

  fun toggleNodeCollapsed(taskId: String) {
    val current = _collapsedNodeIds.value
    _collapsedNodeIds.value = if (taskId in current) current - taskId else current + taskId
  }

  fun clearBannerMessage() {
    _bannerMessage.value = null
  }

  fun postMessage(msg: String) {
    _bannerMessage.value = msg
  }

  fun seedSampleSprint() {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        repository.seedSampleSprintWorkspace()
        _bannerMessage.value = "Shared Gmail team sprint, chat & documents initialized!"
      }.onFailure { e ->
        _bannerMessage.value = "Failed to initialize sample sprint: ${e.localizedMessage}"
      }
      _isBusy.value = false
    }
  }

  fun sendTeamMessage(
    text: String,
    linkedTaskKey: String,
    linkedDocId: String,
    linkedDocTitle: String
  ) {
    val cleanText = text.trim()
    if (cleanText.isBlank()) return
    viewModelScope.launch {
      val resolvedRole = if (_roleMode.value == RoleViewMode.SENIOR_COMMAND) {
        "Senior Lead"
      } else {
        "Junior / Team Member"
      }
      val msg = TeamMessage(
        userId = currentUserId,
        workspaceId = SHARED_WORKSPACE_ID,
        senderName = currentUserDisplayName.ifBlank { "Team Member" }.take(100),
        senderEmail = currentUserEmail.trim().lowercase().take(160),
        senderRole = resolvedRole,
        text = cleanText.take(1000),
        linkedTaskKey = linkedTaskKey.trim().take(32),
        linkedDocId = linkedDocId.trim().take(128),
        linkedDocTitle = linkedDocTitle.trim().take(200)
      )
      repository.createMessage(msg).onFailure {
        _bannerMessage.value = "Could not send message: ${it.localizedMessage}"
      }
    }
  }

  fun shareTeamDocument(
    title: String,
    category: String,
    externalUrl: String,
    contentSummary: String,
    linkedTaskKey: String,
    announceInChat: Boolean
  ) {
    val cleanTitle = title.trim()
    if (cleanTitle.isBlank()) return
    viewModelScope.launch {
      val doc = TeamDocument(
        userId = currentUserId,
        workspaceId = SHARED_WORKSPACE_ID,
        title = cleanTitle.take(200),
        category = category,
        externalUrl = externalUrl.trim().take(500),
        contentSummary = contentSummary.trim().take(2000),
        linkedTaskKey = linkedTaskKey.trim().take(32),
        uploadedByName = currentUserDisplayName.ifBlank { "Team Member" }.take(100),
        uploadedByEmail = currentUserEmail.trim().lowercase().take(160)
      )
      repository.createDocument(doc).onSuccess { docId ->
        _bannerMessage.value = "Shared document '${doc.title}' with team workspace!"
        if (announceInChat) {
          val resolvedRole = if (_roleMode.value == RoleViewMode.SENIOR_COMMAND) {
            "Senior Lead"
          } else {
            "Junior / Team Member"
          }
          val taskPrefix = if (doc.linkedTaskKey.isNotBlank()) " for [${doc.linkedTaskKey}]" else ""
          repository.createMessage(
            TeamMessage(
              userId = currentUserId,
              workspaceId = SHARED_WORKSPACE_ID,
              senderName = doc.uploadedByName,
              senderEmail = doc.uploadedByEmail,
              senderRole = resolvedRole,
              text = "Shared a new document$taskPrefix: ${doc.title}",
              linkedTaskKey = doc.linkedTaskKey,
              linkedDocId = docId,
              linkedDocTitle = doc.title
            )
          )
        }
      }.onFailure {
        _bannerMessage.value = "Could not share document: ${it.localizedMessage}"
      }
    }
  }

  fun deleteTeamDocument(doc: TeamDocument) {
    viewModelScope.launch {
      repository.deleteDocument(doc.id).onSuccess {
        _bannerMessage.value = "Removed '${doc.title}' from workspace"
      }
    }
  }

  fun saveTask(
    existingTask: TeamTask?,
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
  ) {
    viewModelScope.launch {
      val finalProgress = if (status == TaskStatus.DONE.name) 100L else progressPercent.coerceIn(0L, 100L)
      val cleanEmail = assigneeEmail.trim().lowercase().take(160)
      if (existingTask == null) {
        val newTask = TeamTask(
          userId = currentUserId,
          workspaceId = SHARED_WORKSPACE_ID,
          taskKey = taskKey.ifBlank { "ARB-${(100..999).random()}" }.take(32),
          title = title.trim().take(200),
          description = description.trim().take(1000),
          assigneeName = assigneeName.ifBlank { "Unassigned" }.take(100),
          assigneeEmail = cleanEmail,
          assigneeRole = assigneeRole.take(100),
          parentTaskId = parentTaskId,
          prerequisiteIds = prerequisiteIds.take(20),
          status = status,
          priority = priority,
          progressPercent = finalProgress,
          blockerReason = if (status == TaskStatus.BLOCKED.name) "Flagged during task creation" else "",
          dueDateMillis = dueDateMillis.coerceAtLeast(0L),
          reminderOffsetMinutes = reminderOffsetMinutes.coerceAtLeast(0L),
          externalRef = externalRef.take(120)
        )
        repository.createTask(newTask).onSuccess {
          val emailSuffix = if (cleanEmail.isNotBlank()) " → $cleanEmail" else ""
          _bannerMessage.value = "Assigned [${newTask.taskKey}] to ${newTask.assigneeName}$emailSuffix"
        }.onFailure {
          _bannerMessage.value = "Could not create task: ${it.localizedMessage}"
        }
      } else {
        val updated = existingTask.copy(
          taskKey = taskKey.ifBlank { existingTask.taskKey }.take(32),
          title = title.trim().take(200),
          description = description.trim().take(1000),
          assigneeName = assigneeName.ifBlank { "Unassigned" }.take(100),
          assigneeEmail = cleanEmail,
          assigneeRole = assigneeRole.take(100),
          parentTaskId = parentTaskId,
          prerequisiteIds = prerequisiteIds.take(20),
          status = status,
          priority = priority,
          progressPercent = finalProgress,
          blockerReason = if (status == TaskStatus.DONE.name || status == TaskStatus.IN_PROGRESS.name) "" else existingTask.blockerReason,
          dueDateMillis = dueDateMillis.coerceAtLeast(0L),
          reminderOffsetMinutes = reminderOffsetMinutes.coerceAtLeast(0L),
          externalRef = externalRef.take(120)
        )
        repository.updateTask(updated).onSuccess {
          _bannerMessage.value = "Updated [${updated.taskKey}] (${updated.assigneeName})"
        }.onFailure {
          _bannerMessage.value = "Update failed: ${it.localizedMessage}"
        }
      }
    }
  }

  fun quickAdvanceStatus(task: TeamTask, allTasks: List<TeamTask>) {
    viewModelScope.launch {
      val (nextStatus, nextProgress) = when (task.status) {
        TaskStatus.TODO.name -> TaskStatus.IN_PROGRESS.name to 35L
        TaskStatus.IN_PROGRESS.name -> TaskStatus.IN_REVIEW.name to 85L
        TaskStatus.IN_REVIEW.name -> TaskStatus.DONE.name to 100L
        TaskStatus.BLOCKED.name, TaskStatus.CASCADED_BLOCKED.name -> TaskStatus.IN_PROGRESS.name to 50L
        else -> TaskStatus.DONE.name to 100L
      }
      val updated = task.copy(
        status = nextStatus,
        progressPercent = nextProgress,
        blockerReason = if (nextStatus != TaskStatus.BLOCKED.name) "" else task.blockerReason
      )
      repository.updateTask(updated).onSuccess {
        if (task.status == TaskStatus.BLOCKED.name) {
          val impacted = TreeDependencyEngine.findCascadingImpactedTasks(task.id, allTasks)
          impacted.filter { it.status == TaskStatus.CASCADED_BLOCKED.name }.forEach { dep ->
            repository.updateTask(
              dep.copy(
                status = TaskStatus.IN_PROGRESS.name,
                blockerReason = ""
              )
            )
          }
        }
        _bannerMessage.value = "${task.taskKey} moved to ${TaskStatus.valueOf(nextStatus).label} ($nextProgress%)"
      }
    }
  }

  fun deleteTask(task: TeamTask) {
    viewModelScope.launch {
      repository.deleteTask(task.id).onSuccess {
        _bannerMessage.value = "Deleted ${task.taskKey}"
      }
    }
  }

  fun reportBlockerAndCascade(
    targetTask: TeamTask,
    reason: String,
    severity: String,
    reportedByName: String,
    allTasks: List<TeamTask>,
    onAlertCreated: (BlockerAlert) -> Unit = {}
  ) {
    viewModelScope.launch {
      val cleanReason = reason.trim().ifBlank { "Critical dependency bottleneck encountered" }.take(500)
      val impactedTasks = TreeDependencyEngine.findCascadingImpactedTasks(targetTask.id, allTasks)
      val impactedKeys = impactedTasks.map { it.taskKey }.take(20)

      if (targetTask.createdAt != null) {
        repository.updateTask(
          targetTask.copy(
            status = TaskStatus.BLOCKED.name,
            blockerReason = cleanReason,
            priority = if (severity == BlockerSeverity.CRITICAL.name) TaskPriority.CRITICAL.name else targetTask.priority
          )
        )
      }

      impactedTasks.forEach { downstream ->
        if (downstream.createdAt != null) {
          repository.updateTask(
            downstream.copy(
              status = TaskStatus.CASCADED_BLOCKED.name,
              blockerReason = "Cascaded lock from ${targetTask.taskKey}: $cleanReason".take(500)
            )
          )
        }
      }

      val alert = BlockerAlert(
        userId = currentUserId,
        workspaceId = SHARED_WORKSPACE_ID,
        taskId = targetTask.id,
        taskKey = targetTask.taskKey,
        taskTitle = targetTask.title,
        reportedByName = reportedByName.ifBlank { targetTask.assigneeName }.take(100),
        assigneeName = targetTask.assigneeName.ifBlank { "Senior Lead" }.take(100),
        reason = cleanReason,
        severity = severity,
        impactedTaskKeys = impactedKeys,
        status = BlockerStatus.ACTIVE.name,
        resolutionNote = ""
      )
      repository.createBlockerAlert(alert).onSuccess { id ->
        val savedAlert = alert.copy(id = id)
        onAlertCreated(savedAlert)
        _bannerMessage.value = if (impactedKeys.isEmpty()) {
          "Blocker logged on ${targetTask.taskKey}"
        } else {
          "Blocker logged on ${targetTask.taskKey} • Auto-cascaded lock to ${impactedKeys.joinToString(", ")}"
        }
        val integrations = (integrationsState.value as? UiState.Success)?.data.orEmpty()
        integrations.filter { it.autoDispatchBlockers && it.webhookUrl.isNotBlank() }.forEach { config ->
          val payload = JSONObject().apply {
            put("taskKey", targetTask.taskKey)
            put("taskTitle", targetTask.title)
            put("reason", cleanReason)
            put("severity", severity)
            put("impactedTaskKeys", JSONArray(impactedKeys))
          }
          IntegrationAndReminderManager.dispatchWebhookEvent(
            webhookUrl = config.webhookUrl,
            eventType = "BLOCKER_CASCADED",
            payloadJson = payload
          )
        }
      }
    }
  }

  fun escalateBlocker(alert: BlockerAlert, newAssigneeName: String, note: String) {
    viewModelScope.launch {
      if (alert.createdAt == null) return@launch
      val updated = alert.copy(
        status = BlockerStatus.ESCALATED.name,
        severity = BlockerSeverity.CRITICAL.name,
        assigneeName = newAssigneeName.ifBlank { alert.assigneeName }.take(100),
        resolutionNote = note.trim().ifBlank { "Escalated to Senior Command priority queue" }.take(500)
      )
      repository.updateBlockerAlert(updated).onSuccess {
        _bannerMessage.value = "Escalated [${alert.taskKey}] to ${updated.assigneeName}"
      }
    }
  }

  fun resolveBlockerAndUnblockTree(alert: BlockerAlert, resolutionNote: String, allTasks: List<TeamTask>) {
    viewModelScope.launch {
      if (alert.createdAt != null) {
        repository.updateBlockerAlert(
          alert.copy(
            status = BlockerStatus.RESOLVED.name,
            resolutionNote = resolutionNote.trim().ifBlank { "Blocker resolved and downstream subtree unlocked" }.take(500)
          )
        )
      }
      val rootTask = allTasks.find { it.id == alert.taskId || it.taskKey == alert.taskKey }
      if (rootTask != null && rootTask.createdAt != null) {
        repository.updateTask(
          rootTask.copy(
            status = TaskStatus.IN_PROGRESS.name,
            blockerReason = ""
          )
        )
        val impacted = TreeDependencyEngine.findCascadingImpactedTasks(rootTask.id, allTasks)
        impacted.filter { it.status == TaskStatus.CASCADED_BLOCKED.name && it.createdAt != null }.forEach { dep ->
          repository.updateTask(
            dep.copy(
              status = TaskStatus.IN_PROGRESS.name,
              blockerReason = ""
            )
          )
        }
      }
      _bannerMessage.value = "Resolved blocker on ${alert.taskKey} & unlocked downstream tasks!"
    }
  }

  fun addTeamMember(
    displayName: String,
    email: String,
    roleTitle: String,
    department: String,
    isSenior: Boolean,
    capacityHours: Long
  ) {
    viewModelScope.launch {
      val palette = listOf("#6366F1", "#06B6D4", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6")
      val cleanEmail = email.trim().lowercase().take(160)
      val deterministicId = if (cleanEmail.isNotBlank()) {
        "mem_gmail_${cleanEmail.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)}"
      } else {
        ""
      }
      val member = TeamMember(
        id = deterministicId,
        userId = currentUserId,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = displayName.trim().take(100),
        email = cleanEmail,
        roleTitle = roleTitle.trim().ifBlank { "Junior Engineer" }.take(100),
        department = department.trim().ifBlank { "Engineering" }.take(100),
        isSenior = isSenior,
        avatarColorHex = palette.random(),
        capacityHoursPerWeek = capacityHours.coerceIn(1L, 80L)
      )
      repository.createMember(member).onSuccess {
        val emailInfo = if (cleanEmail.isNotBlank()) " ($cleanEmail)" else ""
        _bannerMessage.value = "Registered ${member.displayName}$emailInfo in team roster"
      }
    }
  }

  fun importExternalTasks(rawPayload: String, provider: String, projectKey: String, existingTasks: List<TeamTask>) {
    viewModelScope.launch {
      val parsed = IntegrationAndReminderManager.parseImportedTasks(
        rawPayload = rawPayload,
        projectPrefix = projectKey.ifBlank { "ARB" },
        existingCount = existingTasks.size
      )
      if (parsed.isEmpty()) {
        _bannerMessage.value = "No valid tasks found in import payload"
        return@launch
      }
      val rootCandidateId = existingTasks.firstOrNull { it.parentTaskId.isBlank() }?.id ?: ""
      parsed.forEach { task ->
        repository.createTask(
          task.copy(
            userId = currentUserId,
            workspaceId = SHARED_WORKSPACE_ID,
            parentTaskId = rootCandidateId
          )
        )
      }
      _bannerMessage.value = "Imported ${parsed.size} tasks from $provider into dependency tree!"
    }
  }

  fun saveAndTestIntegration(
    existingConfig: IntegrationConfig?,
    provider: String,
    name: String,
    webhookUrl: String,
    projectKey: String,
    autoDispatchBlockers: Boolean,
    autoSyncCalendar: Boolean,
    testDispatch: Boolean
  ) {
    viewModelScope.launch {
      var summary = "Configured • Ready for sync"
      if (testDispatch && webhookUrl.isNotBlank()) {
        val testPayload = JSONObject().apply {
          put("projectKey", projectKey)
          put("provider", provider)
          put("status", "VERIFIED_ONLINE")
        }
        val res = IntegrationAndReminderManager.dispatchWebhookEvent(
          webhookUrl = webhookUrl.trim(),
          eventType = "CONNECTOR_PING",
          payloadJson = testPayload
        )
        summary = res.fold(
          onSuccess = { "Live Webhook Verified: $it" },
          onFailure = { "Ping Error: ${it.localizedMessage?.take(80)}" }
        )
      }
      val config = (existingConfig ?: IntegrationConfig()).copy(
        userId = currentUserId,
        workspaceId = SHARED_WORKSPACE_ID,
        provider = provider,
        name = name.trim().ifBlank { "$provider Connector" }.take(100),
        webhookUrl = webhookUrl.trim().take(500),
        projectKey = projectKey.trim().ifBlank { "ARB" }.take(50),
        autoDispatchBlockers = autoDispatchBlockers,
        autoSyncCalendar = autoSyncCalendar,
        lastSyncSummary = summary.take(300)
      )
      repository.saveIntegration(config).onSuccess {
        _bannerMessage.value = summary
      }
    }
  }

  private companion object {
    const val TAG = "ArborFlowVM"
    const val STOP_TIMEOUT_MILLIS = 5000L
  }
}
