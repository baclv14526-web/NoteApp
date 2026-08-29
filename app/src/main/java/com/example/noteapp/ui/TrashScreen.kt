package com.example.noteapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.noteapp.data.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(viewModel: NoteViewModel, onBack: () -> Unit) {
    val trashedNotes by viewModel.trashedNotesPaged.collectAsState(initial = emptyList())
    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thùng rác (${trashedNotes.size})", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (trashedNotes.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Dọn sạch thùng rác")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (trashedNotes.isEmpty()) {
            Box(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 8.dp),
                        tint = Color.Gray
                    )
                    Text("Thùng rác trống", color = Color.Gray)
                }
            }
        } else {
            Column(Modifier.padding(padding).fillMaxSize()) {
                Text(
                    "Ghi chú trong thùng rác sẽ tự động bị xoá vĩnh viễn sau 30 ngày.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashedNotes, key = { it.id }) { note ->
                        TrashNoteCard(
                            note = note,
                            deletedDateText = note.deletedAt?.let { dateFormat.format(Date(it)) } ?: "",
                            onRestore = { viewModel.restoreFromTrash(note) },
                            onDeleteForever = { noteToDeletePermanently = note }
                        )
                    }
                }
            }
        }
    }

    // ── Xác nhận xoá vĩnh viễn 1 ghi chú ────────────────────────────────────
    noteToDeletePermanently?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text("Xoá vĩnh viễn?") },
            text = { Text("Ghi chú \"${if (note.isLocked) "Ghi chú bí mật" else note.title.ifBlank { "(Không tiêu đề)" }}\" sẽ bị xoá vĩnh viễn và không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePermanently(note)
                    noteToDeletePermanently = null
                }) { Text("Xoá vĩnh viễn", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeletePermanently = null }) { Text("Huỷ") }
            }
        )
    }

    // ── Xác nhận dọn sạch toàn bộ thùng rác ─────────────────────────────────
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Dọn sạch thùng rác?") },
            text = { Text("Toàn bộ ${trashedNotes.size} ghi chú trong thùng rác sẽ bị xoá vĩnh viễn và không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showEmptyTrashDialog = false
                }) { Text("Xoá tất cả", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
private fun TrashNoteCard(
    note: Note,
    deletedDateText: String,
    onRestore: () -> Unit,
    onDeleteForever: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (note.isLocked) "Ghi chú bí mật" else note.title.ifBlank { "(Không tiêu đề)" },
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    if (note.isLocked) "Nội dung đã được khoá" else note.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deletedDateText.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Đã xoá: $deletedDateText",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            IconButton(onClick = onRestore) {
                Icon(Icons.Default.Restore, contentDescription = "Khôi phục")
            }
            IconButton(onClick = onDeleteForever) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Xoá vĩnh viễn", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
