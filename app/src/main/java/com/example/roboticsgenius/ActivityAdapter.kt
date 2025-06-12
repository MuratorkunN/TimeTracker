package com.example.roboticsgenius

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemActivityBinding

class ActivityAdapter : ListAdapter<Activity, ActivityAdapter.ActivityViewHolder>(ActivityDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityViewHolder {
        val binding = ItemActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivityViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ActivityViewHolder, position: Int) {
        val activity = getItem(position)
        holder.bind(activity)
    }

    class ActivityViewHolder(private val binding: ItemActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.textViewActivityName.text = activity.name
            // We will add button logic here later
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