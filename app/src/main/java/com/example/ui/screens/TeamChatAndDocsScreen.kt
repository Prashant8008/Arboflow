package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DocumentCategory
import com.example.data.model.TeamDocument
import com.example.data.model.TeamMessage
import com.example.data.model.TeamTask
import com.example.ui.theme.CyanTelemetry
import com.example.ui.theme.EmeraldPulse
import com.example.ui.theme.ElectricIndigo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class HubSection(val label: String) {
  CHAT("Live Team Chat"),
  DOCS("Shared Documents")
}

@Composable
fun TeamChatAndDocsScreen(
  messages: List<TeamMessage>,
  documents: List<TeamDocument>,
  tasks: List<TeamTask>,
  currentUserEmail: String,
  currentUserDisplayName: String,
  onSendMessage: (text: String, linkedTaskKey: String, linkedDocId: String, linkedDocTitle: String) -> Unit,
  onShareDocument: (
    title: String,
    category: String,
    externalUrl: String,
    contentSummary: String,
    linkedTaskKey: String,
    announceInChat: Boolean
  ) -> Unit,
  onDeleteDocument: (TeamDocument) -> Unit,
  onPostMessage: (String) -> Unit
) {
  val context = LocalContext.current
  var activeSection by remember { mutableStateOf(HubSection.CHAT) }
  var selectedTaskKeyFilter by remember { mutableStateOf<String?>(null) }

  // Chat composer states
  var messageText by remember { mutableStateOf("") }
  var composerTaskKey by remember { mutableStateOf("") }
  var composerAttachedDoc by remember { mutableStateOf<TeamDocument?>(null) }
  var showAttachDocPicker by remember { mutableStateOf(false) }

  // Document creation & viewer dialogs
  var showShareDocDialog by remember { mutableStateOf(false) }
  var viewingDocument by remember { mutableStateOf<TeamDocument?>(null) }

  val timeFormatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.US) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    // Top Header Card + Section Switcher
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "TEAM CHAT & SHARED DOCUMENTS",
              style = MaterialTheme.typography.labelSmall,
              fontFamily = FontFamily.Monospace,
              color = CyanTelemetry
            )
            Text(
              text = "Workspace Collaboration Hub",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Connected as $currentUserDisplayName ${if (currentUserEmail.isNotBlank()) "($currentUserEmail)" else ""}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          OutlinedButton(
            onClick = { showShareDocDialog = true },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("open_share_document_button")
          ) {
            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Share Doc")
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          HubSection.entries.forEach { section ->
            val count = if (section == HubSection.CHAT) messages.size else documents.size
            FilterChip(
              selected = activeSection == section,
              onClick = { activeSection = section },
              label = { Text("${section.label} ($count)") },
              leadingIcon = {
                Icon(
                  imageVector = if (section == HubSection.CHAT) Icons.Default.Chat else Icons.Default.FolderShared,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
              },
              modifier = Modifier
                .weight(1f)
                .testTag("section_${section.name.lowercase()}")
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Task Filter Row
    if (tasks.isNotEmpty()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        FilterChip(
          selected = selectedTaskKeyFilter == null,
          onClick = { selectedTaskKeyFilter = null },
          label = { Text("All Workspace") }
        )
        tasks.forEach { task ->
          FilterChip(
            selected = selectedTaskKeyFilter == task.taskKey,
            onClick = {
              selectedTaskKeyFilter = if (selectedTaskKeyFilter == task.taskKey) null else task.taskKey
            },
            label = { Text("@${task.taskKey}") }
          )
        }
      }
      Spacer(modifier = Modifier.height(8.dp))
    }

    when (activeSection) {
      HubSection.CHAT -> {
        val filteredMessages = remember(messages, selectedTaskKeyFilter) {
          if (selectedTaskKeyFilter == null) {
            messages
          } else {
            messages.filter { it.linkedTaskKey.equals(selectedTaskKeyFilter, ignoreCase = true) }
          }
        }

        Column(modifier = Modifier.fillMaxSize()) {
          if (filteredMessages.isEmpty()) {
            Card(
              shape = RoundedCornerShape(16.dp),
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  modifier = Modifier.padding(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = "No messages in this channel yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Send a message below, tag a task (@ARB-101), or attach a shared document for your team.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          } else {
            LazyColumn(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              verticalArrangement = Arrangement.spacedBy(10.dp),
              contentPadding = PaddingValues(vertical = 6.dp)
            ) {
              items(filteredMessages, key = { it.id.ifBlank { it.hashCode().toString() } }) { msg ->
                val isCurrentUser = currentUserEmail.isNotBlank() &&
                  msg.senderEmail.equals(currentUserEmail, ignoreCase = true)
                val timestampText = msg.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Just now"

                Card(
                  shape = RoundedCornerShape(16.dp),
                  colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentUser) {
                      ElectricIndigo.copy(alpha = 0.14f)
                    } else {
                      MaterialTheme.colorScheme.surface
                    }
                  ),
                  border = BorderStroke(
                    1.dp,
                    if (isCurrentUser) ElectricIndigo.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                          shape = CircleShape,
                          color = if (msg.senderRole.contains("Senior", ignoreCase = true)) {
                            ElectricIndigo.copy(alpha = 0.2f)
                          } else {
                            CyanTelemetry.copy(alpha = 0.2f)
                          }
                        ) {
                          Text(
                            text = msg.senderRole.ifBlank { "Member" },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.senderRole.contains("Senior", ignoreCase = true)) ElectricIndigo else CyanTelemetry,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                          )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                          Text(
                            text = msg.senderName,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                          )
                          if (msg.senderEmail.isNotBlank()) {
                            Text(
                              text = msg.senderEmail,
                              style = MaterialTheme.typography.labelSmall,
                              fontFamily = FontFamily.Monospace,
                              color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                          }
                        }
                      }

                      Text(
                        text = timestampText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                      text = msg.text,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface
                    )

                    if (msg.linkedTaskKey.isNotBlank() || msg.linkedDocTitle.isNotBlank()) {
                      Spacer(modifier = Modifier.height(8.dp))
                      Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        if (msg.linkedTaskKey.isNotBlank()) {
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                          ) {
                            Text(
                              text = "Task: ${msg.linkedTaskKey}",
                              style = MaterialTheme.typography.labelSmall,
                              fontFamily = FontFamily.Monospace,
                              color = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                          }
                        }

                        if (msg.linkedDocTitle.isNotBlank()) {
                          val matchedDoc = documents.find {
                            it.id == msg.linkedDocId || it.title == msg.linkedDocTitle
                          }
                          Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EmeraldPulse.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, EmeraldPulse.copy(alpha = 0.4f)),
                            modifier = Modifier.clickable {
                              if (matchedDoc != null) {
                                viewingDocument = matchedDoc
                              } else {
                                activeSection = HubSection.DOCS
                              }
                            }
                          ) {
                            Row(
                              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                              verticalAlignment = Alignment.CenterVertically
                            ) {
                              Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = EmeraldPulse,
                                modifier = Modifier.size(14.dp)
                              )
                              Spacer(modifier = Modifier.width(4.dp))
                              Text(
                                text = msg.linkedDocTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPulse,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                              )
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Composer Bar
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(10.dp)) {
              // Optional Tag Task or Attached Doc strip
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedButton(
                  onClick = { showAttachDocPicker = true },
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = composerAttachedDoc?.title?.take(22) ?: "Attach Doc",
                    style = MaterialTheme.typography.labelSmall
                  )
                }

                if (composerAttachedDoc != null) {
                  IconButton(
                    onClick = { composerAttachedDoc = null },
                    modifier = Modifier.size(24.dp)
                  ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear attachment", modifier = Modifier.size(14.dp))
                  }
                }

                tasks.take(6).forEach { t ->
                  FilterChip(
                    selected = composerTaskKey == t.taskKey,
                    onClick = {
                      composerTaskKey = if (composerTaskKey == t.taskKey) "" else t.taskKey
                    },
                    label = { Text("@${t.taskKey}", style = MaterialTheme.typography.labelSmall) }
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                OutlinedTextField(
                  value = messageText,
                  onValueChange = { messageText = it },
                  placeholder = { Text("Message team in shared Gmail workspace...") },
                  modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                  singleLine = false,
                  maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                  onClick = {
                    if (messageText.isNotBlank()) {
                      onSendMessage(
                        messageText,
                        composerTaskKey,
                        composerAttachedDoc?.id.orEmpty(),
                        composerAttachedDoc?.title.orEmpty()
                      )
                      messageText = ""
                      composerAttachedDoc = null
                    }
                  },
                  enabled = messageText.isNotBlank(),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("send_chat_message_button")
                ) {
                  Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send Message")
                }
              }
            }
          }
        }
      }

      HubSection.DOCS -> {
        val filteredDocs = remember(documents, selectedTaskKeyFilter) {
          if (selectedTaskKeyFilter == null) {
            documents
          } else {
            documents.filter { it.linkedTaskKey.equals(selectedTaskKeyFilter, ignoreCase = true) }
          }
        }

        if (filteredDocs.isEmpty()) {
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxSize()
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.FolderShared,
                  contentDescription = null,
                  tint = CyanTelemetry,
                  modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                  text = "No shared documents yet",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Share architecture specs, Google Drive docs, Figma designs, or import files from your device.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(onClick = { showShareDocDialog = true }) {
                  Icon(Icons.Default.UploadFile, contentDescription = null)
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("Share First Document")
                }
              }
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
          ) {
            items(filteredDocs, key = { it.id.ifBlank { it.hashCode().toString() } }) { doc ->
              val catLabel = runCatching { DocumentCategory.valueOf(doc.category).label }.getOrDefault(doc.category)
              val dateStr = doc.createdAt?.toDate()?.let { timeFormatter.format(it) } ?: "Synced"

              Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { viewingDocument = doc }
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                      Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CyanTelemetry.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = CyanTelemetry
                          )
                        }
                      }
                      Spacer(modifier = Modifier.width(10.dp))
                      Column {
                        Text(
                          text = doc.title,
                          style = MaterialTheme.typography.titleSmall,
                          fontWeight = FontWeight.Bold
                        )
                        Text(
                          text = "$catLabel • Shared by ${doc.uploadedByName} ${if (doc.uploadedByEmail.isNotBlank()) "(${doc.uploadedByEmail})" else ""}",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }

                    if (doc.linkedTaskKey.isNotBlank()) {
                      Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                      ) {
                        Text(
                          text = doc.linkedTaskKey,
                          style = MaterialTheme.typography.labelSmall,
                          fontFamily = FontFamily.Monospace,
                          color = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                      }
                    }
                  }

                  if (doc.contentSummary.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                      text = doc.contentSummary,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis
                    )
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = dateStr,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      if (doc.externalUrl.isNotBlank()) {
                        OutlinedButton(
                          onClick = {
                            runCatching {
                              val intent = Intent(Intent.ACTION_VIEW, Uri.parse(doc.externalUrl))
                              context.startActivity(intent)
                            }.onFailure {
                              onPostMessage("Invalid URL: ${doc.externalUrl}")
                            }
                          },
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                          shape = RoundedCornerShape(8.dp)
                        ) {
                          Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                          Spacer(modifier = Modifier.width(4.dp))
                          Text("Open Link", style = MaterialTheme.typography.labelSmall)
                        }
                      }

                      OutlinedButton(
                        onClick = {
                          val shareBody = buildString {
                            appendLine("ArborFlow Shared Document: ${doc.title}")
                            if (doc.linkedTaskKey.isNotBlank()) appendLine("Task: ${doc.linkedTaskKey}")
                            if (doc.externalUrl.isNotBlank()) appendLine("Link: ${doc.externalUrl}")
                            if (doc.contentSummary.isNotBlank()) {
                              appendLine()
                              appendLine(doc.contentSummary)
                            }
                          }
                          val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, doc.title)
                            putExtra(Intent.EXTRA_TEXT, shareBody)
                          }
                          context.startActivity(Intent.createChooser(sendIntent, "Share Document"))
                        },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                      ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelSmall)
                      }

                      IconButton(
                        onClick = { onDeleteDocument(doc) },
                        modifier = Modifier.size(32.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.DeleteOutline,
                          contentDescription = "Delete Document",
                          tint = MaterialTheme.colorScheme.onSurfaceVariant,
                          modifier = Modifier.size(18.dp)
                        )
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Attach Existing Document Picker Dialog for Chat
  if (showAttachDocPicker) {
    AlertDialog(
      onDismissRequest = { showAttachDocPicker = false },
      title = { Text("Attach Shared Document to Message", fontWeight = FontWeight.Bold) },
      text = {
        if (documents.isEmpty()) {
          Text("No shared documents in workspace yet. Tap 'Share New Document' to upload one first.")
        } else {
          Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            documents.forEach { doc ->
              Card(
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    composerAttachedDoc = doc
                    if (composerTaskKey.isBlank() && doc.linkedTaskKey.isNotBlank()) {
                      composerTaskKey = doc.linkedTaskKey
                    }
                    showAttachDocPicker = false
                  }
              ) {
                Column(modifier = Modifier.padding(10.dp)) {
                  Text(doc.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                  Text(
                    text = "${doc.category} • Shared by ${doc.uploadedByName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            showAttachDocPicker = false
            showShareDocDialog = true
          }
        ) {
          Text("+ Share New Document")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAttachDocPicker = false }) {
          Text("Close")
        }
      }
    )
  }

  // Share Document / File Upload Dialog
  if (showShareDocDialog) {
    ShareTeamDocumentDialog(
      tasks = tasks,
      onDismiss = { showShareDocDialog = false },
      onShare = { title, category, url, content, taskKey, announce ->
        onShareDocument(title, category, url, content, taskKey, announce)
        showShareDocDialog = false
      }
    )
  }

  // Full Document Reader Dialog
  if (viewingDocument != null) {
    val doc = viewingDocument!!
    AlertDialog(
      onDismissRequest = { viewingDocument = null },
      title = {
        Column {
          Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          Text(
            text = "Shared by ${doc.uploadedByName} ${if (doc.uploadedByEmail.isNotBlank()) "(${doc.uploadedByEmail})" else ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
          )
        }
      },
      text = {
        Column(
          modifier = Modifier.verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          if (doc.linkedTaskKey.isNotBlank()) {
            Text(
              text = "Linked Task: ${doc.linkedTaskKey}",
              style = MaterialTheme.typography.labelMedium,
              fontFamily = FontFamily.Monospace,
              color = MaterialTheme.colorScheme.primary
            )
          }
          if (doc.externalUrl.isNotBlank()) {
            Surface(
              shape = RoundedCornerShape(10.dp),
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(doc.externalUrl)))
                  }
                }
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Link, contentDescription = null, tint = CyanTelemetry, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = doc.externalUrl,
                  style = MaterialTheme.typography.labelSmall,
                  color = CyanTelemetry,
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
          }
          if (doc.contentSummary.isNotBlank()) {
            Text(
              text = doc.contentSummary,
              style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            composerAttachedDoc = doc
            if (doc.linkedTaskKey.isNotBlank()) composerTaskKey = doc.linkedTaskKey
            viewingDocument = null
            activeSection = HubSection.CHAT
          }
        ) {
          Text("Discuss in Chat")
        }
      },
      dismissButton = {
        TextButton(onClick = { viewingDocument = null }) {
          Text("Close")
        }
      }
    )
  }
}

@Composable
private fun ShareTeamDocumentDialog(
  tasks: List<TeamTask>,
  onDismiss: () -> Unit,
  onShare: (
    title: String,
    category: String,
    externalUrl: String,
    contentSummary: String,
    linkedTaskKey: String,
    announceInChat: Boolean
  ) -> Unit
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var category by remember { mutableStateOf(DocumentCategory.SPEC.name) }
  var externalUrl by remember { mutableStateOf("") }
  var contentSummary by remember { mutableStateOf("") }
  var linkedTaskKey by remember { mutableStateOf(tasks.firstOrNull()?.taskKey.orEmpty()) }
  var announceInChat by remember { mutableStateOf(true) }

  // Allow picking a real file from the Android device to populate title & content preview
  val filePickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri: Uri? ->
    if (uri != null) {
      runCatching {
        var resolvedName = ""
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
          val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
          if (nameIdx >= 0 && cursor.moveToFirst()) {
            resolvedName = cursor.getString(nameIdx).orEmpty()
          }
        }
        if (resolvedName.isNotBlank() && title.isBlank()) {
          title = resolvedName
        }
        val textPreview = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
          reader.readText().take(1800)
        }.orEmpty()
        if (textPreview.isNotBlank()) {
          contentSummary = textPreview
        } else if (contentSummary.isBlank()) {
          contentSummary = "Attached device file: ${resolvedName.ifBlank { uri.lastPathSegment.orEmpty() }}"
        }
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Share Document with Team", fontWeight = FontWeight.Bold) },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedButton(
          onClick = { filePickerLauncher.launch("*/*") },
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Pick File from Device (Text / Spec / Doc)")
        }

        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("Document Title / File Name") },
          placeholder = { Text("e.g., ARB-102 API Contract & Spec") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_title_input")
        )

        Text("Document Type", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          DocumentCategory.entries.forEach { cat ->
            FilterChip(
              selected = category == cat.name,
              onClick = { category = cat.name },
              label = { Text(cat.label) }
            )
          }
        }

        OutlinedTextField(
          value = externalUrl,
          onValueChange = { externalUrl = it },
          label = { Text("Google Drive / Docs / Figma Link (Optional)") },
          placeholder = { Text("https://docs.google.com/document/d/...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        if (tasks.isNotEmpty()) {
          Text("Link to Task Node", style = MaterialTheme.typography.labelMedium)
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            FilterChip(
              selected = linkedTaskKey.isBlank(),
              onClick = { linkedTaskKey = "" },
              label = { Text("General") }
            )
            tasks.forEach { task ->
              FilterChip(
                selected = linkedTaskKey == task.taskKey,
                onClick = { linkedTaskKey = task.taskKey },
                label = { Text(task.taskKey) }
              )
            }
          }
        }

        OutlinedTextField(
          value = contentSummary,
          onValueChange = { contentSummary = it },
          label = { Text("Document Content / Specification Notes") },
          placeholder = { Text("Paste specification details, checklist, or file contents so team members can read it directly inside the app...") },
          minLines = 4,
          maxLines = 7,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("doc_content_input")
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
          Checkbox(
            checked = announceInChat,
            onCheckedChange = { announceInChat = it }
          )
          Text(
            text = "Post notification in Live Team Chat",
            style = MaterialTheme.typography.bodySmall
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (title.isNotBlank()) {
            onShare(
              title,
              category,
              externalUrl,
              contentSummary.ifBlank { "Shared with team workspace." },
              linkedTaskKey,
              announceInChat
            )
          }
        },
        enabled = title.isNotBlank(),
        modifier = Modifier.testTag("confirm_share_document_button")
      ) {
        Text("Share with Team")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}
