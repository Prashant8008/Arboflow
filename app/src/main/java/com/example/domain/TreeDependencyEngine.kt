package com.example.domain

import com.example.data.model.TaskStatus
import com.example.data.model.TeamTask

data class TreeTaskNode(
  val task: TeamTask,
  val depth: Int,
  val children: List<TreeTaskNode>,
  val rolledUpProgress: Int,
  val upstreamTasks: List<TeamTask>,
  val downstreamTasks: List<TeamTask>,
  val isOnCriticalPath: Boolean
)

object TreeDependencyEngine {

  /**
   * Builds a hierarchical tree with depth, rolled-up progress from children,
   * upstream prerequisites, downstream dependents, and critical-path detection.
   */
  fun buildTaskTree(tasks: List<TeamTask>): List<TreeTaskNode> {
    if (tasks.isEmpty()) return emptyList()
    val byId = tasks.associateBy { it.id }
    val childrenByParent = tasks.groupBy { it.parentTaskId }

    fun computeRollUpProgress(taskId: String, visited: Set<String> = emptySet()): Int {
      if (taskId in visited) return byId[taskId]?.progressPercent?.toInt() ?: 0
      val task = byId[taskId] ?: return 0
      if (task.status == TaskStatus.DONE.name) return 100
      val children = childrenByParent[taskId].orEmpty()
      if (children.isEmpty()) return task.progressPercent.toInt().coerceIn(0, 100)
      val nextVisited = visited + taskId
      val childAvg = children.map { computeRollUpProgress(it.id, nextVisited) }.average().toInt()
      return childAvg.coerceIn(0, 100)
    }

    fun getDownstreamTasks(taskId: String): List<TeamTask> {
      return tasks.filter { candidate ->
        candidate.id != taskId &&
          (candidate.parentTaskId == taskId || taskId in candidate.prerequisiteIds)
      }
    }

    fun buildNode(task: TeamTask, depth: Int, visited: Set<String>): TreeTaskNode {
      val nextVisited = visited + task.id
      val rawChildren = childrenByParent[task.id].orEmpty()
        .filter { it.id !in nextVisited }
        .sortedBy { it.taskKey }
      val childNodes = rawChildren.map { buildNode(it, depth + 1, nextVisited) }
      val upstream = task.prerequisiteIds.mapNotNull { byId[it] }
      val downstream = getDownstreamTasks(task.id)
      val isCritical = task.priority == "CRITICAL" ||
        task.status == TaskStatus.BLOCKED.name ||
        task.status == TaskStatus.CASCADED_BLOCKED.name ||
        downstream.size >= 2

      return TreeTaskNode(
        task = task,
        depth = depth,
        children = childNodes,
        rolledUpProgress = computeRollUpProgress(task.id),
        upstreamTasks = upstream,
        downstreamTasks = downstream,
        isOnCriticalPath = isCritical
      )
    }

    val roots = tasks.filter { it.parentTaskId.isBlank() || it.parentTaskId !in byId }
      .sortedBy { it.taskKey }
    return roots.map { buildNode(it, 0, emptySet()) }
  }

  /**
   * Flattens the hierarchical tree into an ordered list for the Expandable Tree View,
   * respecting collapsed node IDs.
   */
  fun flattenTree(
    roots: List<TreeTaskNode>,
    collapsedIds: Set<String>
  ): List<TreeTaskNode> {
    val result = mutableListOf<TreeTaskNode>()
    fun traverse(node: TreeTaskNode) {
      result.add(node)
      if (node.task.id !in collapsedIds) {
        node.children.forEach { traverse(it) }
      }
    }
    roots.forEach { traverse(it) }
    return result
  }

  /**
   * Finds all transitive downstream tasks (via prerequisiteIds or child hierarchy)
   * that are impacted when [rootTaskId] becomes BLOCKED.
   */
  fun findCascadingImpactedTasks(
    rootTaskId: String,
    allTasks: List<TeamTask>
  ): List<TeamTask> {
    val impacted = mutableListOf<TeamTask>()
    val visited = mutableSetOf(rootTaskId)
    val queue = ArrayDeque<String>()
    queue.add(rootTaskId)

    while (queue.isNotEmpty()) {
      val currentId = queue.removeFirst()
      val directDependents = allTasks.filter { candidate ->
        candidate.id !in visited &&
          candidate.status != TaskStatus.DONE.name &&
          (candidate.parentTaskId == currentId || currentId in candidate.prerequisiteIds)
      }
      for (dep in directDependents) {
        visited.add(dep.id)
        impacted.add(dep)
        queue.add(dep.id)
      }
    }
    return impacted
  }

  /**
   * Checks whether adding [candidatePrerequisiteId] to [taskId] would create a dependency cycle.
   */
  fun wouldCreateCycle(
    taskId: String,
    candidatePrerequisiteId: String,
    allTasks: List<TeamTask>
  ): Boolean {
    if (taskId == candidatePrerequisiteId) return true
    val byId = allTasks.associateBy { it.id }
    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(candidatePrerequisiteId)

    while (queue.isNotEmpty()) {
      val curr = queue.removeFirst()
      if (curr == taskId) return true
      if (!visited.add(curr)) continue
      val node = byId[curr] ?: continue
      node.prerequisiteIds.forEach { queue.add(it) }
      if (node.parentTaskId.isNotBlank()) queue.add(node.parentTaskId)
    }
    return false
  }
}
