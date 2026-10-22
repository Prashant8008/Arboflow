package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BlockerAlert
import com.example.data.model.BlockerSeverity
import com.example.data.model.BlockerStatus
import com.example.data.model.DocumentCategory
import com.example.data.model.IntegrationConfig
import com.example.data.model.IntegrationProvider
import com.example.data.model.SHARED_WORKSPACE_ID
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamDocument
import com.example.data.model.TeamMember
import com.example.data.model.TeamMessage
import com.example.data.model.TeamTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ArborFlowRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth: FirebaseAuth
    get() = FirebaseAuth.getInstance()

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeTasks(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<TeamTask>> = callbackFlow {
    val path = "tasks"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  fun observeBlockers(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<BlockerAlert>> = callbackFlow {
    val path = "blockers"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(BlockerAlert::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  fun observeMembers(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<TeamMember>> = callbackFlow {
    val path = "members"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  fun observeIntegrations(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<IntegrationConfig>> = callbackFlow {
    val path = "integrations"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(
              IntegrationConfig::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  fun observeMessages(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<TeamMessage>> = callbackFlow {
    val path = "messages"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  fun observeDocuments(
    userId: String = auth.currentUser?.uid ?: "",
    workspaceId: String = SHARED_WORKSPACE_ID
  ): Flow<List<TeamDocument>> = callbackFlow {
    val path = "documents"
    val registration = db.collection(path)
      .whereEqualTo("workspaceId", workspaceId)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          handleFirestoreError(error, OperationType.LIST, path)
          close(error)
          return@addSnapshotListener
        }
        if (snapshot != null) {
          val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(TeamDocument::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          }
          trySend(items)
        }
      }
    awaitClose { registration.remove() }
  }

  suspend fun createTask(task: TeamTask): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (task.id.isNotBlank()) {
      db.collection("tasks").document(task.id)
    } else {
      db.collection("tasks").document()
    }
    try {
      val payload = task.copy(
        id = docRef.id,
        userId = uid,
        workspaceId = task.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
        createdAt = null,
        updatedAt = null
      )
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateTask(task: TeamTask): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
    requireNotNull(task.createdAt) { "Task createdAt must not be null for update" }
    val docRef = db.collection("tasks").document(task.id)
    try {
      val ownerUid = task.userId.ifBlank { uid }
      docRef.set(
        task.copy(
          userId = ownerUid,
          workspaceId = task.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
          updatedAt = null
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("tasks").document(taskId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun getTaskById(taskId: String): Result<TeamTask?> = runCatching {
    val docRef = db.collection("tasks").document(taskId)
    try {
      val snapshot = docRef.get().await()
      snapshot.toObject(TeamTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun getUserTasks(): Result<List<TeamTask>> = runCatching {
    val uid = requireUserId()
    val path = "tasks"
    try {
      val snapshot = db.collection(path).whereEqualTo("userId", uid).get().await()
      snapshot.documents.mapNotNull {
        it.toObject(TeamTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, path)
      throw e
    }
  }

  suspend fun createBlockerAlert(alert: BlockerAlert): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (alert.id.isNotBlank()) {
      db.collection("blockers").document(alert.id)
    } else {
      db.collection("blockers").document()
    }
    try {
      val payload = alert.copy(
        id = docRef.id,
        userId = uid,
        workspaceId = alert.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
        createdAt = null,
        updatedAt = null
      )
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateBlockerAlert(alert: BlockerAlert): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(alert.id.isNotBlank()) { "Blocker ID must not be blank for update" }
    requireNotNull(alert.createdAt) { "Blocker createdAt must not be null for update" }
    val docRef = db.collection("blockers").document(alert.id)
    try {
      val ownerUid = alert.userId.ifBlank { uid }
      docRef.set(
        alert.copy(
          userId = ownerUid,
          workspaceId = alert.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
          updatedAt = null
        )
      ).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun createMember(member: TeamMember): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (member.id.isNotBlank()) {
      db.collection("members").document(member.id)
    } else {
      db.collection("members").document()
    }
    try {
      val payload = member.copy(
        id = docRef.id,
        userId = uid,
        workspaceId = member.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
        email = member.email.trim().lowercase().take(160),
        createdAt = null,
        updatedAt = null
      )
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun createMessage(message: TeamMessage): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (message.id.isNotBlank()) {
      db.collection("messages").document(message.id)
    } else {
      db.collection("messages").document()
    }
    try {
      val payload = message.copy(
        id = docRef.id,
        userId = uid,
        workspaceId = message.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
        senderName = message.senderName.trim().ifBlank { "Team Member" }.take(100),
        senderEmail = message.senderEmail.trim().lowercase().take(160),
        senderRole = message.senderRole.trim().take(100),
        text = message.text.trim().take(1000),
        linkedTaskKey = message.linkedTaskKey.trim().take(32),
        linkedDocId = message.linkedDocId.trim().take(128),
        linkedDocTitle = message.linkedDocTitle.trim().take(200),
        createdAt = null,
        updatedAt = null
      )
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun createDocument(document: TeamDocument): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (document.id.isNotBlank()) {
      db.collection("documents").document(document.id)
    } else {
      db.collection("documents").document()
    }
    try {
      val payload = document.copy(
        id = docRef.id,
        userId = uid,
        workspaceId = document.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
        title = document.title.trim().take(200),
        category = document.category.trim().ifBlank { DocumentCategory.SPEC.name },
        externalUrl = document.externalUrl.trim().take(500),
        contentSummary = document.contentSummary.trim().take(2000),
        linkedTaskKey = document.linkedTaskKey.trim().take(32),
        uploadedByName = document.uploadedByName.trim().ifBlank { "Team Member" }.take(100),
        uploadedByEmail = document.uploadedByEmail.trim().lowercase().take(160),
        createdAt = null,
        updatedAt = null
      )
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteDocument(documentId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("documents").document(documentId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  /**
   * Automatically registers the signed-in Gmail user in the shared team workspace roster
   * so Seniors and Juniors see everyone who joined with their Gmail account.
   */
  suspend fun ensureGmailMemberRegistered(
    displayName: String,
    email: String
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val cleanEmail = email.trim().lowercase()
    val safeSuffix = if (cleanEmail.isNotBlank()) {
      cleanEmail.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(40)
    } else {
      uid.take(24)
    }
    val docId = "mem_gmail_$safeSuffix"
    val docRef = db.collection("members").document(docId)
    val existingSnap = docRef.get().await()
    if (!existingSnap.exists()) {
      val resolvedName = displayName.trim().ifBlank {
        cleanEmail.substringBefore("@").replace(".", " ").replaceFirstChar { it.uppercase() }
          .ifBlank { "Team Member" }
      }
      val palette = listOf("#6366F1", "#06B6D4", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6")
      val colorHex = palette[Math.abs(docId.hashCode()) % palette.size]
      val newMember = TeamMember(
        id = docId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = resolvedName.take(100),
        email = cleanEmail.take(160),
        roleTitle = "Team Member (Gmail Synced)",
        department = "Engineering Squad",
        isSenior = false,
        avatarColorHex = colorHex,
        capacityHoursPerWeek = 40L,
        createdAt = null,
        updatedAt = null
      )
      docRef.set(newMember).await()
    }
    docId
  }

  suspend fun saveIntegration(config: IntegrationConfig): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = if (config.id.isNotBlank()) {
      db.collection("integrations").document(config.id)
    } else {
      db.collection("integrations").document()
    }
    try {
      if (config.id.isNotBlank() && config.createdAt != null) {
        val ownerUid = config.userId.ifBlank { uid }
        docRef.set(
          config.copy(
            userId = ownerUid,
            workspaceId = config.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
            updatedAt = null
          )
        ).await()
      } else {
        docRef.set(
          config.copy(
            id = docRef.id,
            userId = uid,
            workspaceId = config.workspaceId.ifBlank { SHARED_WORKSPACE_ID },
            createdAt = null,
            updatedAt = null
          )
        ).await()
      }
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun seedSampleSprintWorkspace() {
    val uid = requireUserId()
    val currentUserEmail = auth.currentUser?.email?.trim()?.lowercase().orEmpty()
    val currentUserName = auth.currentUser?.displayName?.trim().orEmpty()
    val now = System.currentTimeMillis()
    val dayMs = 86_400_000L

    val members = listOf(
      TeamMember(
        id = "mem_${uid.take(6)}_lead",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = currentUserName.ifBlank { "Elena Vance" },
        email = currentUserEmail.ifBlank { "elena.senior@gmail.com" },
        roleTitle = "Senior Engineering Lead",
        department = "Executive Leadership",
        isSenior = true,
        avatarColorHex = "#6366F1",
        capacityHoursPerWeek = 40L
      ),
      TeamMember(
        id = "mem_${uid.take(6)}_arch",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = "Marcus Chen",
        email = "marcus.chen@gmail.com",
        roleTitle = "Staff Systems Architect",
        department = "Platform Core",
        isSenior = true,
        avatarColorHex = "#06B6D4",
        capacityHoursPerWeek = 40L
      ),
      TeamMember(
        id = "mem_${uid.take(6)}_back",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = "Priya Sharma",
        email = "priya.junior@gmail.com",
        roleTitle = "Junior Backend Engineer",
        department = "Cloud API",
        isSenior = false,
        avatarColorHex = "#10B981",
        capacityHoursPerWeek = 40L
      ),
      TeamMember(
        id = "mem_${uid.take(6)}_mob",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        displayName = "Liam O'Connor",
        email = "liam.junior@gmail.com",
        roleTitle = "Junior Android Engineer",
        department = "Mobile Experience",
        isSenior = false,
        avatarColorHex = "#F59E0B",
        capacityHoursPerWeek = 40L
      )
    )
    members.forEach { createMember(it) }

    val rootId = "task_${uid.take(6)}_100"
    val authId = "task_${uid.take(6)}_101"
    val schemaId = "task_${uid.take(6)}_102"
    val syncId = "task_${uid.take(6)}_103"
    val treeUiId = "task_${uid.take(6)}_104"
    val calSyncId = "task_${uid.take(6)}_105"
    val qaId = "task_${uid.take(6)}_106"

    val sampleTasks = listOf(
      TeamTask(
        id = rootId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-100",
        title = "Q4 Unified Cloud Sync & Telemetry Launch",
        description = "Executive program milestone encompassing backend sync engine, dependency graph UI, and calendar connectors.",
        assigneeId = members[0].id,
        assigneeName = members[0].displayName,
        assigneeEmail = members[0].email,
        assigneeRole = members[0].roleTitle,
        parentTaskId = "",
        prerequisiteIds = emptyList(),
        status = TaskStatus.IN_PROGRESS.name,
        priority = TaskPriority.CRITICAL.name,
        progressPercent = 48L,
        blockerReason = "",
        dueDateMillis = now + 7 * dayMs,
        reminderOffsetMinutes = 120L,
        externalRef = "JIRA-100"
      ),
      TeamTask(
        id = authId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-101",
        title = "Zero-Trust Identity & OAuth Token Pipeline",
        description = "Provision Google Credential Manager flow and multi-tenant Firestore security rules.",
        assigneeId = members[1].id,
        assigneeName = members[1].displayName,
        assigneeEmail = members[1].email,
        assigneeRole = members[1].roleTitle,
        parentTaskId = rootId,
        prerequisiteIds = emptyList(),
        status = TaskStatus.DONE.name,
        priority = TaskPriority.HIGH.name,
        progressPercent = 100L,
        blockerReason = "",
        dueDateMillis = now + 1 * dayMs,
        reminderOffsetMinutes = 60L,
        externalRef = "JIRA-101"
      ),
      TeamTask(
        id = schemaId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-102",
        title = "Distributed DAG Event Stream & Webhook Broker",
        description = "Stream real-time task state transitions to external REST webhooks and calculate topological roll-ups.",
        assigneeId = members[2].id,
        assigneeName = members[2].displayName,
        assigneeEmail = members[2].email,
        assigneeRole = members[2].roleTitle,
        parentTaskId = rootId,
        prerequisiteIds = listOf(authId),
        status = TaskStatus.BLOCKED.name,
        priority = TaskPriority.CRITICAL.name,
        progressPercent = 60L,
        blockerReason = "External VPC firewall rule for webhook egress pending SecOps approval",
        dueDateMillis = now + 2 * dayMs,
        reminderOffsetMinutes = 30L,
        externalRef = "LIN-204"
      ),
      TeamTask(
        id = syncId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-103",
        title = "Bi-Directional Jira & Linear Issue Synchronizer",
        description = "Map external sprint issues into ArborFlow tree nodes with automatic status reconciliation.",
        assigneeId = members[2].id,
        assigneeName = members[2].displayName,
        assigneeEmail = members[2].email,
        assigneeRole = members[2].roleTitle,
        parentTaskId = schemaId,
        prerequisiteIds = listOf(schemaId),
        status = TaskStatus.CASCADED_BLOCKED.name,
        priority = TaskPriority.HIGH.name,
        progressPercent = 25L,
        blockerReason = "Cascaded lock from ARB-102: External VPC firewall rule for webhook egress pending SecOps approval",
        dueDateMillis = now + 4 * dayMs,
        reminderOffsetMinutes = 60L,
        externalRef = "LIN-205"
      ),
      TeamTask(
        id = treeUiId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-104",
        title = "Interactive 2D Bezier Dependency Canvas",
        description = "Render zoomable/pannable node graph with live progress rings and critical-path glow.",
        assigneeId = members[3].id,
        assigneeName = members[3].displayName,
        assigneeEmail = members[3].email,
        assigneeRole = members[3].roleTitle,
        parentTaskId = rootId,
        prerequisiteIds = listOf(authId),
        status = TaskStatus.IN_PROGRESS.name,
        priority = TaskPriority.HIGH.name,
        progressPercent = 75L,
        blockerReason = "",
        dueDateMillis = now + 3 * dayMs,
        reminderOffsetMinutes = 60L,
        externalRef = "TRL-88"
      ),
      TeamTask(
        id = calSyncId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-105",
        title = "Android Calendar & ICS Schedule Dispatcher",
        description = "Integrate CalendarContract deadline insertion, ICS export, and local reminder notifications.",
        assigneeId = members[3].id,
        assigneeName = members[3].displayName,
        assigneeEmail = members[3].email,
        assigneeRole = members[3].roleTitle,
        parentTaskId = treeUiId,
        prerequisiteIds = listOf(treeUiId),
        status = TaskStatus.IN_REVIEW.name,
        priority = TaskPriority.MEDIUM.name,
        progressPercent = 90L,
        blockerReason = "",
        dueDateMillis = now + 5 * dayMs,
        reminderOffsetMinutes = 45L,
        externalRef = "TRL-89"
      ),
      TeamTask(
        id = qaId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskKey = "ARB-106",
        title = "Executive Command Sign-Off & Load Verification",
        description = "Final end-to-end verification of cascading blocker alerts and velocity telemetry.",
        assigneeId = members[0].id,
        assigneeName = members[0].displayName,
        assigneeEmail = members[0].email,
        assigneeRole = members[0].roleTitle,
        parentTaskId = rootId,
        prerequisiteIds = listOf(syncId, calSyncId),
        status = TaskStatus.CASCADED_BLOCKED.name,
        priority = TaskPriority.CRITICAL.name,
        progressPercent = 10L,
        blockerReason = "Cascaded lock from ARB-102: External VPC firewall rule for webhook egress pending SecOps approval",
        dueDateMillis = now + 6 * dayMs,
        reminderOffsetMinutes = 120L,
        externalRef = "JIRA-106"
      )
    )
    sampleTasks.forEach { createTask(it) }

    createBlockerAlert(
      BlockerAlert(
        id = "blk_${uid.take(6)}_1",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        taskId = schemaId,
        taskKey = "ARB-102",
        taskTitle = "Distributed DAG Event Stream & Webhook Broker",
        reportedByName = "Priya Sharma (priya.junior@gmail.com)",
        assigneeName = "Marcus Chen",
        reason = "External VPC firewall rule for webhook egress pending SecOps approval",
        severity = BlockerSeverity.CRITICAL.name,
        impactedTaskKeys = listOf("ARB-103", "ARB-106"),
        status = BlockerStatus.ACTIVE.name,
        resolutionNote = ""
      )
    )

    saveIntegration(
      IntegrationConfig(
        id = "int_${uid.take(6)}_jira",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        provider = IntegrationProvider.JIRA.name,
        name = "Core Platform Jira Cloud",
        webhookUrl = "https://httpbin.org/post",
        projectKey = "ARB",
        autoDispatchBlockers = true,
        autoSyncCalendar = true,
        lastSyncSummary = "Connected • Auto-dispatching critical blockers"
      )
    )

    val docSpecId = "doc_${uid.take(6)}_spec"
    val docRunbookId = "doc_${uid.take(6)}_runbook"
    createDocument(
      TeamDocument(
        id = docSpecId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        title = "ARB-102 Distributed DAG Webhook Architecture Spec",
        category = DocumentCategory.SPEC.name,
        externalUrl = "https://docs.google.com/document/d/arborflow-dag-spec",
        contentSummary = "Defines idempotent webhook delivery headers (X-ArborFlow-Event), exponential backoff policy, and SecOps VPC egress allowlist rules for port 443.",
        linkedTaskKey = "ARB-102",
        uploadedByName = members[0].displayName,
        uploadedByEmail = members[0].email
      )
    )
    createDocument(
      TeamDocument(
        id = docRunbookId,
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        title = "Junior Onboarding & Sprint Handover Runbook",
        category = DocumentCategory.RUNBOOK.name,
        externalUrl = "https://drive.google.com/drive/folders/arborflow-team-docs",
        contentSummary = "Checklist for Juniors joining with Gmail: 1) Filter tasks by 'Assigned to My Gmail', 2) Advance task status as PRs move to review, 3) Flag blockers immediately so downstream owners are notified.",
        linkedTaskKey = "ARB-100",
        uploadedByName = members[1].displayName,
        uploadedByEmail = members[1].email
      )
    )

    createMessage(
      TeamMessage(
        id = "msg_${uid.take(6)}_1",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        senderName = members[0].displayName,
        senderEmail = members[0].email,
        senderRole = "Senior Lead",
        text = "Welcome team! I've uploaded the DAG Webhook Architecture Spec and assigned ARB-102 & ARB-104 by Gmail.",
        linkedTaskKey = "ARB-102",
        linkedDocId = docSpecId,
        linkedDocTitle = "ARB-102 Distributed DAG Webhook Architecture Spec"
      )
    )
    createMessage(
      TeamMessage(
        id = "msg_${uid.take(6)}_2",
        userId = uid,
        workspaceId = SHARED_WORKSPACE_ID,
        senderName = "Priya Sharma",
        senderEmail = "priya.junior@gmail.com",
        senderRole = "Junior Backend Engineer",
        text = "Thanks! Reviewing the spec now. I flagged the VPC firewall blocker on ARB-102 so everyone sees the downstream impact.",
        linkedTaskKey = "ARB-102",
        linkedDocId = "",
        linkedDocTitle = ""
      )
    )
  }
}
