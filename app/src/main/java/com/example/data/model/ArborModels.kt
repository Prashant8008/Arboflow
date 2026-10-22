package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SHARED_WORKSPACE_ID = "ARBOR-TEAM-1"

enum class TaskStatus(val label: String) {
  TODO("To Do"),
  IN_PROGRESS("In Progress"),
  IN_REVIEW("In Review"),
  BLOCKED("Blocked"),
  CASCADED_BLOCKED("Cascaded Lock"),
  DONE("Done")
}

enum class TaskPriority(val label: String) {
  LOW("Low"),
  MEDIUM("Medium"),
  HIGH("High"),
  CRITICAL("Critical")
}

enum class BlockerSeverity(val label: String) {
  WARNING("Warning"),
  HIGH("High"),
  CRITICAL("Critical")
}

enum class BlockerStatus(val label: String) {
  ACTIVE("Active"),
  ESCALATED("Escalated"),
  RESOLVED("Resolved")
}

enum class IntegrationProvider(val label: String) {
  JIRA("Jira Software"),
  LINEAR("Linear"),
  TRELLO("Trello"),
  CUSTOM_WEBHOOK("REST Webhook")
}

enum class DocumentCategory(val label: String) {
  SPEC("Architecture Spec"),
  DESIGN("UI / Figma Design"),
  DRIVE_DOC("Google Drive / Doc"),
  CODE_SNIPPET("Code / API Contract"),
  RUNBOOK("Ops Runbook")
}

@IgnoreExtraProperties
data class TeamTask(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val taskKey: String = "ARB-100",
  val title: String = "",
  val description: String = "",
  val assigneeId: String = "",
  val assigneeName: String = "Unassigned",
  val assigneeEmail: String = "",
  val assigneeRole: String = "",
  val parentTaskId: String = "",
  val prerequisiteIds: List<String> = emptyList(),
  val status: String = TaskStatus.TODO.name,
  val priority: String = TaskPriority.MEDIUM.name,
  val progressPercent: Long = 0L,
  val blockerReason: String = "",
  val dueDateMillis: Long = 0L,
  val reminderOffsetMinutes: Long = 60L,
  val externalRef: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BlockerAlert(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val taskId: String = "",
  val taskKey: String = "",
  val taskTitle: String = "",
  val reportedByName: String = "",
  val assigneeName: String = "",
  val reason: String = "",
  val severity: String = BlockerSeverity.HIGH.name,
  val impactedTaskKeys: List<String> = emptyList(),
  val status: String = BlockerStatus.ACTIVE.name,
  val resolutionNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TeamMember(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val displayName: String = "",
  val email: String = "",
  val roleTitle: String = "",
  val department: String = "",
  val isSenior: Boolean = false,
  val avatarColorHex: String = "#6366F1",
  val capacityHoursPerWeek: Long = 40L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IntegrationConfig(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val provider: String = IntegrationProvider.JIRA.name,
  val name: String = "",
  val webhookUrl: String = "",
  val projectKey: String = "ARB",
  val autoDispatchBlockers: Boolean = true,
  val autoSyncCalendar: Boolean = true,
  val lastSyncSummary: String = "Ready",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TeamMessage(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val senderName: String = "",
  val senderEmail: String = "",
  val senderRole: String = "Team Member",
  val text: String = "",
  val linkedTaskKey: String = "",
  val linkedDocId: String = "",
  val linkedDocTitle: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TeamDocument(
  @DocumentId val id: String = "",
  val userId: String = "",
  val workspaceId: String = SHARED_WORKSPACE_ID,
  val title: String = "",
  val category: String = DocumentCategory.SPEC.name,
  val externalUrl: String = "",
  val contentSummary: String = "",
  val linkedTaskKey: String = "",
  val uploadedByName: String = "",
  val uploadedByEmail: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
