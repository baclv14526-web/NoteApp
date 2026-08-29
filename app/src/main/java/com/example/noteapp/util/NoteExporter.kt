package com.example.noteapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.noteapp.data.Note
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Xử lý export ghi chú ra .txt / .pdf / .json và import lại từ .json.
 * File tạm được ghi vào cache/exports rồi chia sẻ qua FileProvider (an toàn,
 * không cần quyền ghi bộ nhớ ngoài).
 */
object NoteExporter {

    enum class ExportFormat { TXT, PDF, JSON }

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val fileTimestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    // ── Export ──────────────────────────────────────────────────────────────

    /**
     * Export danh sách ghi chú ra file theo định dạng chỉ định.
     * Trả về Uri (dùng FileProvider) sẵn sàng để chia sẻ qua Intent.
     */
    fun export(context: Context, notes: List<Note>, format: ExportFormat, baseName: String = "NoteApp"): Uri {
        val timestamp = fileTimestamp.format(Date())
        val file = when (format) {
            ExportFormat.TXT -> exportTxt(context, notes, "${baseName}_$timestamp.txt")
            ExportFormat.PDF -> exportPdf(context, notes, "${baseName}_$timestamp.pdf")
            ExportFormat.JSON -> exportJson(context, notes, "${baseName}_$timestamp.json")
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun exportTxt(context: Context, notes: List<Note>, fileName: String): File {
        val file = File(exportsDir(context), fileName)
        file.bufferedWriter().use { writer ->
            notes.forEachIndexed { index, note ->
                writer.appendLine("=".repeat(40))
                writer.appendLine("Tiêu đề: ${note.title.ifBlank { "(Không tiêu đề)" }}")
                writer.appendLine("Category: ${note.category}")
                if (note.tagList.isNotEmpty()) {
                    writer.appendLine("Tags: ${note.tagList.joinToString(", ") { "#$it" }}")
                }
                writer.appendLine("Ngày sửa: ${dateFormat.format(Date(note.updatedAt))}")
                writer.appendLine("-".repeat(40))
                writer.appendLine(note.content)
                writer.appendLine()
                if (index == notes.lastIndex) writer.appendLine("=".repeat(40))
            }
        }
        return file
    }

    private fun exportJson(context: Context, notes: List<Note>, fileName: String): File {
        val file = File(exportsDir(context), fileName)
        val backup = NoteBackup(
            exportedAt = System.currentTimeMillis(),
            noteCount = notes.size,
            notes = notes
        )
        file.writeText(gson.toJson(backup))
        return file
    }

    private fun exportPdf(context: Context, notes: List<Note>, fileName: String): File {
        val file = File(exportsDir(context), fileName)
        val document = PdfDocument()

        val pageWidth = 595   // A4 @ 72dpi
        val pageHeight = 842
        val margin = 40f
        val maxLineWidth = pageWidth - margin * 2

        val titlePaint = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val metaPaint = Paint().apply { textSize = 10f; color = android.graphics.Color.DKGRAY }
        val bodyPaint = Paint().apply { textSize = 12f }

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = margin + 20f

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = margin + 20f
        }

        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val lines = mutableListOf<String>()
            text.split("\n").forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    lines.add("")
                    return@forEach
                }
                var current = StringBuilder()
                paragraph.split(" ").forEach { word ->
                    val candidate = if (current.isEmpty()) word else "$current $word"
                    if (paint.measureText(candidate) > maxWidth && current.isNotEmpty()) {
                        lines.add(current.toString())
                        current = StringBuilder(word)
                    } else {
                        current = StringBuilder(candidate)
                    }
                }
                if (current.isNotEmpty()) lines.add(current.toString())
            }
            return lines
        }

        notes.forEach { note ->
            if (y > pageHeight - margin - 60) newPage()

            canvas.drawText(note.title.ifBlank { "(Không tiêu đề)" }, margin, y, titlePaint)
            y += 18f

            val meta = "Category: ${note.category}" +
                (if (note.tagList.isNotEmpty()) "  •  Tags: ${note.tagList.joinToString(", ") { "#$it" }}" else "") +
                "  •  ${dateFormat.format(Date(note.updatedAt))}"
            wrapText(meta, metaPaint, maxLineWidth).forEach { line ->
                if (y > pageHeight - margin) newPage()
                canvas.drawText(line, margin, y, metaPaint)
                y += 13f
            }
            y += 6f

            wrapText(note.content, bodyPaint, maxLineWidth).forEach { line ->
                if (y > pageHeight - margin) newPage()
                canvas.drawText(line, margin, y, bodyPaint)
                y += 16f
            }
            y += 20f
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    // ── Share Intent ────────────────────────────────────────────────────────

    fun shareIntent(context: Context, uri: Uri, format: ExportFormat): Intent {
        val mimeType = when (format) {
            ExportFormat.TXT -> "text/plain"
            ExportFormat.PDF -> "application/pdf"
            ExportFormat.JSON -> "application/json"
        }
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    // ── Import ──────────────────────────────────────────────────────────────

    data class NoteBackup(
        val exportedAt: Long,
        val noteCount: Int,
        val notes: List<Note>
    )

    /** Đọc file .json đã export trước đó, trả về danh sách Note để import vào DB. */
    suspend fun parseJsonBackup(context: Context, uri: Uri): List<Note> = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Không đọc được file")

        // Hỗ trợ cả 2 dạng: file backup đầy đủ {exportedAt, notes:[...]}
        // hoặc chỉ đơn giản là một mảng Note [...] (phòng trường hợp người dùng
        // tự chỉnh sửa file JSON).
        try {
            val backup = gson.fromJson(text, NoteBackup::class.java)
            backup?.notes ?: emptyList()
        } catch (e: Exception) {
            val listType = object : TypeToken<List<Note>>() {}.type
            gson.fromJson<List<Note>>(text, listType) ?: emptyList()
        }
    }

    /**
     * Đọc file .txt đã export trước đó (theo đúng format của exportTxt ở trên)
     * và parse ngược lại thành danh sách Note.
     *
     * Format mỗi ghi chú:
     *   ========================================
     *   Tiêu đề: ...
     *   Category: ...
     *   Tags: #tag1, #tag2        (dòng này có thể vắng mặt nếu không có tag)
     *   Ngày sửa: dd/MM/yyyy HH:mm
     *   ----------------------------------------
     *   <nội dung, có thể nhiều dòng>
     *
     * Nếu file không khớp format trên (ví dụ người dùng tự viết file .txt tay),
     * fallback: coi toàn bộ nội dung file là 1 ghi chú duy nhất, tiêu đề lấy
     * từ dòng đầu tiên không rỗng.
     */
    suspend fun parseTxtBackup(context: Context, uri: Uri): List<Note> = withContext(Dispatchers.IO) {
        val rawText = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw IllegalArgumentException("Không đọc được file")

        val separator = "=".repeat(40)
        val metaDivider = "-".repeat(40)

        // Tách file thành từng khối ghi chú dựa trên dòng "====...====".
        // filter loại bỏ khối rỗng sinh ra do dấu phân cách ở đầu/cuối file.
        val blocks = rawText.split(separator).map { it.trim() }.filter { it.isNotBlank() }

        val looksLikeExportFormat = blocks.isNotEmpty() && blocks.all { it.contains(metaDivider) }

        if (!looksLikeExportFormat) {
            // Fallback: toàn bộ file là 1 ghi chú.
            val trimmed = rawText.trim()
            if (trimmed.isBlank()) {
                emptyList()
            } else {
                val firstLine = trimmed.lineSequence().firstOrNull { it.isNotBlank() }?.take(60) ?: "Ghi chú nhập từ .txt"
                listOf(
                    Note(
                        title = firstLine,
                        content = trimmed,
                        category = "Nhập từ .txt"
                    )
                )
            }
        } else {
            blocks.mapNotNull { block -> parseTxtBlock(block, metaDivider) }
        }
    }

    private fun parseTxtBlock(block: String, metaDivider: String): Note? {
        val parts = block.split(metaDivider, limit = 2)
        if (parts.size < 2) return null

        val metaLines = parts[0].lines().map { it.trim() }.filter { it.isNotBlank() }
        val content = parts[1].trim()

        var title = ""
        var category = "Chung"
        var tags = ""

        metaLines.forEach { line ->
            when {
                line.startsWith("Tiêu đề:") -> title = line.removePrefix("Tiêu đề:").trim()
                line.startsWith("Category:") -> category = line.removePrefix("Category:").trim()
                line.startsWith("Tags:") -> {
                    // Tags được lưu dạng "#tag1, #tag2" khi export, bỏ dấu # khi đọc lại.
                    tags = line.removePrefix("Tags:").trim()
                        .split(",")
                        .map { it.trim().removePrefix("#").trim() }
                        .filter { it.isNotEmpty() }
                        .joinToString(",")
                }
                // Dòng "Ngày sửa:" bị bỏ qua có chủ đích: ghi chú import sẽ nhận
                // ngày tạo/sửa là thời điểm import, không cố khôi phục ngày cũ vì
                // định dạng ngày trong .txt là để đọc, không phải để parse ngược.
            }
        }

        if (title.isBlank() && content.isBlank()) return null

        return Note(
            title = title.ifBlank { "(Không tiêu đề)" },
            content = content,
            category = category.ifBlank { "Chung" },
            tags = tags
        )
    }
}
