package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemInsightsSummaryBinding

class InsightsAdapter : ListAdapter<InsightSummaryItem, InsightsAdapter.SummaryViewHolder>(SummaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val binding = ItemInsightsSummaryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SummaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SummaryViewHolder(private val binding: ItemInsightsSummaryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: InsightSummaryItem) {
            binding.colorIndicator.setBackgroundColor(Color.parseColor(item.color))
            binding.textViewActivityName.text = item.activityName
            binding.textViewTotalDuration.text = item.totalDurationFormatted
        }
    }
}

class SummaryDiffCallback : DiffUtil.ItemCallback<InsightSummaryItem>() {
    override fun areItemsTheSame(oldItem: InsightSummaryItem, newItem: InsightSummaryItem): Boolean = oldItem.activityName == newItem.activityName
    override fun areContentsTheSame(oldItem: InsightSummaryItem, newItem: InsightSummaryItem): Boolean = oldItem == newItem
}