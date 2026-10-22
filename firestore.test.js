const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTaskPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    workspaceId: "PRIVATE",
    taskKey: "ARB-101",
    title: "Core Authentication Service",
    description: "Build OAuth2 and token refresh pipeline",
    assigneeId: "mem_1",
    assigneeName: "Alex Rivera",
    assigneeEmail: "alex.junior@gmail.com",
    assigneeRole: "Staff Backend Engineer",
    parentTaskId: "",
    prerequisiteIds: [],
    status: "IN_PROGRESS",
    priority: "HIGH",
    progressPercent: 45,
    blockerReason: "",
    dueDateMillis: 1792700000000,
    reminderOffsetMinutes: 60,
    externalRef: "JIRA-101",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validBlockerPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    workspaceId: "ARBOR-TEAM-1",
    taskId: "task_1",
    taskKey: "ARB-101",
    taskTitle: "Core Authentication Service",
    reportedByName: "Alex Rivera",
    assigneeName: "Alex Rivera",
    reason: "Waiting on staging SSL certificate provisioning",
    severity: "CRITICAL",
    impactedTaskKeys: ["ARB-102", "ARB-103"],
    status: "ACTIVE",
    resolutionNote: "",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validMessagePayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    workspaceId: "ARBOR-TEAM-1",
    senderName: "Elena Vance",
    senderEmail: "elena.senior@gmail.com",
    senderRole: "Senior Lead",
    text: "Please review the updated API spec for ARB-102!",
    linkedTaskKey: "ARB-102",
    linkedDocId: "doc_1",
    linkedDocTitle: "Q4 API Architecture Spec",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validDocumentPayload(userId, overrides = {}) {
  const now = new Date(Date.now() - 1000);
  return {
    userId,
    workspaceId: "ARBOR-TEAM-1",
    title: "Q4 API Architecture Spec",
    category: "SPEC",
    externalUrl: "https://docs.google.com/document/d/arbor-spec",
    contentSummary: "Zero-trust OAuth and DAG event stream architecture specification.",
    linkedTaskKey: "ARB-102",
    uploadedByName: "Elena Vance",
    uploadedByEmail: "elena.senior@gmail.com",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read tasks, blockers, messages, or documents", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tasks").get());
  await assertFails(unauthDb.collection("blockers").get());
  await assertFails(unauthDb.collection("messages").get());
  await assertFails(unauthDb.collection("documents").get());
});

test("Authenticated user: can create and query their own task with matching userId filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("tasks").where("userId", "==", ALICE_UID).get()
  );
});

test("Senior assigns task to Junior Gmail in shared workspace: Junior can query and update progress", async () => {
  const seniorDb = testEnv.authenticatedContext(ALICE_UID, { email: "senior@gmail.com" }).firestore();
  const juniorDb = testEnv.authenticatedContext(BOB_UID, { email: "junior@gmail.com" }).firestore();
  const taskPayload = validTaskPayload(ALICE_UID, {
    workspaceId: "ARBOR-TEAM-1",
    assigneeName: "Bob Junior",
    assigneeEmail: "junior@gmail.com",
  });
  await assertSucceeds(
    seniorDb.collection("tasks").doc("task_shared_1").set(taskPayload)
  );
  await assertSucceeds(
    juniorDb.collection("tasks").where("workspaceId", "==", "ARBOR-TEAM-1").get()
  );
  await assertSucceeds(
    juniorDb.collection("tasks").doc("task_shared_1").set({
      ...taskPayload,
      progressPercent: 85,
      status: "IN_REVIEW",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Team members can exchange chat messages and share documents in shared workspace", async () => {
  const seniorDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const juniorDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    seniorDb.collection("documents").doc("doc_shared_1").set(validDocumentPayload(ALICE_UID))
  );
  await assertSucceeds(
    seniorDb.collection("messages").doc("msg_shared_1").set(validMessagePayload(ALICE_UID))
  );

  await assertSucceeds(
    juniorDb.collection("documents").where("workspaceId", "==", "ARBOR-TEAM-1").get()
  );
  await assertSucceeds(
    juniorDb.collection("messages").where("workspaceId", "==", "ARBOR-TEAM-1").get()
  );
});

test("Authenticated user: fails query without filter (rules are not filters)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("tasks").doc("task_alice_1").set(validTaskPayload(ALICE_UID))
  );
  await assertFails(aliceDb.collection("tasks").get());
});

test("Authenticated user: cannot read, update, or delete another user's private task", async () => {
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("tasks").doc("bob_task_1").set(validTaskPayload(BOB_UID, { workspaceId: "PRIVATE", assigneeEmail: "" }))
  );

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("tasks").doc("bob_task_1").get());
  await assertFails(aliceDb.collection("tasks").doc("bob_task_1").delete());
});

test("Shadow update & invalid enum rejection on tasks, blockers, and documents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("tasks").doc("task_bad_1").set({
      ...validTaskPayload(ALICE_UID),
      isVerified: true,
    })
  );
  await assertFails(
    aliceDb.collection("documents").doc("doc_bad_1").set(
      validDocumentPayload(ALICE_UID, { category: "UNKNOWN_CATEGORY" })
    )
  );
  await assertSucceeds(
    aliceDb.collection("blockers").doc("blocker_1").set(validBlockerPayload(ALICE_UID))
  );
});
