package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskStatus
import com.example.data.model.TeamTask
import com.example.domain.TreeDependencyEngine
import com.example.domain.TreeTaskNode
import com.example.ui.TreeDisplayMode
import com.example.ui.theme.AmberProgress
import com.example.ui.theme.CrimsonBlocker
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.EmeraldPulse

@Composable
fun TaskTreeScreen(
  tasks: List<TeamTask>,
  displayMode: TreeDisplayMode,
  collapsedNodeIds: Set<String>,
  onDisplayModeChange: (TreeDisplayMode) -> Unit,
  onToggleCollapse: (String) -> Unit,
  onTaskClick: (TeamTask) -> Unit,
  onQuickAdvance: (TeamTask) -> Unit,
  onReportBlocker: (TeamTask) -> Unit,
  onCreateTaskClick: () -> Unit
) {
  val treeRoots = remember(tasks) { TreeDependencyEngine.buildTaskTree(tasks) }
  val flattenedNodes = remember(treeRoots, collapsedNodeIds) {
    TreeDependencyEngine.flattenTree(treeRoots, collapsedNodeIds)
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Header & View Switcher (Interactive 2D Node Graph vs. Expandable Hierarchy Tree)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "DEPENDENCY TREE & PROGRESS ROLL-UP",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.secondary
        )
        Text(
          text = "Interactive Node Graph & Hierarchy",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground
        )
      }
      Button(
        onClick = onCreateTaskClick,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("tree_add_task_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Node", modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add Node")
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Mode Toggle Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        .padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      TreeDisplayMode.entries.forEach { mode ->
        val selected = displayMode == mode
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
          modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clickable { onDisplayModeChange(mode) }
            .testTag("tree_mode_${mode.name.lowercase()}")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = if (mode == TreeDisplayMode.NODE_GRAPH) Icons.Default.Hub else Icons.Default.AccountTree,
              contentDescription = mode.label,
              tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = mode.label,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Legend Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      LegendDot(label = "Done", color = EmeraldPulse)
      LegendDot(label = "In Progress", color = ElectricIndigo)
      LegendDot(label = "In Review", color = CyanTelemetry)
      LegendDot(label = "Blocked", color = CrimsonBlocker)
      LegendDot(label = "Cascaded Lock", color = AmberProgress)
    }

    Spacer(modifier = Modifier.height(10.dp))

    if (tasks.isEmpty()) {
      Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No task nodes yet. Tap 'Add Node' or initialize the sample sprint on the Dashboard.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    } else if (displayMode == TreeDisplayMode.NODE_GRAPH) {
      InteractiveNodeGraphCanvas(
        roots = treeRoots,
        allTasks = tasks,
        onTaskClick = onTaskClick,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .testTag("hierarchy_tree_list"),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(flattenedNodes, key = { it.task.id }) { node ->
          ExpandableTreeRow(
            node = node,
            isCollapsed = node.task.id in collapsedNodeIds,
            onToggleCollapse = { onToggleCollapse(node.task.id) },
            onTaskClick = { onTaskClick(node.task) },
            onQuickAdvance = { onQuickAdvance(node.task) },
            onReportBlocker = { onReportBlocker(node.task) }
          )
        }
      }
    }
  }
}

@Composable
private fun LegendDot(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

private data class PositionedNode(
  val node: TreeTaskNode,
  val rect: Rect
)

@Composable
private fun InteractiveNodeGraphCanvas(
  roots: List<TreeTaskNode>,
  allTasks: List<TeamTask>,
  onTaskClick: (TeamTask) -> Unit,
  modifier: Modifier = Modifier
) {
  var scale by remember { mutableFloatStateOf(1f) }
  var panOffset by remember { mutableStateOf(Offset(30f, 30f)) }
  val textMeasurer = rememberTextMeasurer()

  // Compute layered DAG layout positions by depth
  val allNodes = remember(roots) {
    TreeDependencyEngine.flattenTree(roots, emptySet())
  }
  val nodesByDepth = remember(allNodes) {
    allNodes.groupBy { it.depth }.toSortedMap()
  }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF080C14)),
    modifier = modifier
      .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
      .testTag("interactive_node_graph_canvas")
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      val nodeWidthBase = 230f
      val nodeHeightBase = 104f
      val horizontalGapBase = 95f
      val verticalGapBase = 36f

      val positionedNodes = remember(nodesByDepth, scale, panOffset) {
        val list = mutableListOf<PositionedNode>()
        nodesByDepth.forEach { (depth, levelNodes) ->
          levelNodes.forEachIndexed { idx, treeNode ->
            val left = panOffset.x + (depth * (nodeWidthBase + horizontalGapBase)) * scale
            val top = panOffset.y + (idx * (nodeHeightBase + verticalGapBase)) * scale
            val w = nodeWidthBase * scale
            val h = nodeHeightBase * scale
            list.add(PositionedNode(treeNode, Rect(left, top, left + w, top + h)))
          }
        }
        list
      }

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
              scale = (scale * zoom).coerceIn(0.55f, 1.8f)
              panOffset += pan
            }
          }
          .pointerInput(positionedNodes) {
            detectTapGestures { tapOffset ->
              val hit = positionedNodes.firstOrNull { it.rect.contains(tapOffset) }
              if (hit != null) {
                onTaskClick(hit.node.task)
              }
            }
          }
      ) {
        // 1. Draw architectural CAD grid
        val gridSpacing = 48.dp.toPx() * scale
        val gridColor = Color(0xFF162032)
        if (gridSpacing > 16f) {
          var gx = (panOffset.x % gridSpacing + gridSpacing) % gridSpacing
          while (gx < size.width) {
            drawLine(gridColor, Offset(gx, 0f), Offset(gx, size.height), strokeWidth = 1f)
            gx += gridSpacing
          }
          var gy = (panOffset.y % gridSpacing + gridSpacing) % gridSpacing
          while (gy < size.height) {
            drawLine(gridColor, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 1f)
            gy += gridSpacing
          }
        }

        val byId = positionedNodes.associateBy { it.node.task.id }

        // 2. Draw parent-child & prerequisite cubic Bezier connectors
        positionedNodes.forEach { target ->
          val parentPos = byId[target.node.task.parentTaskId]
          if (parentPos != null) {
            val start = Offset(parentPos.rect.right, parentPos.rect.center.y)
            val end = Offset(target.rect.left, target.rect.center.y)
            val ctrlX = (start.x + end.x) / 2f
            val path = Path().apply {
              moveTo(start.x, start.y)
              cubicTo(ctrlX, start.y, ctrlX, end.y, end.x, end.y)
            }
            val edgeColor = statusColorFor(target.node.task.status)
            drawPath(
              path = path,
              color = edgeColor.copy(alpha = 0.85f),
              style = Stroke(width = 3f * scale, cap = StrokeCap.Round)
            )
          }

          // Cross-branch prerequisite dashed lines
          target.node.task.prerequisiteIds
            .filter { it != target.node.task.parentTaskId }
            .forEach { prereqId ->
              val prereqPos = byId[prereqId]
              if (prereqPos != null) {
                val start = Offset(prereqPos.rect.right, prereqPos.rect.center.y)
                val end = Offset(target.rect.left, target.rect.center.y)
                val ctrlX = (start.x + end.x) / 2f
                val path = Path().apply {
                  moveTo(start.x, start.y)
                  cubicTo(ctrlX, start.y - 24f * scale, ctrlX, end.y + 24f * scale, end.x, end.y)
                }
                drawPath(
                  path = path,
                  color = CyanTelemetry.copy(alpha = 0.75f),
                  style = Stroke(
                    width = 2.2f * scale,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * scale, 8f * scale), 0f)
                  )
                )
              }
            }
        }

        // 3. Draw Task Nodes with Rolled-Up Progress & Blocker Halos
        positionedNodes.forEach { item ->
          val task = item.node.task
          val r = item.rect
          val statusColor = statusColorFor(task.status)
          val isBlocked = task.status == TaskStatus.BLOCKED.name || task.status == TaskStatus.CASCADED_BLOCKED.name

          if (isBlocked) {
            drawRoundRect(
              color = statusColor.copy(alpha = 0.22f),
              topLeft = Offset(r.left - 6f * scale, r.top - 6f * scale),
              size = Size(r.width + 12f * scale, r.height + 12f * scale),
              cornerRadius = CornerRadius(18f * scale, 18f * scale)
            )
          }

          drawRoundRect(
            color = Color(0xFF131A26),
            topLeft = r.topLeft,
            size = r.size,
            cornerRadius = CornerRadius(14f * scale, 14f * scale)
          )

          drawRoundRect(
            color = statusColor,
            topLeft = r.topLeft,
            size = r.size,
            cornerRadius = CornerRadius(14f * scale, 14f * scale),
            style = Stroke(width = if (item.node.isOnCriticalPath) 2.6f * scale else 1.5f * scale)
          )

          // Progress bar along bottom inside node
          val barPadding = 12f * scale
          val barY = r.bottom - 14f * scale
          val barW = (r.width - barPadding * 2).coerceAtLeast(10f)
          drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(r.left + barPadding, barY),
            size = Size(barW, 5f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale)
          )
          val fillW = barW * (item.node.rolledUpProgress / 100f).coerceIn(0f, 1f)
          if (fillW > 0f) {
            drawRoundRect(
              color = statusColor,
              topLeft = Offset(r.left + barPadding, barY),
              size = Size(fillW, 5f * scale),
              cornerRadius = CornerRadius(3f * scale, 3f * scale)
            )
          }

          // Node labels
          val keyText = "${task.taskKey} • ${item.node.rolledUpProgress}%"
          drawText(
            textMeasurer = textMeasurer,
            text = keyText,
            topLeft = Offset(r.left + 12f * scale, r.top + 10f * scale),
            style = TextStyle(
              color = statusColor,
              fontSize = (11f * scale).coerceIn(8f, 18f).sp,
              fontWeight = FontWeight.Bold
            )
          )

          val shortTitle = if (task.title.length > 24) task.title.take(22) + "…" else task.title
          drawText(
            textMeasurer = textMeasurer,
            text = shortTitle,
            topLeft = Offset(r.left + 12f * scale, r.top + 32f * scale),
            style = TextStyle(
              color = Color.White,
              fontSize = (12.5f * scale).coerceIn(9f, 20f).sp,
              fontWeight = FontWeight.SemiBold
            )
          )

          val ownerLabel = "${task.assigneeName.take(16)} (${task.status.replace("_", " ")})"
          drawText(
            textMeasurer = textMeasurer,
            text = ownerLabel,
            topLeft = Offset(r.left + 12f * scale, r.top + 56f * scale),
            style = TextStyle(
              color = Color(0xFF94A3B8),
              fontSize = (10f * scale).coerceIn(7f, 16f).sp
            )
          )
        }
      }

      // Floating Zoom & Reset Controls
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(12.dp)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { scale = (scale + 0.15f).coerceAtMost(1.8f) }) {
            Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
          }
          IconButton(onClick = { scale = (scale - 0.15f).coerceAtLeast(0.55f) }) {
            Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
          }
          IconButton(onClick = {
            scale = 1f
            panOffset = Offset(30f, 30f)
          }) {
            Icon(Icons.Default.CenterFocusStrong, contentDescription = "Reset View")
          }
        }
      }

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        modifier = Modifier
          .align(Alignment.TopStart)
          .padding(12.dp)
      ) {
        Text(
          text = "Drag to pan • Pinch to zoom • Tap any node to edit or cascade blockers",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
      }
    }
  }
}

@Composable
private fun ExpandableTreeRow(
  node: TreeTaskNode,
  isCollapsed: Boolean,
  onToggleCollapse: () -> Unit,
  onTaskClick: () -> Unit,
  onQuickAdvance: () -> Unit,
  onReportBlocker: () -> Unit
) {
  val task = node.task
  val statusColor = statusColorFor(task.status)
  val indentDp = (node.depth * 20).coerceAtMost(60).dp

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(start = indentDp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (node.depth > 0) {
      Box(
        modifier = Modifier
          .width(12.dp)
          .height(2.dp)
          .background(statusColor.copy(alpha = 0.6f))
      )
      Spacer(modifier = Modifier.width(6.dp))
    }

    Card(
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, statusColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
        .clickable { onTaskClick() }
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            if (node.children.isNotEmpty()) {
              IconButton(
                onClick = onToggleCollapse,
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = if (isCollapsed) Icons.Default.ChevronRight else Icons.Default.ExpandMore,
                  contentDescription = "Expand or Collapse Subtree",
                  tint = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.width(4.dp))
            }
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = statusColor.copy(alpha = 0.18f)
            ) {
              Text(
                text = task.taskKey,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = task.title,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }

          Text(
            text = "${node.rolledUpProgress}%",
            style = MaterialTheme.typography.labelLarge,
            color = statusColor
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = buildString {
              append("${task.assigneeName} • ${task.status.replace("_", " ")}")
              if (node.children.isNotEmpty()) {
                append(" • ${node.children.size} child tasks")
              }
              if (node.upstreamTasks.isNotEmpty()) {
                append(" • Prereqs: ${node.upstreamTasks.joinToString { it.taskKey }}")
              }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        AnimatedVisibility(visible = task.blockerReason.isNotBlank()) {
          Text(
            text = "⚠ ${task.blockerReason}",
            style = MaterialTheme.typography.labelSmall,
            color = CrimsonBlocker,
            modifier = Modifier.padding(top = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
          progress = { (node.rolledUpProgress / 100f).coerceIn(0f, 1f) },
          color = statusColor,
          trackColor = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          if (task.status != TaskStatus.BLOCKED.name) {
            TextButton(onClick = onReportBlocker) {
              Icon(Icons.Default.WarningAmber, contentDescription = null, tint = CrimsonBlocker, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Flag Blocker", color = CrimsonBlocker, style = MaterialTheme.typography.labelSmall)
            }
          }
          if (task.status != TaskStatus.DONE.name) {
            TextButton(onClick = onQuickAdvance) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(15.dp))
              Spacer(modifier = Modifier.width(2.dp))
              Text("Advance", style = MaterialTheme.typography.labelSmall)
            }
          }
        }
      }
    }
  }
}
