package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentTimelineBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private lateinit var timelineAdapter: TimelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getDatabase(requireContext())
        setupRecyclerView()

        lifecycleScope.launch {
            db.activityDao().getAllLogsWithActivityNames().collect { logs ->
                processAndSubmitLogs(logs)
            }
        }
    }

    private fun setupRecyclerView() {
        timelineAdapter = TimelineAdapter()
        binding.recyclerViewTimeline.apply {
            adapter = timelineAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun processAndSubmitLogs(logs: List<TimeLogWithActivityName>) {
        if (logs.isEmpty()) {
            timelineAdapter.submitList(emptyList())
            // Optionally show an "empty state" view
            return
        }

        val timelineItems = mutableListOf<TimelineItem>()
        // Group logs by their calendar date
        val groupedLogs = logs.groupBy { getCalendarDate(it.startTime) }

        for ((date, dailyLogs) in groupedLogs) {
            // Add Header
            val totalDurationSeconds = dailyLogs.sumOf { it.durationInSeconds }
            timelineItems.add(
                TimelineItem.Header(
                    id = date.time.toString(),
                    date = formatDate(date),
                    totalDuration = formatDuration(totalDurationSeconds)
                )
            )
            // Add Log Entries for that day
            dailyLogs.forEach { log ->
                timelineItems.add(TimelineItem.Log(id = log.startTime, logEntry = log))
            }
        }
        timelineAdapter.submitList(timelineItems)
    }

    private fun getCalendarDate(timestamp: Long): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatDate(cal: Calendar): String {
        val today = getCalendarDate(System.currentTimeMillis())
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DATE, -1) }

        return when (cal) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(cal.time)
        }
    }

    private fun formatDuration(totalSeconds: Int): String {
        if (totalSeconds < 0) return ""
        val hours = TimeUnit.SECONDS.toHours(totalSeconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong()) % 60
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}