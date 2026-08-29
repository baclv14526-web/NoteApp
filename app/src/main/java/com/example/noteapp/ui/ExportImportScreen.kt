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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.noteapp.util.NoteExporter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportImportScreen(viewModel: NoteViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImportingJson by remember { mutableStateOf(false) }
    var isImportingTxt by remember { mutableStateOf(false) }

    fun doExport(format: NoteExporter.ExportFormat) {
        scope.launch {
            isExporting = true
            try {
                // Mặc định loại trừ ghi chú bí mật khỏi file export — các định
                // dạng .txt/.pdf/.json đều không mã hoá, xuất note bí mật ra
                // sẽ vô hiệu hoá hoàn toàn mục đích của tính năng khoá PIN.
                val allNotes = viewModel.getAllNotesForExport(excludeLocked = false)
                val exportableNotes = viewModel.getAllNotesForExport(excludeLocked = true)
                val lockedCount = allNotes.size - exportableNotes.size

                if (exportableNotes.isEmpty()) {
                    val message = if (lockedCount > 0) {
                        "Tất cả ghi chú đều đang bị khoá nên không thể xuất file"
                    } else {
                        "Chưa có ghi chú nào để xuất"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                } else {
                    if (lockedCount > 0) {
                        Toast.makeText(
                            context,
                            "Đã bỏ qua $lockedCount ghi chú bí mật khi xuất file (không mã hoá)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    val uri = NoteExporter.export(context, exportableNotes, format)
                    val intent = NoteExporter.shareIntent(context, uri, format)
                    context.startActivity(android.content.Intent.createChooser(intent, "Chia sẻ file ghi chú"))
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi xuất file: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isExporting = false
            }
        }
    }

    /** Import chung cho cả .json và .txt — chỉ khác hàm parse được truyền vào. */
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
                        Toast.makeText(context, "Đã import $count ghi chú", Toast.LENGTH_LONG).show()
                        setLoading(false)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi khi đọc file: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
        }
    }

    // Chọn file .json từ máy để import lại (backup đầy đủ)
    val importJsonPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            doImport(uri, { isImportingJson = it }) { ctx, u -> NoteExporter.parseJsonBackup(ctx, u) }
        }
    }

    // Chọn file .txt từ máy để import lại (theo format app tự xuất ra, hoặc
    // fallback coi cả file là 1 ghi chú nếu không đúng format)
    val importTxtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            doImport(uri, { isImportingTxt = it }) { ctx, u -> NoteExporter.parseTxtBackup(ctx, u) }
        }
    }

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
        Column(Modifier.padding(padding).padding(16.dp)) {

            Text("Xuất ghi chú (Export)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Xuất toàn bộ ghi chú ra file để backup hoặc chia sẻ. Ghi chú bí mật (đã khoá) sẽ không được đưa vào file.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            ExportOptionCard(
                icon = Icons.Default.DataObject,
                title = "Xuất file .json",
                subtitle = "Đầy đủ dữ liệu — dùng để backup và import lại sau này",
                enabled = !isExporting,
                onClick = { doExport(NoteExporter.ExportFormat.JSON) }
            )
            Spacer(Modifier.height(10.dp))
            ExportOptionCard(
                icon = Icons.Default.Description,
                title = "Xuất file .txt",
                subtitle = "Văn bản thuần, dễ đọc, dễ chia sẻ",
                enabled = !isExporting,
                onClick = { doExport(NoteExporter.ExportFormat.TXT) }
            )
            Spacer(Modifier.height(10.dp))
            ExportOptionCard(
                icon = Icons.Default.PictureAsPdf,
                title = "Xuất file .pdf",
                subtitle = "Định dạng để in hoặc lưu trữ trang trọng",
                enabled = !isExporting,
                onClick = { doExport(NoteExporter.ExportFormat.PDF) }
            )

            if (isExporting) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Đang chuẩn bị file...", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text("Nhập ghi chú (Import)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Nhập lại ghi chú từ file .json hoặc .txt đã xuất trước đó. Ghi chú import sẽ được thêm mới, không ghi đè dữ liệu hiện có.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { importJsonPicker.launch(arrayOf("application/json", "text/plain")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImportingJson && !isImportingTxt
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isImportingJson) "Đang nhập..." else "Chọn file .json để nhập")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { importTxtPicker.launch(arrayOf("text/plain")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isImportingJson && !isImportingTxt
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isImportingTxt) "Đang nhập..." else "Chọn file .txt để nhập")
            }

            Text(
                "Lưu ý: file .pdf không hỗ trợ nhập lại (PDF không lưu category/tag/màu ở dạng đọc được).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ExportOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.FileDownload, contentDescription = "Xuất file")
        }
    }
}
