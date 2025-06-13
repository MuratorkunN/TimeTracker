package com.example.roboticsgenius

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class HistoryAdapter : ListAdapter<TimeLogWithActivityName, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class HistoryViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(log: TimeLogWithActivityName) {
            binding.textViewHistoryActivityName.text = log.activityName
            binding.textViewHistoryDuration.text = "Duration: ${formatTime(log.durationInSeconds)}"
            binding.textViewHistoryDate.text = "Logged at: ${dateFormat.format(Date(log.startTime))}"
        }

        private fun formatTime(seconds: Int): String {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            val secs = seconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, secs)
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<TimeLogWithActivityName>() {
    override fun areItemsTheSame(oldItem: TimeLogWithActivityName, newItem: TimeLogWithActivityName): Boolean {
        return oldItem.startTime == newItem.startTime && oldItem.activityName == newItem.activityName
    }

    override fun areContentsTheSame(oldItem: TimeLogWithActivityName, newItem: TimeLogWithActivityName): Boolean {
        return oldItem == newItem
    }
}