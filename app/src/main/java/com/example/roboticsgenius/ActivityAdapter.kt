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
    private val onStartClick: (ActivityUiModel) -> Unit,
    private val onStopClick: () -> Unit,
    private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
    private val onCancelClick: () -> Unit,
    private val onEditTargetClick: (ActivityUiModel) -> Unit,
    private val onAddLogClick: (ActivityUiModel) -> Unit // NEW
) : ListAdapter<ActivityUiModel, ActivityAdapter.ActivityViewHolder>(ActivityUiModelDiffCallback()) {

    private var activeId: Int? = null
    private var currentTime: Int = 0
    private var isPaused: Boolean = false

    fun submitList(list: List<ActivityUiModel>?, a: Boolean) {
        super.submitList(list)
    }

    fun setActiveTimerState(activeId: Int? = this.activeId, time: Int? = null, isPaused: Boolean? = null) {
        val oldActiveId = this.activeId
        val oldIsPaused = this.isPaused
        this.activeId = activeId
        time?.let { this.currentTime = it }
        isPaused?.let { this.isPaused = it }
        val fullUpdateNeeded = oldActiveId != this.activeId || (this.activeId != null && oldIsPaused != this.isPaused)
        if (fullUpdateNeeded) {
            notifyDataSetChanged()
        } else if (activeId != null && !this.isPaused) {
            val activePosition = currentList.indexOfFirst { it.activity.id == this.activeId }
            if (activePosition != -1) {
                notifyItemChanged(activePosition, PAYLOAD_TIME_UPDATE)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding, onStartClick, onStopClick, onPauseResumeClick, onCancelClick, onEditTargetClick, onAddLogClick)
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
        private val onStartClick: (ActivityUiModel) -> Unit,
        private val onStopClick: () -> Unit,
        private val onPauseResumeClick: (isPaused: Boolean) -> Unit,
        private val onCancelClick: () -> Unit,
        private val onEditTargetClick: (ActivityUiModel) -> Unit,
        private val onAddLogClick: (ActivityUiModel) -> Unit // NEW
    ) : RecyclerView.ViewHolder(binding.root) {

        fun updateTimer(time: Int) {
            binding.textViewRunningTimer.text = formatTime(time)
        }

        fun bind(
            uiModel: ActivityUiModel, activeId: Int?, currentTime: Int, isPaused: Boolean
        ) {
            val activity = uiModel.activity
            binding.textViewActivityName.text = activity.name
            binding.textViewTargetStatus.text = uiModel.statusText
            binding.textViewStreak.text = "🔥 ${uiModel.streakCount}"
            (binding.cardView as MaterialCardView).strokeColor = Color.parseColor(activity.color)
            (binding.textViewStreak.background as GradientDrawable).setColor(Color.parseColor(activity.color))
            val isTimerRunningForAnyActivity = activeId != null
            val isThisCardTheActiveOne = isTimerRunningForAnyActivity && activeId == activity.id

            binding.textViewTargetStatus.setOnClickListener {
                if (!isTimerRunningForAnyActivity) { onEditTargetClick(uiModel) }
            }

            // NEW: Set click listener for Add Log button
            binding.buttonAddLog.setOnClickListener { onAddLogClick(uiModel) }

            binding.containerLayout.alpha = if (isTimerRunningForAnyActivity && !isThisCardTheActiveOne) 0.5f else 1.0f
            binding.buttonStart.isEnabled = !isTimerRunningForAnyActivity
            if (isThisCardTheActiveOne) {
                binding.textViewTargetStatus.text = if (isPaused) "Paused" else uiModel.statusText
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
                binding.buttonAddLog.visibility = View.VISIBLE
                binding.textViewRunningTimer.visibility = View.GONE
                binding.buttonStart.visibility = View.VISIBLE
                binding.layoutRunningControls.visibility = View.GONE
                binding.buttonStart.setOnClickListener { onStartClick(uiModel) }
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