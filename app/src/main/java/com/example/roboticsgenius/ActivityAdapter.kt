// ActivityAdapter.kt
package com.example.roboticsgenius
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemActivityBinding
import com.google.android.material.card.MaterialCardView

class ActivityAdapter(
    private val onStartClick: (Activity) -> Unit,
    private val onStopClick: () -> Unit
) : ListAdapter<Activity, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    private var activeId: Int? = null
    private var currentTime: Int = 0

    fun setUiState(activeActivityId: Int?, time: Int) {
        val needsUpdate = activeId != activeActivityId || (activeActivityId != null)
        activeId = activeActivityId
        currentTime = time
        if (needsUpdate) notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position), activeId, currentTime, onStartClick, onStopClick)
    }

    class ActivityViewHolder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            activity: Activity,
            activeId: Int?,
            currentTime: Int,
            onStartClick: (Activity) -> Unit,
            onStopClick: () -> Unit
        ) {
            binding.textViewActivityName.text = activity.name
            (binding.cardView as MaterialCardView).strokeColor = Color.parseColor(activity.color)
            (binding.textViewStreak.background as GradientDrawable).setColor(Color.parseColor(activity.color))
            binding.textViewStreak.text = "🔥 0"

            val isTimerRunning = activeId != null
            val isThisCardActive = isTimerRunning && activeId == activity.id

            if (isTimerRunning) {
                if (isThisCardActive) {
                    binding.containerLayout.alpha = 1.0f
                    binding.textViewTargetStatus.text = "Stopwatch is running..."
                    binding.buttonAddLog.visibility = View.GONE
                    binding.textViewRunningTimer.visibility = View.VISIBLE
                    binding.textViewRunningTimer.text = formatTime(currentTime)
                    binding.buttonStart.visibility = View.GONE
                    binding.layoutRunningControls.visibility = View.VISIBLE
                    binding.buttonStop.setOnClickListener { onStopClick() }
                } else {
                    binding.containerLayout.alpha = 0.5f
                    binding.buttonStart.isEnabled = false
                }
            } else {
                binding.containerLayout.alpha = 1.0f
                binding.textViewTargetStatus.text = "Target: ${formatTime(activity.targetDurationSeconds)}"
                binding.buttonAddLog.visibility = View.VISIBLE
                binding.textViewRunningTimer.visibility = View.GONE
                binding.buttonStart.visibility = View.VISIBLE
                binding.buttonStart.isEnabled = true
                binding.layoutRunningControls.visibility = View.GONE
                binding.buttonStart.setOnClickListener { onStartClick(activity) }
            }
        }

        private fun formatTime(seconds: Int): String {
            val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }
}

class ActivityDiffCallback : DiffUtil.ItemCallback<Activity>() {
    override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean = oldItem == newItem
}