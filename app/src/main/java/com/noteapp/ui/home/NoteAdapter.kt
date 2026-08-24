package com.noteapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.noteapp.data.db.entities.Note
import com.noteapp.databinding.ItemNoteBinding
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : PagingDataAdapter<Note, NoteAdapter.NoteVH>(DIFF) {

    inner class NoteVH(private val b: ItemNoteBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(note: Note) {
            b.tvTitle.text   = note.title.ifEmpty { "Không có tiêu đề" }
            b.tvContent.text = note.content.take(200)
            b.tvDate.text    = fmtDate(note.updatedAt)

            // Background image OR color
            if (!note.backgroundImageUri.isNullOrEmpty()) {
                b.ivBackground.visibility = View.VISIBLE
                try {
                    Glide.with(b.root)
                        .load(note.backgroundImageUri)
                        .centerCrop()
                        .into(b.ivBackground)
                } catch (t: Throwable) {
                    b.ivBackground.visibility = View.GONE
                }
                b.cardNote.setCardBackgroundColor(0x00000000)
            } else {
                b.ivBackground.visibility = View.GONE
                b.cardNote.setCardBackgroundColor(note.backgroundColor)
            }

            b.tvTitle.setTextColor(note.textColor)
            b.tvContent.setTextColor(note.textColor)
            b.tvDate.setTextColor(note.textColor)

            b.ivLock.visibility = if (note.isSecure) View.VISIBLE else View.GONE
            b.ivPin.visibility  = if (note.isPinned) View.VISIBLE else View.GONE

            b.root.setOnClickListener     { onClick(note) }
            b.root.setOnLongClickListener { onLongClick(note); true }
        }

        private fun fmtDate(ts: Long) =
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteVH =
        NoteVH(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: NoteVH, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(a: Note, b: Note)    = a.id == b.id
            override fun areContentsTheSame(a: Note, b: Note) = a == b
        }
    }
}
