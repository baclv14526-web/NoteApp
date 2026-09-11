package com.example.noteapp.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.noteapp.util.NoteExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Export dùng ActivityResultContracts.CreateDocument — hệ thống hiện file
 * picker để người dùng TỰ CHỌN nơi lưu (bộ nhớ trong, thẻ nhớ microSD,
 * Google Drive...). Không cần quyền WRITE_EXTERNAL_STORAGE.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(viewModel: NoteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dateStamp = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) }

    var exportingFormat by remember { mutableStateOf<NoteExporter.ExportFormat?>(null) }
    var isImportingJson by remember { mutableStateOf(false) }
    var isImportingTxt by remember { mutableStateOf(false) }

    fun writeExport(destUri: Uri, format: NoteExporter.ExportFormat) {
        scope.launch {
            exportingFormat = format
            try {
                val allNotes = viewModel.getAllNotesForExport(excludeLocked = false)
                val exportableNotes = viewModel.getAllNotesForExport(excludeLocked = true)
                val lockedCount = allNotes.size - exportableNotes.size

                if (exportableNotes.isEmpty()) {
                    Toast.makeText(
                        context,
                        if (lockedCount > 0) "Tất cả ghi chú đều đang bị khoá, không thể xuất"
                        else "Chưa có ghi chú nào để xuất",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(destUri)?.use { out ->
                        NoteExporter.exportToStream(out, exportableNotes, format, context)
                    } ?: throw Exception("Không mở được file để ghi")
                }

                val msg = buildString {
                    append("✓ Đã lưu ${exportableNotes.size} ghi chú")
                    if (lockedCount > 0) append(" (bỏ qua $lockedCount ghi chú bí mật)")
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi lưu file: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                exportingFormat = null
            }
        }
    }

    fun doImport(
        uri: Uri,
        setLoading: (Boolean) -> Unit,
        parse: suspend (android.content.Context, Uri) -> List<com.example.noteapp.data.Note>
    ) {
        setLoading(true)
        scope.launch {
            try {
                val notes = parse(context, uri)
                if (notes.isEmpty()) {
                    Toast.makeText(context, "File không có ghi chú hợp lệ", Toast.LENGTH_SHORT).show()
                    setLoading(false)
                } else {
                    viewModel.importNotes(notes) { count ->
                        Toast.makeText(context, "✓ Đã nhập $count ghi chú", Toast.LENGTH_LONG).show()
                        setLoading(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi đọc file: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    // CreateDocument mở file picker, người dùng chọn nơi lưu
    val saveJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { writeExport(it, NoteExporter.ExportFormat.JSON) } }

    val saveTxtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri -> uri?.let { writeExport(it, NoteExporter.ExportFormat.TXT) } }

    val savePdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> uri?.let { writeExport(it, NoteExporter.ExportFormat.PDF) } }

    val importJsonPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { doImport(it, { v -> isImportingJson = v }) { ctx, u -> NoteExporter.parseJsonBackup(ctx, u) } }
    }

    val importTxtPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { doImport(it, { v -> isImportingTxt = v }) { ctx, u -> NoteExporter.parseTxtBackup(ctx, u) } }
    }

    val isExporting = exportingFormat != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Export / Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── XUẤT GHI CHÚ ────────────────────────────────────────────────
            SectionHeader(
                title = "Xuất ghi chú ra file",
                subtitle = "Hệ thống sẽ mở cửa sổ chọn vị trí lưu — bạn có thể lưu vào bộ nhớ trong, thẻ nhớ microSD hoặc Google Drive"
            )
            Spacer(Modifier.height(12.dp))

            ExportCard(
                icon = Icons.Default.DataObject,
                title = "Lưu file .json",
                subtitle = "Backup đầy đủ — nhập lại được",
                badge = "Khuyến nghị",
                loading = exportingFormat == NoteExporter.ExportFormat.JSON,
                enabled = !isExporting,
                onClick = { saveJsonLauncher.launch("NoteApp_backup_$dateStamp.json") }
            )
            Spacer(Modifier.height(10.dp))
            ExportCard(
                icon = Icons.Default.Description,
                title = "Lưu file .txt",
                subtitle = "Văn bản thuần, mở được trên mọi thiết bị",
                loading = exportingFormat == NoteExporter.ExportFormat.TXT,
                enabled = !isExporting,
                onClick = { saveTxtLauncher.launch("NoteApp_$dateStamp.txt") }
            )
            Spacer(Modifier.height(10.dp))
            ExportCard(
                icon = Icons.Default.PictureAsPdf,
                title = "Lưu file .pdf",
                subtitle = "Để in hoặc lưu trữ — không nhập lại được",
                loading = exportingFormat == NoteExporter.ExportFormat.PDF,
                enabled = !isExporting,
                onClick = { savePdfLauncher.launch("NoteApp_$dateStamp.pdf") }
            )

            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    "⚠️  Ghi chú bí mật (đã khoá PIN) sẽ không được đưa vào file xuất để bảo vệ dữ liệu riêng tư.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(28.dp))
            HorizontalDivider()
            Spacer(Modifier.height(28.dp))

            // ── NHẬP GHI CHÚ ────────────────────────────────────────────────
            SectionHeader(
                title = "Nhập ghi chú từ file",
                subtitle = "Ghi chú được thêm mới, không ghi đè dữ liệu hiện có"
            )
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { importJsonPicker.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImportingJson && !isImportingTxt
            ) {
                if (isImportingJson) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Đang nhập...")
                } else {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhập từ file .json")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { importTxtPicker.launch(arrayOf("text/plain")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImportingJson && !isImportingTxt
            ) {
                if (isImportingTxt) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Đang nhập...")
                } else {
                    Icon(Icons.Default.FileUpload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nhập từ file .txt")
                }
            }

            Text(
                "File .pdf không hỗ trợ nhập lại.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ExportCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badge: String? = null,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled && !loading,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.SaveAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
