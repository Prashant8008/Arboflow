package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.DocumentCategory
import com.example.data.model.SHARED_WORKSPACE_ID
import com.example.data.model.TaskStatus
import com.example.data.model.TeamDocument
import com.example.data.model.TeamMessage
import com.example.data.model.TeamTask
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ArborFlowRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ArborFlowRepository(firestore)
    val uniqueId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val sampleTask = TeamTask(
      id = uniqueId,
      taskKey = "ARB-201",
      title = "Verify Security Rules",
      description = "Integration test task",
      assigneeName = "Alice",
      assigneeEmail = ALICE_EMAIL
    )

    val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(sampleTask) }
    assertTrue("Expected task creation to succeed: ${createResult.exceptionOrNull()}", createResult.isSuccess)
    assertEquals(uniqueId, createResult.getOrThrow())
  }

  @Test
  fun getUserTasks_authenticatedOwner_returnsMatchingTasks() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = ArborFlowRepository(firestore)
    val uniqueId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val sampleTask = TeamTask(
      id = uniqueId,
      taskKey = "ARB-202",
      title = "Owner Query Verification",
      assigneeName = "Alice",
      assigneeEmail = ALICE_EMAIL
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(sampleTask).getOrThrow() }

    val tasksResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserTasks() }
    assertTrue(tasksResult.isSuccess)
    assertTrue(tasksResult.getOrThrow().any { it.id == uniqueId })
  }

  @Test
  fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = ArborFlowRepository(firestore)
    val uniqueId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val sampleTask = TeamTask(
      id = uniqueId,
      taskKey = "ARB-203",
      title = "Realtime Stream Test",
      assigneeName = "Alice",
      assigneeEmail = ALICE_EMAIL
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(sampleTask).getOrThrow() }

    val emitted = withTimeout(FLOW_TIMEOUT_MS) {
      repository.observeTasks(uid).first { list -> list.any { it.id == uniqueId } }
    }
    assertTrue(emitted.any { it.id == uniqueId })
  }

  @Test
  fun seniorAssignsTaskToJuniorGmail_juniorCanObserveAndUpdateSharedTask() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val seniorRepo = ArborFlowRepository(firestore)
    val uniqueId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val assignedTask = TeamTask(
      id = uniqueId,
      workspaceId = SHARED_WORKSPACE_ID,
      taskKey = "ARB-205",
      title = "Junior Gmail Assigned Deliverable",
      assigneeName = "Bob Junior",
      assigneeEmail = BOB_EMAIL,
      assigneeRole = "Junior Engineer"
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { seniorRepo.createTask(assignedTask).getOrThrow() }

    val juniorUid = signInTestUser(BOB_EMAIL)
    val juniorRepo = ArborFlowRepository(firestore)
    val fetched = withTimeout(FLOW_TIMEOUT_MS) {
      juniorRepo.observeTasks(juniorUid).first { list -> list.any { it.id == uniqueId } }
    }.first { it.id == uniqueId }

    val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
      juniorRepo.updateTask(
        fetched.copy(
          status = TaskStatus.DONE.name,
          progressPercent = 100L
        )
      )
    }
    assertTrue("Expected Junior to update assigned shared task: ${updateResult.exceptionOrNull()}", updateResult.isSuccess)
  }

  @Test
  fun teamMembersCanExchangeMessagesAndShareDocumentsInSharedWorkspace() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val seniorRepo = ArborFlowRepository(firestore)
    val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(12)}"

    val sharedDoc = TeamDocument(
      id = docId,
      workspaceId = SHARED_WORKSPACE_ID,
      title = "Sprint Architecture Spec",
      category = DocumentCategory.SPEC.name,
      externalUrl = "https://docs.google.com/document/d/sprint-spec",
      contentSummary = "Detailed API specification for Juniors",
      linkedTaskKey = "ARB-205",
      uploadedByName = "Alice Senior",
      uploadedByEmail = ALICE_EMAIL
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { seniorRepo.createDocument(sharedDoc).getOrThrow() }

    val sharedMsg = TeamMessage(
      id = msgId,
      workspaceId = SHARED_WORKSPACE_ID,
      senderName = "Alice Senior",
      senderEmail = ALICE_EMAIL,
      senderRole = "Senior Lead",
      text = "Shared the Sprint Architecture Spec for ARB-205!",
      linkedTaskKey = "ARB-205",
      linkedDocId = docId,
      linkedDocTitle = sharedDoc.title
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { seniorRepo.createMessage(sharedMsg).getOrThrow() }

    val juniorUid = signInTestUser(BOB_EMAIL)
    val juniorRepo = ArborFlowRepository(firestore)

    val docs = withTimeout(FLOW_TIMEOUT_MS) {
      juniorRepo.observeDocuments(juniorUid).first { list -> list.any { it.id == docId } }
    }
    assertTrue(docs.any { it.id == docId })

    val msgs = withTimeout(FLOW_TIMEOUT_MS) {
      juniorRepo.observeMessages(juniorUid).first { list -> list.any { it.id == msgId } }
    }
    assertTrue(msgs.any { it.id == msgId })
  }

  @Test
  fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = ArborFlowRepository(firestore)
    val uniqueId = "task_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val sampleTask = TeamTask(
      id = uniqueId,
      workspaceId = "PRIVATE",
      taskKey = "ARB-204",
      title = "Alice Private Task",
      assigneeName = "Alice",
      assigneeEmail = ""
    )
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createTask(sampleTask).getOrThrow() }

    signInTestUser(BOB_EMAIL)
    val bobRepo = ArborFlowRepository(firestore)
    val result = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getTaskById(uniqueId) }
    assertTrue("Expected cross-user read to fail", result.isFailure)
    val ex = result.exceptionOrNull() as? FirebaseFirestoreException
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repository = ArborFlowRepository(firestore)

    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTasks("unauth_uid").first()
      }
      fail("Expected FirebaseFirestoreException.PERMISSION_DENIED")
    } catch (e: FirebaseFirestoreException) {
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@arborflow.test"
    const val BOB_EMAIL = "bob@arborflow.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
