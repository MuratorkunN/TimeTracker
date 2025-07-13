package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemNoteBinding

class NotesAdapter(
    private val onNoteClicked: (Note) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    class NoteViewHolder(
        private val binding: ItemNoteBinding,
        private val onNoteClicked: (Note) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(note: Note) {
            binding.cardView.strokeColor = Color.parseColor(note.color)

            // THE FIX: Set the title text directly without asterisks.
            binding.textViewNoteTitle.text = note.title
            // Set stroke for the title's "bold" effect
            binding.textViewNoteTitle.setStroke(2f, Color.WHITE)

            // Show a preview of the content, replacing newlines with spaces for a cleaner look
            val previewContent = note.content.replace("\n", " ").trim()
            binding.textViewNoteContent.text = previewContent

            itemView.setOnClickListener { onNoteClicked(note) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NoteViewHolder(binding, onNoteClicked)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {
    override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
}