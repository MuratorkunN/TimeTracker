package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemActivityBinding
import com.google.android.material.card.MaterialCardView

class ActivityAdapter(private val onStartClick: (Activity) -> Unit) : ListAdapter<Activity, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ActivityViewHolder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.textViewActivityName.text = activity.name

            // Set the card's outline color
            (binding.cardView as MaterialCardView).strokeColor = Color.parseColor(activity.color)

            // We will add the logic for target/status text and streak later.
            // For now, just set some placeholder text.
            binding.textViewTargetStatus.text = "${activity.targetPeriod} Target"
            binding.textViewStreak.text = "🔥 0"

            // Make the start button work again
            binding.buttonStart.setOnClickListener {
                onStartClick(activity)
            }
        }
    }
}

class ActivityDiffCallback : DiffUtil.ItemCallback<Activity>() {
    override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
        return oldItem == newItem
    }
}