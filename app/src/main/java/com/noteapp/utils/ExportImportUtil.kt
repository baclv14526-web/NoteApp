package com.noteapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.noteapp.data.db.entities.Note
import com.noteapp.data.db.entities.NoteWithTags
import com.noteapp.data.db.entities.Tag
import com.noteapp.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

class ExportImportUtil(
    private val context: Context,
    private val repository: NoteRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    // ─── Export ──────────────────────────────────────────────────────────────

    suspend fun exportNotes(format: String) {
        withContext(Dispatchers.IO) {
            try {
                val notes = repository.getAllNotesForExport()
                val timestamp = dateFormat.format(Date())
                val fileName = "NoteApp_$timestamp.$format"
                val file = File(context.getExternalFilesDir(null), fileName)

                val content = when (format) {
                    "txt"  -> buildTxtContent(notes)
                    "json" -> buildJsonContent(notes)
                    else   -> return@withContext
                }

                file.writeText(content, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    shareFile(file, format)
                    Toast.makeText(context, "Đã xuất: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi xuất: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildTxtContent(notes: List<NoteWithTags>): String {
        val sb = StringBuilder()
        sb.appendLine("=== NOTEAPP EXPORT ===")
        sb.appendLine("Ngày xuất: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine("Số ghi chú: ${notes.size}")
        sb.appendLine()
        notes.forEachIndexed { i, nwt ->
            sb.appendLine("─".repeat(50))
            sb.appendLine("[${i + 1}] ${nwt.note.title.ifEmpty { "(Không có tiêu đề)" }}")
            if (nwt.tags.isNotEmpty()) {
                sb.appendLine("Tags: ${nwt.tags.joinToString(", ") { "#${it.name}" }}")
            }
            sb.appendLine("Ngày tạo: ${formatDate(nwt.note.createdAt)}")
            sb.appendLine("Cập nhật: ${formatDate(nwt.note.updatedAt)}")
            if (nwt.note.isSecure) sb.appendLine("[BẢO MẬT]")
            sb.appendLine()
            sb.appendLine(nwt.note.content)
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun buildJsonContent(notes: List<NoteWithTags>): String {
        val exportList = notes.map { nwt ->
            mapOf(
                "id"             to nwt.note.id,
                "title"          to nwt.note.title,
                "content"        to nwt.note.content,
                "tags"           to nwt.tags.map { it.name },
                "backgroundColor" to nwt.note.backgroundColor,
                "textColor"      to nwt.note.textColor,
                "isPinned"       to nwt.note.isPinned,
                "isSecure"       to nwt.note.isSecure,
                "createdAt"      to nwt.note.createdAt,
                "updatedAt"      to nwt.note.updatedAt
            )
        }
        return gson.toJson(mapOf("version" to 1, "notes" to exportList))
    }

    private fun shareFile(file: File, format: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val mimeType = if (format == "json") "application/json" else "text/plain"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ bản sao lưu").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    // ─── Import ──────────────────────────────────────────────────────────────

    suspend fun importFromUri(uri: Uri, format: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val content = readTextFromUri(uri)
                when (format) {
                    "txt"  -> importFromTxt(content)
                    "json" -> importFromJson(content)
                    else   -> 0
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi import: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                0
            }
        }
    }

    private fun readTextFromUri(uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.appendLine(line)
                }
            }
        }
        return sb.toString()
    }

    private suspend fun importFromTxt(content: String): Int {
        var count = 0
        val sections = content.split("─".repeat(50))
        for (section in sections.drop(1)) {
            if (section.isBlank()) continue
            val lines = section.trim().lines()
            if (lines.isEmpty()) continue
            val titleLine = lines.firstOrNull { it.startsWith("[") }
            val title = titleLine?.substringAfter("] ") ?: "Imported"
            val contentLines = lines.dropWhile {
                it.startsWith("[") || it.startsWith("Tags:") ||
                it.startsWith("Ngày") || it.startsWith("Cập nhật") ||
                it.startsWith("[BẢO") || it.isBlank()
            }
            val noteContent = contentLines.joinToString("\n").trim()
            if (noteContent.isNotBlank() || title.isNotBlank()) {
                repository.insertNote(
                    Note(title = title, content = noteContent)
                )
                count++
            }
        }
        return count
    }

    private suspend fun importFromJson(content: String): Int {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        val data: Map<String, Any> = gson.fromJson(content, type)
        @Suppress("UNCHECKED_CAST")
        val notes = data["notes"] as? List<Map<String, Any>> ?: return 0
        var count = 0
        for (noteMap in notes) {
            val title   = noteMap["title"]   as? String ?: ""
            val noteContent = noteMap["content"] as? String ?: ""
            val isPinned = noteMap["isPinned"] as? Boolean ?: false
            val isSecure = noteMap["isSecure"] as? Boolean ?: false
            val bgColor  = (noteMap["backgroundColor"] as? Double)?.toLong()?.toInt()
                        ?: 0xFFFFFFFF.toInt()
            val txtColor = (noteMap["textColor"] as? Double)?.toLong()?.toInt()
                        ?: 0xFF212121.toInt()
            val noteId = repository.insertNote(
                Note(
                    title = title,
                    content = noteContent,
                    backgroundColor = bgColor,
                    textColor = txtColor,
                    isPinned = isPinned,
                    isSecure = isSecure
                )
            )
            @Suppress("UNCHECKED_CAST")
            val tagNames = noteMap["tags"] as? List<String> ?: emptyList()
            for (tagName in tagNames) {
                val existing = repository.getAllTagsSync().find { it.name == tagName }
                val tagId = existing?.id ?: repository.insertTag(Tag(name = tagName))
                repository.insertNoteTagCrossRef(
                    com.noteapp.data.db.entities.NoteTagCrossRef(noteId, tagId)
                )
            }
            count++
        }
        return count
    }

    private fun formatDate(ts: Long) =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
}
