package com.example.noteapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.example.noteapp.data.ALL_CATEGORIES
import com.example.noteapp.data.Note
import com.example.noteapp.data.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onManageCategories: () -> Unit,
    onExportImport: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenSecuritySettings: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val noteCount by viewModel.noteCount.collectAsState()
    val trashCount by viewModel.trashCount.collectAsState()
    val pagedNotes: LazyPagingItems<Note> = viewModel.notesPaged.collectAsLazyPagingItems()
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghi chú của tôi ($noteCount)", fontWeight = FontWeight.Bold) },
                actions = {
                    // ── Sắp xếp: dropdown menu chọn 1 trong 6 kiểu sort ─────
                    Box {
                        IconButton(onClick = { sortMenuExpanded = true }) {
                            Icon(Icons.Default.SwapVert, contentDescription = "Sắp xếp")
                        }
                        DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                            SortOption.values().forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    trailingIcon = {
                                        if (option == sortOption) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.onSortOptionSelected(option)
                                        sortMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // ── Thùng rác, hiện badge số lượng nếu có ghi chú đã xoá ─
                    IconButton(onClick = onOpenTrash) {
                        if (trashCount > 0) {
                            BadgedBox(badge = { Badge { Text(trashCount.toString()) } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Thùng rác")
                            }
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Thùng rác")
                        }
                    }

                    IconButton(onClick = onExportImport) {
                        Icon(Icons.Default.ImportExport, contentDescription = "Export / Import")
                    }
                    IconButton(onClick = onOpenSecuritySettings) {
                        Icon(Icons.Default.Security, contentDescription = "Bảo mật ghi chú")
                    }
                    IconButton(onClick = onManageCategories) {
                        Icon(Icons.Default.Add, contentDescription = "Quản lý category")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNote) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ghi chú")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Tìm kiếm theo tên / nội dung / tag ─────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Tìm theo tên, nội dung, tag...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // ── Bộ lọc category dạng chip cuộn ngang ───────────────────────
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == ALL_CATEGORIES,
                        onClick = { viewModel.onCategorySelected(ALL_CATEGORIES) },
                        label = { Text("$ALL_CATEGORIES (${categoryCounts[ALL_CATEGORIES] ?: 0})") }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { viewModel.onCategorySelected(cat.name) },
                        label = { Text("${cat.name} (${categoryCounts[cat.name] ?: 0})") }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Lưới ghi chú, tự động phân trang khi cuộn (Paging3) ────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pagedNotes.itemCount) { index ->
                    val note = pagedNotes[index]
                    if (note != null) {
                        NoteCard(note = note, onClick = { onOpenNote(note.id) })
                    }
                }

                if (pagedNotes.loadState.append is LoadState.Loading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                if (pagedNotes.itemCount == 0 && pagedNotes.loadState.refresh !is LoadState.Loading) {
                    item(span = { GridItemSpan(2) }) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có ghi chú nào phù hợp.", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(note.bgColorHex)) }
        .getOrDefault(Color(0xFFFFF9C4))
    val textColor = runCatching { Color(android.graphics.Color.parseColor(note.textColorHex)) }
        .getOrDefault(Color.Black)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .background(if (note.bgImageUri == null) bgColor else Color.Transparent)
        ) {
            if (note.bgImageUri != null) {
                AsyncImage(
                    model = note.bgImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Ghim",
                            tint = if (note.bgImageUri != null) Color.White else textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (note.reminderAt != null) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = "Có nhắc nhở",
                            tint = if (note.bgImageUri != null) Color.White else textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    if (note.isLocked) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Ghi chú bí mật",
                            tint = if (note.bgImageUri != null) Color.White else textColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        // Ghi chú bí mật: KHÔNG hiện title thật ở danh sách,
                        // kể cả tiêu đề cũng có thể chứa thông tin nhạy cảm.
                        if (note.isLocked) "Ghi chú bí mật" else note.title.ifBlank { "(Không tiêu đề)" },
                        color = if (note.bgImageUri != null) Color.White else textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    // Ẩn toàn bộ nội dung thật, kể cả bản xem trước — chỉ hiện
                    // sau khi người dùng nhập đúng PIN trong NoteEditScreen.
                    if (note.isLocked) "Chạm để nhập mật khẩu và xem nội dung" else note.content,
                    color = (if (note.bgImageUri != null) Color.White else textColor).copy(alpha = 0.85f),
                    maxLines = 4
                )
                if (!note.isLocked && note.tagList.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        note.tagList.joinToString(" ") { "#$it" },
                        color = (if (note.bgImageUri != null) Color.White else textColor).copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (!note.isLocked) {
                    AssistChip(onClick = {}, label = { Text(note.category, style = MaterialTheme.typography.bodySmall) })
                }
            }
        }
    }
}
