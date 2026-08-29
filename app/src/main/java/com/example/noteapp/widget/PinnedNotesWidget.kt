package com.example.noteapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.noteapp.MainActivity
import com.example.noteapp.NoteApplication
import com.example.noteapp.data.Note

/**
 * Widget Home Screen hiển thị các ghi chú đã ghim (isPinned = true).
 * Dùng Glance (Compose cho App Widget) thay vì RemoteViews XML truyền thống
 * vì code ngắn gọn, dễ bảo trì, và tự động style theo Material You (Android 12+).
 *
 * Widget đọc trực tiếp từ Room qua NoteApplication.repository — không cần
 * IPC hay ContentProvider riêng vì widget chạy trong cùng process với app.
 */
class PinnedNotesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = (context.applicationContext as NoteApplication).repository
        val pinnedNotes = repository.getPinnedNotesOnce()

        provideContent {
            GlanceTheme {
                WidgetContent(pinnedNotes)
            }
        }
    }
}

/** Key dùng để truyền noteId qua Intent khi tap vào 1 dòng trong widget.
 *  Phải khớp với MainActivity.EXTRA_WIDGET_NOTE_ID để deep-link hoạt động. */
object WidgetActionKeys {
    val noteIdKey = ActionParameters.Key<Long>(MainActivity.EXTRA_WIDGET_NOTE_ID)
}

@Composable
private fun WidgetContent(notes: List<Note>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .padding(12.dp)
    ) {
        // ── Header ───────────────────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = "\uD83D\uDCCC Ghi chú ghim",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface
                )
            )
            Spacer(modifier = GlanceModifier.width(8.dp))
            Text(
                text = "(${notes.size})",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }

        Spacer(modifier = GlanceModifier.height(8.dp))

        // ── Danh sách ghi chú ghim ───────────────────────────────────────
        if (notes.isEmpty()) {
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = "Chưa có ghi chú nào được ghim.\nMở app và ghim ghi chú để hiện ở đây.",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = GlanceTheme.colors.onSurfaceVariant
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                items(notes, itemId = { it.id }) { note ->
                    Column {
                        PinnedNoteRow(note)
                        Spacer(modifier = GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedNoteRow(note: Note) {
    val params = actionParametersOf(WidgetActionKeys.noteIdKey to note.id)

    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .background(GlanceTheme.colors.surfaceVariant)
            .padding(10.dp)
            .clickable(actionStartActivity<MainActivity>(parameters = params))
    ) {
        Text(
            text = note.title.ifBlank { "(Không tiêu đề)" },
            maxLines = 1,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                maxLines = 2,
                style = TextStyle(
                    fontSize = 12.sp,
                    color = GlanceTheme.colors.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Receiver bắt buộc phải có để Android nhận diện đây là App Widget.
 * Khai báo trong AndroidManifest.xml, trỏ tới class PinnedNotesWidget ở trên.
 */
class PinnedNotesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PinnedNotesWidget()
}
