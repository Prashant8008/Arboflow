package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BlockerAlert
import com.example.data.model.TaskStatus
import com.example.data.model.TeamMember
import com.example.data.model.TeamTask
import com.example.domain.IntegrationAndReminderManager
import com.example.domain.TreeDependencyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ArborFlow", appName)
  }

  @Test
  fun `tree dependency engine computes roll-up and cascading blockers`() {
    val root = TeamTask(id = "t1", taskKey = "ARB-1", title = "Root", status = TaskStatus.IN_PROGRESS.name, progressPercent = 0L)
    val child1 = TeamTask(id = "t2", taskKey = "ARB-2", title = "Child 1", parentTaskId = "t1", status = TaskStatus.DONE.name, progressPercent = 100L)
    val child2 = TeamTask(id = "t3", taskKey = "ARB-3", title = "Child 2", parentTaskId = "t1", prerequisiteIds = listOf("t2"), status = TaskStatus.IN_PROGRESS.name, progressPercent = 50L)
    val leaf = TeamTask(id = "t4", taskKey = "ARB-4", title = "Leaf", parentTaskId = "t3", prerequisiteIds = listOf("t3"), status = TaskStatus.TODO.name, progressPercent = 0L)

    val all = listOf(root, child1, child2, leaf)
    val tree = TreeDependencyEngine.buildTaskTree(all)
    assertEquals(1, tree.size)
    assertEquals(50, tree.first().rolledUpProgress)

    val impactedByChild2 = TreeDependencyEngine.findCascadingImpactedTasks("t3", all)
    assertTrue(impactedByChild2.any { it.id == "t4" })
  }

  @Test
  fun `ios companion generators output html report, apple calendar ics, and swiftui bridge`() {
    val sampleTask = TeamTask(
      id = "t100",
      taskKey = "ARB-100",
      title = "Cross-Platform Sync Milestone",
      assigneeName = "Elena Vance",
      status = TaskStatus.IN_PROGRESS.name,
      progressPercent = 65L,
      reminderOffsetMinutes = 30L
    )
    val sampleBlocker = BlockerAlert(
      id = "b1",
      taskId = "t100",
      taskKey = "ARB-100",
      taskTitle = "Cross-Platform Sync Milestone",
      reportedByName = "Priya Sharma",
      assigneeName = "Elena Vance",
      reason = "Waiting on certificate provisioning",
      impactedTaskKeys = listOf("ARB-101")
    )
    val sampleMember = TeamMember(
      id = "m1",
      displayName = "Elena Vance",
      roleTitle = "VP Engineering",
      department = "Executive"
    )

    val html = IntegrationAndReminderManager.generateIosWebDashboardHtml(
      tasks = listOf(sampleTask),
      blockers = listOf(sampleBlocker),
      members = listOf(sampleMember)
    )
    assertTrue(html.contains("apple-mobile-web-app-capable"))
    assertTrue(html.contains("ARB-100"))

    val ics = IntegrationAndReminderManager.generateIcsSchedule(listOf(sampleTask))
    assertTrue(ics.contains("X-APPLE-CALENDAR-COLOR:#6366F1"))
    assertTrue(ics.contains("ACTION:AUDIO"))

    val swiftCode = IntegrationAndReminderManager.generateSwiftUiFirebaseBridgeCode(
      projectId = "gen-lang-client-0242699036",
      databaseId = "ai-studio-android-arborflo-9c053938-150a-49a4-9c8c-dc2a7d40028f"
    )
    assertTrue(swiftCode.contains("Firestore.firestore(database: firestoreDatabaseId)"))
    assertTrue(swiftCode.contains("struct TeamTask: Codable"))
  }
}
