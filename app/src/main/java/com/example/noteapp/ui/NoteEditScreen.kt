package com.example.noteapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.noteapp.data.Note
import com.example.noteapp.util.PinManager
import com.example.noteapp.util.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val bgColorPalette = listOf(
    "#FFF9C4", "#FFCCBC", "#C8E6C9", "#B3E5FC", "#D1C4E9", "#F8BBD0", "#FFFFFF", "#212121"
)
private val textColorPalette = listOf("#000000", "#FFFFFF", "#5D4037", "#1A237E", "#B71C1C", "#1B5E20")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    viewModel: NoteViewModel,
    noteId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var bgColorHex by remember { mutableStateOf(bgColorPalette[0]) }
    var textColorHex by remember { mutableStateOf(textColorPalette[0]) }
    var bgImageUri by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("Chung") }
    var tagsText by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var reminderAt by remember { mutableStateOf<Long?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var existingNote by remember { mutableStateOf<Note?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    // ── Gate bảo vệ ghi chú bí mật ──────────────────────────────────────────
    // Nếu ghi chú đang mở đã bị khoá (isLocked = true trong DB), người dùng
    // phải nhập đúng PIN trước khi thấy được nội dung thật. isUnlocked chỉ
    // sống trong phiên hiện tại của Composable này — rời màn hình rồi quay
    // lại sẽ phải nhập PIN lại từ đầu (không cache trạng thái mở khoá).
    var noteRequiresPinCheck by remember { mutableStateOf<Boolean?>(null) }
    var isUnlockedThisSession by remember { mutableStateOf(false) }
    var showSetupPinScreen by remember { mutableStateOf(false) }
    var showVerifyPinScreen by remember { mutableStateOf(false) }
    var showForgotPinScreen by remember { mutableStateOf(false) }
    // Khi bấm khoá 1 note mà app chưa từng đặt PIN, cần đặt PIN xong rồi mới
    // thực sự khoá note — cờ này đánh dấu "đang chờ đặt PIN xong để khoá".
    var pendingLockAfterPinSetup by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                existingNote = note
                title = note.title
                content = note.content
                bgColorHex = note.bgColorHex
                textColorHex = note.textColorHex
                bgImageUri = note.bgImageUri
                category = note.category
                tagsText = note.tags
                isPinned = note.isPinned
                reminderAt = note.reminderAt
                isLocked = note.isLocked
                // Ghi chú đã khoá thì phải xác thực PIN trước khi hiển thị nội
                // dung thật — đặt cờ để UI render màn VerifyPinScreen thay vì
                // form sửa ghi chú bình thường.
                noteRequiresPinCheck = note.isLocked
                if (note.isLocked) showVerifyPinScreen = true
            }
        } else {
            // Ghi chú mới tạo, không cần qua bước xác thực PIN nào cả.
            noteRequiresPinCheck = false
        }
    }

    // ── Màn hình đặt PIN lần đầu (khi bấm khoá note nhưng chưa từng có PIN) ──
    if (showSetupPinScreen) {
        SetupPinScreen(
            onBack = {
                showSetupPinScreen = false
                pendingLockAfterPinSetup = false
            },
            onPinSet = {
                showSetupPinScreen = false
                if (pendingLockAfterPinSetup) {
                    isLocked = true
                    pendingLockAfterPinSetup = false
                    Toast.makeText(context, "Ghi chú sẽ được khoá sau khi lưu", Toast.LENGTH_SHORT).show()
                }
            }
        )
        return
    }

    // ── Màn hình xác thực PIN (khi mở 1 ghi chú đã bị khoá) ──────────────────
    if (showVerifyPinScreen) {
        VerifyPinScreen(
            onBack = onBack,
            onSuccess = {
                showVerifyPinScreen = false
                isUnlockedThisSession = true
            },
            onForgotPin = {
                showVerifyPinScreen = false
                showForgotPinScreen = true
            }
        )
        return
    }

    // ── Màn hình quên PIN (đặt lại PIN qua câu hỏi bảo mật) ──────────────────
    if (showForgotPinScreen) {
        ForgotPinScreen(
            onBack = {
                showForgotPinScreen = false
                showVerifyPinScreen = true
            },
            onPinReset = {
                showForgotPinScreen = false
                isUnlockedThisSession = true
            }
        )
        return
    }

    // Ghi chú đã khoá nhưng chưa xác thực xong trong phiên này — không render
    // form sửa ghi chú (chặn nội dung bí mật khỏi bị hiện ra dù chỉ 1 khung
    // hình trước khi VerifyPinScreen kịp hiển thị).
    if (noteRequiresPinCheck == true && !isUnlockedThisSession) {
        return
    }

    // Chọn ảnh .png / .jpg / .jpeg từ thư viện để làm nền ghi chú
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            bgImageUri = uri.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "Ghi chú mới" else "Chỉnh sửa ghi chú") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (!isLocked) {
                            // Đang mở khoá -> muốn khoá lại: cần PIN đã được đặt trước.
                            if (!PinManager.isPinSet(context)) {
                                pendingLockAfterPinSetup = true
                                showSetupPinScreen = true
                            } else {
                                isLocked = true
                                Toast.makeText(context, "Ghi chú sẽ được khoá sau khi lưu", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Đang khoá -> mở khoá: không cần nhập lại PIN vì người
                            // dùng đã xác thực để vào được màn hình sửa này rồi.
                            isLocked = false
                        }
                    }) {
                        Icon(
                            if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = if (isLocked) "Ghi chú bí mật" else "Khoá ghi chú",
                            tint = if (isLocked) MaterialTheme.colorScheme.error else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Ghim",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    if (existingNote != null) {
                        IconButton(onClick = {
                            existingNote?.let {
                                viewModel.moveToTrash(it)
                                ReminderScheduler.cancel(context, it.id)
                            }
                            onBack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Chuyển vào thùng rác")
                        }
                    }
                    TextButton(onClick = {
                        // Phòng thủ 2 lớp: ghi chú bí mật không được vừa khoá vừa
                        // ghim (widget đã tự loại note khoá ở tầng SQL, nhưng ép
                        // isPinned = false ở đây để tránh mọi rủi ro rò rỉ khác).
                        val effectivePinned = if (isLocked) false else isPinned
                        // Ghi chú bí mật không đặt nhắc nhở — nội dung note sẽ
                        // hiện ra trong notification, làm mất tác dụng khoá.
                        val effectiveReminder = if (isLocked) null else reminderAt

                        val note = (existingNote ?: Note(title = "", content = "")).copy(
                            title = title,
                            content = content,
                            bgColorHex = bgColorHex,
                            textColorHex = textColorHex,
                            bgImageUri = bgImageUri,
                            category = category,
                            tags = tagsText,
                            isPinned = effectivePinned,
                            reminderAt = effectiveReminder,
                            isLocked = isLocked
                        )
                        viewModel.saveNote(note) { savedId ->
                            // Đặt/huỷ alarm SAU KHI lưu xong vì ghi chú mới cần
                            // id thật do Room sinh ra để làm requestCode cho
                            // AlarmManager — id = 0 tạm thời lúc chưa lưu sẽ
                            // đè lẫn alarm của nhau nếu dùng trực tiếp.
                            // Dùng effectiveReminder (không phải reminderAt) vì
                            // ghi chú bí mật đã bị ép reminderAt = null lúc lưu.
                            if (effectiveReminder != null) {
                                val ok = ReminderScheduler.schedule(
                                    context = context,
                                    noteId = savedId,
                                    title = title.ifBlank { "Nhắc nhở ghi chú" },
                                    content = content,
                                    triggerAtMillis = effectiveReminder
                                )
                                if (!ok) {
                                    Toast.makeText(
                                        context,
                                        "Không thể đặt nhắc nhở — vui lòng cấp quyền báo thức chính xác trong Cài đặt",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                ReminderScheduler.cancel(context, savedId)
                            }
                            onBack()
                        }
                    }) { Text("Lưu") }
                }
            )
        }
    ) { padding ->
        val previewBg = runCatching { Color(android.graphics.Color.parseColor(bgColorHex)) }.getOrDefault(Color.White)
        val previewText = runCatching { Color(android.graphics.Color.parseColor(textColorHex)) }.getOrDefault(Color.Black)

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Vùng xem trước với nền màu / nền ảnh ────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (bgImageUri == null) previewBg else Color.LightGray)
            ) {
                if (bgImageUri != null) {
                    AsyncImage(
                        model = bgImageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(Modifier.fillMaxWidth().height(180.dp).background(Color.Black.copy(alpha = 0.2f)))
                }
                Column(Modifier.padding(16.dp)) {
                    TextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("Tiêu đề", color = (if (bgImageUri != null) Color.White else previewText).copy(alpha = 0.6f)) },
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            focusedTextColor = if (bgImageUri != null) Color.White else previewText,
                            unfocusedTextColor = if (bgImageUri != null) Color.White else previewText
                        )
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text("Nội dung ghi chú...") }
            )

            Spacer(Modifier.height(16.dp))
            Text("Màu nền", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ColorRow(bgColorPalette, bgColorHex) { bgColorHex = it; bgImageUri = null }

            Spacer(Modifier.height(12.dp))
            Text("Màu chữ", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ColorRow(textColorPalette, textColorHex) { textColorHex = it }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { imagePicker.launch(arrayOf("image/png", "image/jpeg")) }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Đặt ảnh nền (.png/.jpg/.jpeg)")
                }
                if (bgImageUri != null) {
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { bgImageUri = null }) { Text("Bỏ ảnh") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Category", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = category == cat.name,
                        onClick = { category = cat.name },
                        label = { Text("${cat.name} (${categoryCounts[cat.name] ?: 0})") }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tag, cách nhau bởi dấu phẩy (vd: urgent, idea)") }
            )

            Spacer(Modifier.height(16.dp))
            Text("Nhắc nhở", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            ReminderCard(
                reminderAt = reminderAt,
                onSetReminder = { showDatePicker = true },
                onClearReminder = { reminderAt = null }
            )

            // Ghi chú mới tạo (chưa lưu lần nào) thì chưa có ngày tạo/sửa thật
            // để hiển thị, nên chỉ hiện khối này khi đang sửa note đã tồn tại.
            existingNote?.let { note ->
                Spacer(Modifier.height(16.dp))
                NoteMetadataCard(note = note)
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    // ── Bước 1: chọn ngày ────────────────────────────────────────────────────
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pendingDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("Tiếp tục") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Huỷ") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Bước 2: chọn giờ, sau đó ghép ngày + giờ thành 1 timestamp ──────────
    if (showTimePicker) {
        val calendar = remember { Calendar.getInstance() }
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Chọn giờ nhắc") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = pendingDateMillis ?: System.currentTimeMillis()
                    val combined = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    if (combined <= System.currentTimeMillis()) {
                        Toast.makeText(context, "Vui lòng chọn thời điểm trong tương lai", Toast.LENGTH_SHORT).show()
                    } else {
                        reminderAt = combined
                    }
                    showTimePicker = false
                }) { Text("Xác nhận") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
private fun NoteMetadataCard(note: Note) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy 'lúc' HH:mm", Locale("vi")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            MetadataRow(label = "Ngày tạo", value = dateFormat.format(java.util.Date(note.createdAt)))
            Spacer(Modifier.height(6.dp))
            MetadataRow(label = "Sửa lần cuối", value = dateFormat.format(java.util.Date(note.updatedAt)))
            Spacer(Modifier.height(6.dp))
            MetadataRow(
                label = "Số lần đã sửa",
                value = if (note.editCount == 0) "Chưa sửa lần nào" else "${note.editCount} lần"
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReminderCard(
    reminderAt: Long?,
    onSetReminder: () -> Unit,
    onClearReminder: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, dd/MM/yyyy 'lúc' HH:mm", Locale("vi")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (reminderAt != null) Icons.Default.Alarm else Icons.Default.AlarmOff,
                contentDescription = null,
                tint = if (reminderAt != null) MaterialTheme.colorScheme.primary else Color.Gray
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (reminderAt != null) {
                    Text(dateFormat.format(java.util.Date(reminderAt)), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text("Chưa đặt nhắc nhở", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (reminderAt != null) {
                TextButton(onClick = onClearReminder) { Text("Bỏ") }
            }
            TextButton(onClick = onSetReminder) { Text(if (reminderAt != null) "Đổi giờ" else "Đặt giờ") }
        }
    }
}

@Composable
private fun ColorRow(palette: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(palette) { hex ->
            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Gray)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (hex == selected) 3.dp else 1.dp,
                        color = if (hex == selected) MaterialTheme.colorScheme.primary else Color.Gray,
                        shape = CircleShape
                    )
                    .clickable { onSelect(hex) }
            )
        }
    }
}
