package com.noteapp.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.noteapp.data.db.entities.Note
import com.noteapp.data.db.entities.NoteTagCrossRef
import com.noteapp.data.db.entities.NoteWithTags
import com.noteapp.data.db.entities.Tag
import com.noteapp.data.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExportImportUtil(
    private val context: Context,
    private val repository: NoteRepository
) {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val stampFmt   = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    companion object {
        private const val TAG = "ExportImportUtil"
    }

    // ── Export ────────────────────────────────────────────────────────────────

    suspend fun exportNotes(format: String) {
        withContext(Dispatchers.IO) {
            try {
                val notes    = repository.getAllNotesForExport()
                val fileName = "NoteApp_${stampFmt.format(Date())}.$format"

                // getExternalFilesDir có thể null trên một số thiết bị Android 9
                // (SD card bị ngắt, hoặc storage chưa mount) → fallback về internal storage
                val dir  = context.getExternalFilesDir(null) ?: context.filesDir
                val file = File(dir, fileName)

                val content: String
                when (format) {
                    "txt"  -> content = buildTxtContent(notes)
                    "json" -> content = buildJsonContent(notes)
                    else   -> return@withContext
                }

                file.writeText(content, Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    try {
                        shareFile(file, format)
                        Toast.makeText(context, "Đã xuất: ${file.name}", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "shareFile failed", e)
                        Toast.makeText(context, "Xuất thành công: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "exportNotes failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi xuất: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildTxtContent(notes: List<NoteWithTags>): String {
        val sb = StringBuilder()
        sb.appendLine("=== NOTEAPP EXPORT ===")
        sb.appendLine("Ngày xuất: ${displayFmt.format(Date())}")
        sb.appendLine("Số ghi chú: ${notes.size}")
        sb.appendLine()
        notes.forEachIndexed { idx, nwt ->
            sb.appendLine("─".repeat(50))
            sb.appendLine("[${idx + 1}] ${nwt.note.title.ifEmpty { "(Không có tiêu đề)" }}")
            if (nwt.tags.isNotEmpty()) {
                sb.appendLine("Tags: ${nwt.tags.joinToString(", ") { "#${it.name}" }}")
            }
            sb.appendLine("Ngày tạo: ${displayFmt.format(Date(nwt.note.createdAt))}")
            sb.appendLine("Cập nhật: ${displayFmt.format(Date(nwt.note.updatedAt))}")
            if (nwt.note.isSecure) sb.appendLine("[BẢO MẬT]")
            sb.appendLine()
            sb.appendLine(nwt.note.content)
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun buildJsonContent(notes: List<NoteWithTags>): String {
        val list = notes.map { nwt ->
            mapOf(
                "id"              to nwt.note.id,
                "title"           to nwt.note.title,
                "content"         to nwt.note.content,
                "tags"            to nwt.tags.map { it.name },
                "backgroundColor" to nwt.note.backgroundColor,
                "textColor"       to nwt.note.textColor,
                "isPinned"        to nwt.note.isPinned,
                "isSecure"        to nwt.note.isSecure,
                "createdAt"       to nwt.note.createdAt,
                "updatedAt"       to nwt.note.updatedAt
            )
        }
        return gson.toJson(mapOf("version" to 1, "notes" to list))
    }

    private fun shareFile(file: File, format: String) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val mime  = if (format == "json") "application/json" else "text/plain"
        val share = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(share, "Chia sẻ bản sao lưu")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // ── Import ────────────────────────────────────────────────────────────────

    suspend fun importFromUri(uri: Uri, format: String): Int {
        return withContext(Dispatchers.IO) {
            try {
                val content = readText(uri)
                if (content.isBlank()) return@withContext 0
                when (format) {
                    "txt"  -> importTxt(content)
                    "json" -> importJson(content)
                    else   -> 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "importFromUri failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi import: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                0
            }
        }
    }

    private fun readText(uri: Uri): String {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    sb.appendLine(line)
                    line = reader.readLine()
                }
            }
        }
        return sb.toString()
    }

    private suspend fun importTxt(content: String): Int {
        var count = 0
        val sections = content.split("─".repeat(50))
        for (section in sections.drop(1)) {
            if (section.isBlank()) continue
            val lines     = section.trim().lines()
            val titleLine = lines.firstOrNull { it.startsWith("[") }
            val title     = titleLine?.substringAfter("] ")?.trim() ?: "Imported"
            val body      = lines
                .dropWhile { it.startsWith("[") || it.startsWith("Tags:") ||
                             it.startsWith("Ngày") || it.startsWith("Cập nhật") ||
                             it.startsWith("[BẢO") || it.isBlank() }
                .joinToString("\n")
                .trim()
            if (title.isNotBlank() || body.isNotBlank()) {
                try {
                    repository.insertNote(Note(title = title, content = body))
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "importTxt: insert note failed", e)
                }
            }
        }
        return count
    }

    private suspend fun importJson(content: String): Int {
        return try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val data: Map<String, Any> = gson.fromJson(content, mapType)

            @Suppress("UNCHECKED_CAST")
            val notesList = data["notes"] as? List<Map<String, Any>> ?: return 0

            var count = 0
            for (map in notesList) {
                try {
                    val title    = map["title"]   as? String  ?: ""
                    val body     = map["content"] as? String  ?: ""
                    val isPinned = map["isPinned"] as? Boolean ?: false
                    val isSecure = map["isSecure"] as? Boolean ?: false
                    val bgColor  = (map["backgroundColor"] as? Double)?.toInt()
                                    ?: (map["backgroundColor"] as? Long)?.toInt()
                                    ?: 0xFFFFFFFF.toInt()
                    val txtColor = (map["textColor"] as? Double)?.toInt()
                                    ?: (map["textColor"] as? Long)?.toInt()
                                    ?: 0xFF212121.toInt()

                    val noteId = repository.insertNote(
                        Note(
                            title           = title,
                            content         = body,
                            backgroundColor = bgColor,
                            textColor       = txtColor,
                            isPinned        = isPinned,
                            isSecure        = isSecure
                        )
                    )

                    @Suppress("UNCHECKED_CAST")
                    val tagNames = map["tags"] as? List<String> ?: emptyList()
                    for (name in tagNames) {
                        try {
                            val existing = repository.getAllTagsSync().find { it.name == name }
                            val tagId    = existing?.id ?: repository.insertTag(Tag(name = name))
                            repository.insertNoteTagCrossRef(NoteTagCrossRef(noteId, tagId))
                        } catch (e: Exception) {
                            Log.e(TAG, "importJson: insert tag failed for '$name'", e)
                        }
                    }
                    count++
                } catch (e: Exception) {
                    Log.e(TAG, "importJson: failed to import one note", e)
                }
            }
            count
        } catch (e: Exception) {
            Log.e(TAG, "importJson: parse failed", e)
            0
        }
    }
}
