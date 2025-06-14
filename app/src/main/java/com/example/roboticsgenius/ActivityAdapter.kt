// app/src/main/java/com/example/roboticsgenius/ActivityAdapter.kt
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
    private val onStopClick: () -> Unit,
    private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
    private val onCancelClick: () -> Unit
) : ListAdapter<ActivityUiModel, ActivityAdapter.ActivityViewHolder>(ActivityUiModelDiffCallback()) {

    // Real-time state from the TimerService
    private var activeId: Int? = null
    private var currentTime: Int = 0
    private var isPaused: Boolean = false

    fun setActiveTimerState(activeId: Int? = this.activeId, time: Int? = null, isPaused: Boolean? = null) {
        val oldActiveId = this.activeId
        val oldIsPaused = this.isPaused

        this.activeId = activeId
        time?.let { this.currentTime = it }
        isPaused?.let { this.isPaused = it }

        if (oldActiveId != this.activeId || oldIsPaused != this.isPaused) {
            // A major state change occurred, redraw all visible items.
            notifyDataSetChanged()
        } else if (activeId != null && !this.isPaused) {
            // Only the time changed, find the active view holder and update it efficiently.
            val activePosition = currentList.indexOfFirst { it.activity.id == this.activeId }
            if (activePosition != -1) {
                notifyItemChanged(activePosition, PAYLOAD_TIME_UPDATE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding, onStartClick, onStopClick, onPauseResumeClick, onCancelClick)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position), activeId, currentTime, isPaused)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_TIME_UPDATE)) {
            holder.updateTimer(currentTime)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    class ActivityViewHolder(
        private val binding: ItemActivityBinding,
        private val onStartClick: (Activity) -> Unit,
        private val onStopClick: () -> Unit,
        private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
        private val onCancelClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun updateTimer(time: Int) {
            binding.textViewRunningTimer.text = formatTime(time)
        }

        fun bind(
            uiModel: ActivityUiModel,
            activeId: Int?,
            currentTime: Int,
            isPaused: Boolean
        ) {
            val activity = uiModel.activity

            // --- Bind data from UiModel ---
            binding.textViewActivityName.text = activity.name
            binding.textViewTargetStatus.text = uiModel.statusText
            binding.textViewStreak.text = "🔥 ${uiModel.streakCount}"

            (binding.cardView as MaterialCardView).strokeColor = Color.parseColor(activity.color)
            (binding.textViewStreak.background as GradientDrawable).setColor(Color.parseColor(activity.color))

            // --- Dynamic State Binding based on TimerService ---
            val isTimerRunningForAnyActivity = activeId != null
            val isThisCardTheActiveOne = isTimerRunningForAnyActivity && activeId == activity.id

            binding.containerLayout.alpha = if (isTimerRunningForAnyActivity && !isThisCardTheActiveOne) 0.5f else 1.0f
            binding.buttonStart.isEnabled = !isTimerRunningForAnyActivity

            if (isThisCardTheActiveOne) {
                // --- THIS is the active card ---
                if (isPaused) {
                    binding.textViewTargetStatus.text = "Paused"
                } // When running, the status text is replaced by the timer value below

                binding.buttonAddLog.visibility = View.GONE
                binding.textViewRunningTimer.visibility = View.VISIBLE
                binding.textViewRunningTimer.text = formatTime(currentTime)
                binding.buttonStart.visibility = View.GONE
                binding.layoutRunningControls.visibility = View.VISIBLE

                binding.buttonCancel.setOnClickListener { onCancelClick() }
                binding.buttonStop.setOnClickListener { onStopClick() }
                binding.buttonPause.setOnClickListener { onPauseResumeClick(isPaused) }

                if (isPaused) {
                    binding.buttonPause.setIconResource(android.R.drawable.ic_media_play)
                    binding.buttonPause.contentDescription = itemView.context.getString(R.string.resume_timer)
                } else {
                    binding.buttonPause.setIconResource(android.R.drawable.ic_media_pause)
                    binding.buttonPause.contentDescription = itemView.context.getString(R.string.pause_timer)
                }
            } else {
                // --- This is an INACTIVE card ---
                // statusText is already set from the uiModel above
                binding.buttonAddLog.visibility = View.VISIBLE
                binding.textViewRunningTimer.visibility = View.GONE
                binding.buttonStart.visibility = View.VISIBLE
                binding.layoutRunningControls.visibility = View.GONE
                binding.buttonStart.setOnClickListener { onStartClick(activity) }
            }
        }

        private fun formatTime(seconds: Int): String {
            val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }

    companion object {
        const val PAYLOAD_TIME_UPDATE = "PAYLOAD_TIME_UPDATE"
    }
}

class ActivityUiModelDiffCallback : DiffUtil.ItemCallback<ActivityUiModel>() {
    override fun areItemsTheSame(oldItem: ActivityUiModel, newItem: ActivityUiModel): Boolean =
        oldItem.activity.id == newItem.activity.id

    override fun areContentsTheSame(oldItem: ActivityUiModel, newItem: ActivityUiModel): Boolean =
        oldItem == newItem
}