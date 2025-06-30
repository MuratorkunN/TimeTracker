package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemReorderBinding
import java.util.Collections

class ReorderAdapter : ListAdapter<Activity, ReorderAdapter.ReorderViewHolder>(ReorderDiffCallback()) {

    // Function to handle the item move for drag and drop
    fun onItemMove(fromPosition: Int, toPosition: Int) {
        val currentList = currentList.toMutableList()
        Collections.swap(currentList, fromPosition, toPosition)
        submitList(currentList) // Update the list in the adapter
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReorderViewHolder {
        val binding = ItemReorderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReorderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReorderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReorderViewHolder(private val binding: ItemReorderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.activityNameText.text = activity.name
            // Set the outline color
            (binding.root.background as? GradientDrawable)?.setStroke(
                (2 * itemView.resources.displayMetrics.density).toInt(),
                Color.parseColor(activity.color)
            )
        }
    }
}

class ReorderDiffCallback : DiffUtil.ItemCallback<Activity>() {
    override fun areItemsTheSame(oldItem: Activity, newItem: Activity): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: Activity, newItem: Activity): Boolean {
        return oldItem == newItem
    }
}