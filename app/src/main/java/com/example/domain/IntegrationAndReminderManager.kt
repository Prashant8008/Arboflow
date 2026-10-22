package com.example.domain

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.data.model.BlockerAlert
import com.example.data.model.BlockerStatus
import com.example.data.model.TaskPriority
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object IntegrationAndReminderManager {

  private const val CHANNEL_BLOCKERS = "arborflow_blockers_channel"
  private const val CHANNEL_REMINDERS = "arborflow_reminders_channel"
  private val httpClient = OkHttpClient()

  fun ensureNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      val blockerChannel = NotificationChannel(
        CHANNEL_BLOCKERS,
        "Critical Blocker Alerts",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Real-time notifications when upstream or cascading blockers occur"
      }
      val reminderChannel = NotificationChannel(
        CHANNEL_REMINDERS,
        "Task Deadline Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Scheduled task deadline and check-in reminders"
      }
      manager.createNotificationChannel(blockerChannel)
      manager.createNotificationChannel(reminderChannel)
    }
  }

  fun sendBlockerNotification(context: Context, alert: BlockerAlert): Boolean {
    ensureNotificationChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    val impactedText = if (alert.impactedTaskKeys.isEmpty()) {
      "Direct node blocked (${alert.severity})"
    } else {
      "Cascaded lock to: ${alert.impactedTaskKeys.joinToString(", ")}"
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKERS)
      .setSmallIcon(android.R.drawable.stat_notify_error)
      .setContentTitle("🚨 Blocker [${alert.taskKey}]: ${alert.taskTitle}")
      .setContentText("${alert.reason} • $impactedText")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("Reported by ${alert.reportedByName} (Assigned: ${alert.assigneeName})\nReason: ${alert.reason}\n$impactedText")
      )
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(context).notify(alert.taskKey.hashCode(), notification)
    return true
  }

  fun sendTaskReminderNotification(context: Context, task: TeamTask): Boolean {
    ensureNotificationChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
      .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
      .setContentTitle("⏰ Task Reminder [${task.taskKey}]: ${task.title}")
      .setContentText("Owner: ${task.assigneeName} • Progress: ${task.progressPercent}% • Lead: ${task.reminderOffsetMinutes}m")
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setAutoCancel(true)
      .build()

    NotificationManagerCompat.from(context).notify(task.id.hashCode(), notification)
    return true
  }

  /**
   * Launches the Android system Calendar insert intent for the task deadline & reminder.
   */
  fun openSystemCalendarSync(context: Context, task: TeamTask) {
    val startMillis = if (task.dueDateMillis > 0L) {
      task.dueDateMillis - (task.reminderOffsetMinutes * 60_000L)
    } else {
      System.currentTimeMillis() + 3_600_000L
    }
    val endMillis = startMillis + 3_600_000L

    val intent = Intent(Intent.ACTION_INSERT).apply {
      data = CalendarContract.Events.CONTENT_URI
      putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
      putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
      putExtra(CalendarContract.Events.TITLE, "[${task.taskKey}] ${task.title}")
      putExtra(
        CalendarContract.Events.DESCRIPTION,
        "ArborFlow Task (${task.status})\nAssignee: ${task.assigneeName}\nPriority: ${task.priority}\n\n${task.description}"
      )
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
  }

  /**
   * Generates an Apple Calendar (iOS / macOS) and RFC 5545 compliant iCalendar (.ics) schedule
   * with X-WR-CALNAME, X-APPLE-CALENDAR-COLOR, and native DISPLAY + AUDIO VALARM reminders.
   */
  fun generateIcsSchedule(tasks: List<TeamTask>): String {
    val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val sb = StringBuilder()
    sb.appendLine("BEGIN:VCALENDAR")
    sb.appendLine("VERSION:2.0")
    sb.appendLine("PRODID:-//ArborFlow//iOS & Android Team Dependency Tree//EN")
    sb.appendLine("CALSCALE:GREGORIAN")
    sb.appendLine("METHOD:PUBLISH")
    sb.appendLine("X-WR-CALNAME:ArborFlow Team Dependency Tree")
    sb.appendLine("X-WR-TIMEZONE:UTC")
    sb.appendLine("X-APPLE-CALENDAR-COLOR:#6366F1")

    val nowStr = utcFormat.format(Date())
    tasks.forEach { task ->
      val dueMs = if (task.dueDateMillis > 0L) task.dueDateMillis else System.currentTimeMillis() + 86_400_000L
      val startStr = utcFormat.format(Date(dueMs - 3_600_000L))
      val endStr = utcFormat.format(Date(dueMs))
      sb.appendLine("BEGIN:VEVENT")
      sb.appendLine("UID:${task.id.ifBlank { task.taskKey }}@arborflow.app")
      sb.appendLine("DTSTAMP:$nowStr")
      sb.appendLine("DTSTART:$startStr")
      sb.appendLine("DTEND:$endStr")
      sb.appendLine("SUMMARY:[${task.taskKey}] ${task.title} (${task.status})")
      sb.appendLine("DESCRIPTION:Assignee: ${task.assigneeName} | Progress: ${task.progressPercent}% | Priority: ${task.priority} | Ref: ${task.externalRef}")
      sb.appendLine("STATUS:${if (task.status == TaskStatus.DONE.name) "CONFIRMED" else "TENTATIVE"}")
      if (task.reminderOffsetMinutes > 0) {
        sb.appendLine("BEGIN:VALARM")
        sb.appendLine("TRIGGER:-PT${task.reminderOffsetMinutes}M")
        sb.appendLine("ACTION:DISPLAY")
        sb.appendLine("DESCRIPTION:ArborFlow Reminder: [${task.taskKey}] ${task.title}")
        sb.appendLine("END:VALARM")
        sb.appendLine("BEGIN:VALARM")
        sb.appendLine("TRIGGER:-PT${task.reminderOffsetMinutes}M")
        sb.appendLine("ACTION:AUDIO")
        sb.appendLine("END:VALARM")
      }
      sb.appendLine("END:VEVENT")
    }
    sb.appendLine("END:VCALENDAR")
    return sb.toString()
  }

  /**
   * Generates a universal calendar registration link and webcal:// subscription URI for iOS & Web users.
   */
  fun generateAppleCalendarWebCalAndUniversalLink(task: TeamTask): String {
    val utcFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.US).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
    val dueMs = if (task.dueDateMillis > 0L) task.dueDateMillis else System.currentTimeMillis() + 86_400_000L
    val startStr = utcFormat.format(Date(dueMs - 3_600_000L))
    val endStr = utcFormat.format(Date(dueMs))
    val encodedTitle = Uri.encode("[${task.taskKey}] ${task.title}")
    val encodedDetails = Uri.encode(
      "ArborFlow Dependency Node (${task.status})\nAssignee: ${task.assigneeName}\nProgress: ${task.progressPercent}%\n${task.description}"
    )
    val webCalLink = "webcal://arborflow.app/calendars/${task.taskKey.lowercase(Locale.US)}.ics"
    val universalCalendarUrl =
      "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$encodedTitle&dates=$startStr/$endStr&details=$encodedDetails"
    return "iOS WebCal Feed: $webCalLink\nUniversal Calendar Link: $universalCalendarUrl"
  }

  /**
   * Generates a self-contained, mobile-responsive HTML5 Executive & Dependency Tree Dashboard
   * optimized for iOS Safari, iPadOS, and desktop web browsers.
   */
  fun generateIosWebDashboardHtml(
    tasks: List<TeamTask>,
    blockers: List<BlockerAlert>,
    members: List<TeamMember>
  ): String {
    val roots = TreeDependencyEngine.buildTaskTree(tasks)
    val overallRollUp = if (roots.isNotEmpty()) {
      roots.map { it.rolledUpProgress }.average().toInt()
    } else if (tasks.isNotEmpty()) {
      tasks.map { it.progressPercent }.average().toInt()
    } else {
      0
    }
    val activeBlockers = blockers.filter { it.status != BlockerStatus.RESOLVED.name }
    val doneCount = tasks.count { it.status == TaskStatus.DONE.name }

    fun renderTreeNodeHtml(node: TreeTaskNode): String {
      val t = node.task
      val badgeColor = when (t.status) {
        TaskStatus.DONE.name -> "#10B981"
        TaskStatus.IN_PROGRESS.name -> "#6366F1"
        TaskStatus.IN_REVIEW.name -> "#06B6D4"
        TaskStatus.BLOCKED.name -> "#F43F5E"
        TaskStatus.CASCADED_BLOCKED.name -> "#F59E0B"
        else -> "#94A3B8"
      }
      val prereqHtml = if (node.upstreamTasks.isNotEmpty()) {
        "<div class='meta'>Depends on: ${node.upstreamTasks.joinToString(", ") { it.taskKey }}</div>"
      } else ""
      val blockerHtml = if (t.blockerReason.isNotBlank()) {
        "<div class='blocker-pill'>⚠ ${escapeHtml(t.blockerReason)}</div>"
      } else ""
      val childrenHtml = if (node.children.isNotEmpty()) {
        "<div class='children'>" + node.children.joinToString("") { renderTreeNodeHtml(it) } + "</div>"
      } else ""

      return """
        <details open class="tree-node" style="border-left: 3px solid $badgeColor;">
          <summary>
            <span class="key" style="color:$badgeColor">${escapeHtml(t.taskKey)}</span>
            <strong>${escapeHtml(t.title)}</strong>
            <span class="badge" style="background:${badgeColor}22;color:$badgeColor;border:1px solid ${badgeColor}66;">
              ${t.status.replace("_", " ")} • ${node.rolledUpProgress}%
            </span>
          </summary>
          <div class="node-body">
            <div class="meta">Assignee: <b>${escapeHtml(t.assigneeName)}</b> (${escapeHtml(t.assigneeRole.ifBlank { "Team" })}) • Priority: ${t.priority}</div>
            $prereqHtml
            $blockerHtml
            <div class="progress-track"><div class="progress-fill" style="width:${node.rolledUpProgress}%;background:$badgeColor;"></div></div>
            $childrenHtml
          </div>
        </details>
      """.trimIndent()
    }

    val treeHtml = if (roots.isEmpty()) {
      "<p>No tasks in workspace yet.</p>"
    } else {
      roots.joinToString("\n") { renderTreeNodeHtml(it) }
    }

    val blockersHtml = if (activeBlockers.isEmpty()) {
      "<div class='card ok-card'>✓ Zero Active Blockers — All dependency branches flowing normally.</div>"
    } else {
      activeBlockers.joinToString("\n") { b ->
        val cascadeInfo = if (b.impactedTaskKeys.isNotEmpty()) {
          "<div class='cascade-tag'>Cascaded Lock to: ${b.impactedTaskKeys.joinToString(", ")}</div>"
        } else ""
        """
          <div class="card blocker-card">
            <div class="row">
              <span class="key" style="color:#F43F5E">🚨 ${escapeHtml(b.taskKey)} • ${b.severity}</span>
              <span class="badge" style="background:#F43F5E33;color:#F43F5E;">${b.status}</span>
            </div>
            <h4>${escapeHtml(b.taskTitle)}</h4>
            <p>${escapeHtml(b.reason)}</p>
            $cascadeInfo
            <div class="meta">Reported by ${escapeHtml(b.reportedByName)} • Assigned: ${escapeHtml(b.assigneeName)}</div>
          </div>
        """.trimIndent()
      }
    }

    return """
      <!DOCTYPE html>
      <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, viewport-fit=cover" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
        <title>ArborFlow — iOS & Web Executive Command Report</title>
        <style>
          :root { color-scheme: dark; }
          body {
            margin: 0; padding: 20px;
            background: #0B0F17; color: #F8FAFC;
            font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Plus Jakarta Sans', sans-serif;
          }
          .header {
            background: linear-gradient(135deg, #1E1B4B, #0F172A);
            border: 1px solid #28354C; border-radius: 20px; padding: 20px; margin-bottom: 18px;
          }
          .tag { color: #06B6D4; font-size: 11px; letter-spacing: 1.2px; text-transform: uppercase; font-weight: 700; }
          h1 { margin: 6px 0 4px 0; font-size: 26px; }
          .kpi-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 20px; }
          .kpi { background: #131A26; border: 1px solid #28354C; border-radius: 16px; padding: 14px; }
          .kpi-val { font-size: 24px; font-weight: 800; margin-top: 4px; }
          .card { background: #131A26; border: 1px solid #28354C; border-radius: 16px; padding: 16px; margin-bottom: 12px; }
          .blocker-card { border-color: #F43F5E88; background: #4C051933; }
          .ok-card { border-color: #10B98166; color: #10B981; }
          .tree-node {
            background: #131A26; border: 1px solid #28354C; border-radius: 14px;
            padding: 12px; margin-top: 10px;
          }
          summary { cursor: pointer; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
          .key { font-family: ui-monospace, 'SF Mono', monospace; font-weight: 700; font-size: 12px; }
          .badge { font-size: 11px; padding: 2px 8px; border-radius: 99px; font-weight: 600; margin-left: auto; }
          .node-body { margin-top: 8px; padding-left: 8px; }
          .children { margin-left: 14px; }
          .meta { color: #94A3B8; font-size: 12px; margin-top: 4px; }
          .blocker-pill { background: #F43F5E22; color: #F43F5E; padding: 6px 10px; border-radius: 8px; font-size: 12px; margin-top: 6px; }
          .cascade-tag { color: #F59E0B; font-size: 12px; font-weight: 600; margin-top: 6px; }
          .progress-track { height: 6px; background: #1E293B; border-radius: 99px; overflow: hidden; margin-top: 8px; }
          .progress-fill { height: 100%; border-radius: 99px; }
        </style>
      </head>
      <body>
        <div class="header">
          <div class="tag">ARBORFLOW • iOS SAFARI & WEB EXECUTIVE VIEW</div>
          <h1>Live Team Dependency Tree</h1>
          <div class="meta">Synced from ArborFlow Cloud • ${members.size.coerceAtLeast(1)} Squad Members • ${tasks.size} Total Nodes</div>
        </div>
        <div class="kpi-grid">
          <div class="kpi"><div class="tag" style="color:#10B981">TREE ROLL-UP</div><div class="kpi-val">$overallRollUp%</div><div class="meta">$doneCount/${tasks.size} Done</div></div>
          <div class="kpi"><div class="tag" style="color:#F43F5E">BLOCKERS</div><div class="kpi-val">${activeBlockers.size}</div><div class="meta">Active Alerts</div></div>
          <div class="kpi"><div class="tag" style="color:#06B6D4">SQUAD</div><div class="kpi-val">${members.size.coerceAtLeast(1)}</div><div class="meta">Connected</div></div>
        </div>
        <h3>Real-Time Blocker & Cascade Feed</h3>
        $blockersHtml
        <h3>Interactive Task Dependency Tree</h3>
        $treeHtml
      </body>
      </html>
    """.trimIndent()
  }

  private fun escapeHtml(input: String): String =
    input.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")

  /**
   * Generates a complete SwiftUI + FirebaseFirestore iOS companion source file configured with
   * the exact Firestore Enterprise database ID and zero-trust schema models.
   */
  fun generateSwiftUiFirebaseBridgeCode(projectId: String, databaseId: String): String {
    return """
      // ArborFlowiOSBridge.swift
      // Drop into your Xcode SwiftUI project with FirebaseFirestore & FirebaseAuth SPM packages.
      // Connects iOS devices in real time to the exact same ArborFlow Firestore database as Android.

      import SwiftUI
      import FirebaseCore
      import FirebaseAuth
      import FirebaseFirestore

      public enum ArborFlowFirebaseConfig {
          public static let projectId = "$projectId"
          public static let firestoreDatabaseId = "$databaseId"

          public static var db: Firestore {
              Firestore.firestore(database: firestoreDatabaseId)
          }
      }

      public struct TeamTask: Codable, Identifiable {
          @DocumentID public var id: String?
          public var userId: String
          public var taskKey: String
          public var title: String
          public var description: String
          public var assigneeId: String
          public var assigneeName: String
          public var assigneeRole: String
          public var parentTaskId: String
          public var prerequisiteIds: [String]
          public var status: String
          public var priority: String
          public var progressPercent: Int
          public var blockerReason: String
          public var dueDateMillis: Int64
          public var reminderOffsetMinutes: Int64
          public var externalRef: String
          @ServerTimestamp public var createdAt: Timestamp?
          @ServerTimestamp public var updatedAt: Timestamp?
      }

      public struct BlockerAlert: Codable, Identifiable {
          @DocumentID public var id: String?
          public var userId: String
          public var taskId: String
          public var taskKey: String
          public var taskTitle: String
          public var reportedByName: String
          public var assigneeName: String
          public var reason: String
          public var severity: String
          public var impactedTaskKeys: [String]
          public var status: String
          public var resolutionNote: String
          @ServerTimestamp public var createdAt: Timestamp?
          @ServerTimestamp public var updatedAt: Timestamp?
      }

      @MainActor
      public final class ArborFlowiOSViewModel: ObservableObject {
          @Published public var tasks: [TeamTask] = []
          @Published public var blockers: [BlockerAlert] = []
          private var taskListener: ListenerRegistration?
          private var blockerListener: ListenerRegistration?

          public func startRealtimeSync() {
              guard let uid = Auth.auth().currentUser?.uid else { return }
              let db = ArborFlowFirebaseConfig.db

              taskListener = db.collection("tasks")
                  .whereField("userId", isEqualTo: uid)
                  .addSnapshotListener { [weak self] snapshot, _ in
                      guard let docs = snapshot?.documents else { return }
                      self?.tasks = docs.compactMap { try? $0.data(as: TeamTask.self) }
                  }

              blockerListener = db.collection("blockers")
                  .whereField("userId", isEqualTo: uid)
                  .addSnapshotListener { [weak self] snapshot, _ in
                      guard let docs = snapshot?.documents else { return }
                      self?.blockers = docs.compactMap { try? $0.data(as: BlockerAlert.self) }
                  }
          }
      }
    """.trimIndent()
  }

  /**
   * Exports tasks as JSON or CSV formatted for Jira, Linear, or Trello.
   */
  fun exportTasksForPmTool(tasks: List<TeamTask>, format: String): String {
    if (format.equals("CSV", ignoreCase = true)) {
      val header = "Issue Key,Summary,Status,Priority,Assignee,Parent Key,Progress,External Ref"
      val rows = tasks.map { t ->
        val safeTitle = t.title.replace("\"", "\"\"")
        "\"${t.taskKey}\",\"$safeTitle\",\"${t.status}\",\"${t.priority}\",\"${t.assigneeName}\",\"${t.parentTaskId}\",${t.progressPercent},\"${t.externalRef}\""
      }
      return (listOf(header) + rows).joinToString("\n")
    } else {
      val arr = JSONArray()
      tasks.forEach { t ->
        arr.put(
          JSONObject().apply {
            put("key", t.taskKey)
            put("title", t.title)
            put("description", t.description)
            put("status", t.status)
            put("priority", t.priority)
            put("assignee", t.assigneeName)
            put("parentTaskId", t.parentTaskId)
            put("prerequisiteIds", JSONArray(t.prerequisiteIds))
            put("progressPercent", t.progressPercent)
            put("externalRef", t.externalRef)
          }
        )
      }
      return arr.toString(2)
    }
  }

  /**
   * Parses imported CSV or JSON lines from Jira/Linear/Trello into TeamTask models.
   */
  fun parseImportedTasks(rawPayload: String, projectPrefix: String, existingCount: Int): List<TeamTask> {
    val trimmed = rawPayload.trim()
    if (trimmed.isEmpty()) return emptyList()
    val now = System.currentTimeMillis()
    val result = mutableListOf<TeamTask>()

    if (trimmed.startsWith("[")) {
      runCatching {
        val arr = JSONArray(trimmed)
        for (i in 0 until arr.length()) {
          val obj = arr.getJSONObject(i)
          val idx = existingCount + i + 1
          result.add(
            TeamTask(
              taskKey = obj.optString("key", "$projectPrefix-${100 + idx}").take(32),
              title = obj.optString("title", obj.optString("summary", "Imported Task $idx")).take(200),
              description = obj.optString("description", "Imported from external PM suite").take(1000),
              assigneeName = obj.optString("assignee", "Unassigned").ifBlank { "Unassigned" }.take(100),
              status = obj.optString("status", TaskStatus.TODO.name).uppercase(Locale.US).let { s ->
                if (TaskStatus.entries.any { it.name == s }) s else TaskStatus.TODO.name
              },
              priority = obj.optString("priority", TaskPriority.MEDIUM.name).uppercase(Locale.US).let { p ->
                if (TaskPriority.entries.any { it.name == p }) p else TaskPriority.MEDIUM.name
              },
              progressPercent = obj.optLong("progressPercent", 0L).coerceIn(0L, 100L),
              dueDateMillis = now + (idx * 86_400_000L),
              externalRef = obj.optString("externalRef", obj.optString("key", "$projectPrefix-$idx")).take(120)
            )
          )
        }
      }
    } else {
      val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }
      val dataLines = if (lines.firstOrNull()?.contains("Summary", ignoreCase = true) == true ||
        lines.firstOrNull()?.contains("Title", ignoreCase = true) == true
      ) {
        lines.drop(1)
      } else {
        lines
      }
      dataLines.forEachIndexed { i, line ->
        val cols = line.split(",").map { it.trim().removeSurrounding("\"") }
        val idx = existingCount + i + 1
        val key = cols.getOrNull(0)?.ifBlank { "$projectPrefix-${100 + idx}" } ?: "$projectPrefix-${100 + idx}"
        val title = cols.getOrNull(1)?.ifBlank { line } ?: line
        val statusRaw = cols.getOrNull(2)?.uppercase(Locale.US)?.replace(" ", "_") ?: "TODO"
        val priorityRaw = cols.getOrNull(3)?.uppercase(Locale.US) ?: "MEDIUM"
        val assignee = cols.getOrNull(4)?.ifBlank { "Unassigned" } ?: "Unassigned"

        result.add(
          TeamTask(
            taskKey = key.take(32),
            title = title.take(200),
            description = "Imported via PM Connector ($key)",
            assigneeName = assignee.take(100),
            status = if (TaskStatus.entries.any { it.name == statusRaw }) statusRaw else TaskStatus.TODO.name,
            priority = if (TaskPriority.entries.any { it.name == priorityRaw }) priorityRaw else TaskPriority.MEDIUM.name,
            progressPercent = if (statusRaw == "DONE") 100L else 0L,
            dueDateMillis = now + ((i + 2) * 86_400_000L),
            externalRef = key.take(120)
          )
        )
      }
    }
    return result
  }

  /**
   * Dispatches a live HTTP POST webhook payload using OkHttp to notify external systems.
   */
  suspend fun dispatchWebhookEvent(
    webhookUrl: String,
    eventType: String,
    payloadJson: JSONObject
  ): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
      require(webhookUrl.startsWith("http://") || webhookUrl.startsWith("https://")) {
        "Webhook URL must start with https:// or http://"
      }
      val envelope = JSONObject().apply {
        put("source", "ArborFlow")
        put("event", eventType)
        put("timestamp", System.currentTimeMillis())
        put("data", payloadJson)
      }
      val body = envelope.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
      val request = Request.Builder()
        .url(webhookUrl)
        .post(body)
        .header("User-Agent", "ArborFlow-Android-Webhook/1.0")
        .build()

      httpClient.newCall(request).execute().use { response ->
        "HTTP ${response.code} (${response.message.ifBlank { "OK" }})"
      }
    }
  }
}
