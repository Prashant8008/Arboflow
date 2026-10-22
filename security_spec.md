# ArborFlow Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Identity Isolation**: Every document in `/tasks/{taskId}`, `/blockers/{blockerId}`, `/members/{memberId}`, and `/integrations/{integrationId}` MUST belong to an authenticated user (`request.auth != null`) and enforce `userId == request.auth.uid`.
2. **Schema & Bounds Integrity**: Every string field must be length-bounded, every enum field must match its allowed uppercase values (`TODO`, `IN_PROGRESS`, `IN_REVIEW`, `BLOCKED`, `CASCADED_BLOCKED`, `DONE`, etc.), and every array (`prerequisiteIds`, `impactedTaskKeys`) must be bounded (`size() <= 20`).
3. **Immutability**: `userId` and `createdAt` cannot be modified after creation.
4. **Temporal Integrity**: `createdAt` and `updatedAt` must be Firestore `timestamp` objects with `<= request.time`.
5. **Query Enforcer**: Every `allow list` rule must verify `existing().userId == request.auth.uid` so un-filtered queries are rejected.

## 2. The "Dirty Dozen" Adversarial Payloads
1. Unauthenticated read on `/tasks`.
2. Cross-user read (`alice` reading `bob`'s task).
3. Unfiltered list query on `/tasks` without `.where("userId", "==", uid)`.
4. Spoofed `userId` on task creation (`alice` creating a task with `userId: "bob"`).
5. Shadow field injection (`isVerified: true` extra property on create/update).
6. Mutating immutable `userId` on task update.
7. Mutating immutable `createdAt` on task update.
8. Invalid enum value (`status: "HACKED"` on task).
9. Out-of-bounds progress percentage (`progressPercent: 150`).
10. Oversized array (`prerequisiteIds` with 25 items).
11. Non-timestamp `createdAt` (passing integer epoch millis instead of ServerTimestamp).
12. Cross-user delete (`alice` deleting `bob`'s blocker alert).
