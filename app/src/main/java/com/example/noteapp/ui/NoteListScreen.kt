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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showSortSheet by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Column {
                        Text(
                            "Ghi chú",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$noteCount ghi chú",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Sort button
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sắp xếp")
                    }
                    // Trash with badge
                    IconButton(onClick = onOpenTrash) {
                        if (trashCount > 0) {
                            BadgedBox(badge = { Badge { Text("$trashCount") } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Thùng rác")
                            }
                        } else {
                            Icon(Icons.Default.Delete, contentDescription = "Thùng rác")
                        }
                    }
                    // Overflow menu (3 chấm) gom các action phụ
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Thêm tuỳ chọn")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Quản lý category") },
                                leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                                onClick = { showOverflowMenu = false; onManageCategories() }
                            )
                            DropdownMenuItem(
                                text = { Text("Export / Import") },
                                leadingIcon = { Icon(Icons.Default.ImportExport, contentDescription = null) },
                                onClick = { showOverflowMenu = false; onExportImport() }
                            )
                            DropdownMenuItem(
                                text = { Text("Bảo mật ghi chú") },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) },
                                onClick = { showOverflowMenu = false; onOpenSecuritySettings() }
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNote,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm ghi chú", tint = Color.White)
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Thanh tìm kiếm ─────────────────────────────────────────────
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    "Tìm theo tên, nội dung, tag...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                }
            }

            // ── Chip lọc category cuộn ngang ────────────────────────────────
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryFilterChip(
                        label = ALL_CATEGORIES,
                        count = categoryCounts[ALL_CATEGORIES] ?: 0,
                        selected = selectedCategory == ALL_CATEGORIES,
                        onClick = { viewModel.onCategorySelected(ALL_CATEGORIES) }
                    )
                }
                items(categories) { cat ->
                    CategoryFilterChip(
                        label = cat.name,
                        count = categoryCounts[cat.name] ?: 0,
                        selected = selectedCategory == cat.name,
                        onClick = { viewModel.onCategorySelected(cat.name) }
                    )
                }
            }

            // ── Sort indicator ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${pagedNotes.itemCount} kết quả",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = { showSortSheet = true }) {
                    Icon(
                        Icons.Default.Sort,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        sortOption.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // ── Lưới ghi chú ────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
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
                        Box(
                            Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
                    }
                }

                if (pagedNotes.itemCount == 0 && pagedNotes.loadState.refresh !is LoadState.Loading) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(48.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📝", fontSize = 40.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (searchQuery.isNotBlank()) "Không tìm thấy ghi chú phù hợp"
                                else "Chưa có ghi chú nào\nBấm + để tạo ghi chú đầu tiên",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Khoảng trống phía dưới để FAB không che ghi chú cuối
                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }

    // ── Bottom Sheet chọn kiểu sắp xếp ─────────────────────────────────────
    if (showSortSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Sắp xếp ghi chú",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
                HorizontalDivider()
                SortOption.values().forEach { option ->
                    val selected = option == sortOption
                    DropdownMenuItem(
                        text = {
                            Text(
                                option.label,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        trailingIcon = {
                            if (selected) Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {
                            viewModel.onSortOptionSelected(option)
                            showSortSheet = false
                        },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

// ── Chip lọc category với style riêng, hiển thị số đếm ────────────────────
@Composable
private fun CategoryFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(4.dp))
                Surface(
                    shape = CircleShape,
                    color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "$count",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

// ── Card ghi chú với tiêu đề xuống dòng tối đa 2 dòng ─────────────────────
@Composable
private fun NoteCard(note: Note, onClick: () -> Unit) {
    val bgColor = runCatching { Color(android.graphics.Color.parseColor(note.bgColorHex)) }
        .getOrDefault(Color(0xFFFFF9C4))
    val textColor = runCatching { Color(android.graphics.Color.parseColor(note.textColorHex)) }
        .getOrDefault(Color.Black)
    val onImage = note.bgImageUri != null

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = if (!onImage) bgColor else Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (!onImage) bgColor else Color.Transparent)
        ) {
            // ── Ảnh nền ─────────────────────────────────────────────────
            if (onImage) {
                AsyncImage(
                    model = note.bgImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay cho chữ dễ đọc
                Box(
                    Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(0.05f), Color.Black.copy(0.45f))
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ── Badge icons hàng trên ────────────────────────────────
                if (note.isPinned || note.reminderAt != null || note.isLocked) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (note.isPinned) StatusIcon(Icons.Default.PushPin, "Ghim", onImage, textColor)
                        if (note.reminderAt != null) StatusIcon(Icons.Default.Alarm, "Nhắc nhở", onImage, textColor)
                        if (note.isLocked) StatusIcon(Icons.Default.Lock, "Khoá", onImage, textColor)
                    }
                }

                // ── Tiêu đề: tối đa 2 dòng, xuống dòng tự nhiên ─────────
                Text(
                    text = if (note.isLocked) "Ghi chú bí mật"
                           else note.title.ifBlank { "(Không tiêu đề)" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (onImage) Color.White else textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                // ── Nội dung preview ─────────────────────────────────────
                if (note.isLocked) {
                    Text(
                        "Chạm để nhập mật khẩu",
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (onImage) Color.White else textColor).copy(alpha = 0.65f),
                        maxLines = 1
                    )
                } else if (note.content.isNotBlank()) {
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = (if (onImage) Color.White else textColor).copy(alpha = 0.80f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                // ── Tags ─────────────────────────────────────────────────
                if (!note.isLocked && note.tagList.isNotEmpty()) {
                    Text(
                        note.tagList.take(3).joinToString(" ") { "#$it" },
                        style = MaterialTheme.typography.labelSmall,
                        color = (if (onImage) Color.White else textColor).copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ── Category chip nhỏ ────────────────────────────────────
                if (!note.isLocked) {
                    Spacer(Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = (if (onImage) Color.White else textColor).copy(alpha = 0.15f)
                    ) {
                        Text(
                            note.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (onImage) Color.White else textColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    onImage: Boolean,
    textColor: Color
) {
    Icon(
        icon,
        contentDescription = desc,
        tint = if (onImage) Color.White.copy(0.9f) else textColor.copy(0.7f),
        modifier = Modifier.size(13.dp)
    )
}
