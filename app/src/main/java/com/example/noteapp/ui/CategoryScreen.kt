package com.example.noteapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.noteapp.data.ALL_CATEGORIES

private const val MAX_CATEGORY_LENGTH = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(viewModel: NoteViewModel, onBack: () -> Unit) {
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    var newCategory by remember { mutableStateOf("") }

    val trimmed = newCategory.trim()
    // Trùng tên (không phân biệt hoa/thường) hoặc trùng chữ "Tất cả" dùng cho bộ lọc.
    val isDuplicate = trimmed.isNotEmpty() && (
        trimmed.equals(ALL_CATEGORIES, ignoreCase = true) ||
            categories.any { it.name.equals(trimmed, ignoreCase = true) }
        )
    val canSubmit = trimmed.isNotEmpty() && !isDuplicate

    fun submit() {
        if (canSubmit) {
            viewModel.addCategory(trimmed, "#90CAF9")
            newCategory = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Category", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // ── Khối thêm category mới ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Thêm category mới",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    ModernTextField(
                        value = newCategory,
                        onValueChange = { if (it.length <= MAX_CATEGORY_LENGTH) newCategory = it },
                        label = "Tên category",
                        placeholder = "Ví dụ: Công việc, Học tập...",
                        leading = { Icon(Icons.Default.Category, contentDescription = null) },
                        showClear = true,
                        isError = isDuplicate,
                        supportingText = if (isDuplicate) "Category này đã tồn tại"
                                         else "${newCategory.length}/$MAX_CATEGORY_LENGTH ký tự",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() })
                    )

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { submit() },
                        enabled = canSubmit,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Thêm category")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Danh sách (${categories.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Spacer(Modifier.height(8.dp))

            if (categories.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Chưa có category nào.\nHãy thêm category đầu tiên ở trên.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val count = categoryCounts[cat.name] ?: 0
                        val dotColor = runCatching { Color(android.graphics.Color.parseColor(cat.colorHex)) }
                            .getOrDefault(Color(0xFF90CAF9))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (count == 0) "Chưa có ghi chú" else "$count ghi chú",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Xoá category",
                                        tint = MaterialTheme.colorScheme.error
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
