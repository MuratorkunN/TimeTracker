// app/src/main/java/com/example/roboticsgenius/TimelineAdapter.kt

package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemTimelineHeaderBinding
import com.example.roboticsgenius.databinding.ItemTimelineLogBinding
import java.text.SimpleDateFormat
import java.util.*

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_LOG = 1

class TimelineAdapter(
    private val onLogLongClick: (logId: Int) -> Unit
) : ListAdapter<TimelineItem, RecyclerView.ViewHolder>(TimelineDiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TimelineItem.Header -> VIEW_TYPE_HEADER
            is TimelineItem.Log -> VIEW_TYPE_LOG
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemTimelineHeaderBinding.inflate(inflater, parent, false))
            VIEW_TYPE_LOG -> LogViewHolder(ItemTimelineLogBinding.inflate(inflater, parent, false), onLogLongClick)
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is TimelineItem.Header -> (holder as HeaderViewHolder).bind(item)
            is TimelineItem.Log -> (holder as LogViewHolder).bind(item)
        }
    }

    class HeaderViewHolder(private val binding: ItemTimelineHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: TimelineItem.Header) {
            binding.textViewDate.text = header.date
            binding.textViewDailyTotal.text = header.totalDuration
        }
    }

    class LogViewHolder(
        private val binding: ItemTimelineLogBinding,
        private val onLogLongClick: (logId: Int) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(logItem: TimelineItem.Log) {
            val log = logItem.logEntry
            binding.textViewLogActivityName.text = log.activityName
            binding.textViewLogDuration.text = formatDuration(log.durationInSeconds)

            // THE FIX: Set color indicator and new time range text
            binding.colorIndicator.setBackgroundColor(Color.parseColor(log.activityColor))
            val startTime = Date(log.startTime)
            val endTime = Date(log.startTime + (log.durationInSeconds * 1000L))
            binding.textViewLogTime.text = "${timeFormat.format(startTime)} - ${timeFormat.format(endTime)}"

            itemView.setOnLongClickListener {
                onLogLongClick(log.id)
                true
            }
        }

        private fun formatDuration(seconds: Int): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }
}

class TimelineDiffCallback : DiffUtil.ItemCallback<TimelineItem>() {
    override fun areItemsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
        return when {
            oldItem is TimelineItem.Header && newItem is TimelineItem.Header -> oldItem.id == newItem.id
            oldItem is TimelineItem.Log && newItem is TimelineItem.Log -> oldItem.logEntry.id == newItem.logEntry.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: TimelineItem, newItem: TimelineItem): Boolean {
        return oldItem == newItem
    }
}