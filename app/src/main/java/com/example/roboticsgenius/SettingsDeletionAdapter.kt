package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemSettingsDeleteActivityBinding
import com.example.roboticsgenius.databinding.ItemSettingsDeleteDatasetBinding

class SettingsDeletionAdapter(
    private val onDataSetDelete: (DataSet) -> Unit,
    private val onActivityDelete: (Activity) -> Unit
) : ListAdapter<DeletionItem, RecyclerView.ViewHolder>(DeletionDiffCallback()) {

    private val VIEW_TYPE_DATASET = 1
    private val VIEW_TYPE_ACTIVITY = 2

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DeletionItem.DataSetItem -> VIEW_TYPE_DATASET
            is DeletionItem.ActivityItem -> VIEW_TYPE_ACTIVITY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_DATASET -> {
                val binding = ItemSettingsDeleteDatasetBinding.inflate(inflater, parent, false)
                DataSetViewHolder(binding, onDataSetDelete)
            }
            VIEW_TYPE_ACTIVITY -> {
                val binding = ItemSettingsDeleteActivityBinding.inflate(inflater, parent, false)
                ActivityViewHolder(binding, onActivityDelete)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is DeletionItem.DataSetItem -> (holder as DataSetViewHolder).bind(item.dataSet)
            is DeletionItem.ActivityItem -> (holder as ActivityViewHolder).bind(item.activity)
        }
    }

    // ViewHolder for DataSets, using the new header layout
    class DataSetViewHolder(
        private val binding: ItemSettingsDeleteDatasetBinding,
        private val onDataSetDelete: (DataSet) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(dataSet: DataSet) {
            binding.textViewName.text = dataSet.name
            binding.colorIndicator.setBackgroundColor(Color.parseColor(dataSet.color))
            binding.buttonDelete.setOnClickListener { onDataSetDelete(dataSet) }
        }
    }

    // ViewHolder for Activities, using the indented card layout
    class ActivityViewHolder(
        private val binding: ItemSettingsDeleteActivityBinding,
        private val onActivityDelete: (Activity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(activity: Activity) {
            binding.textViewName.text = activity.name
            (binding.cardView).apply {
                strokeColor = Color.parseColor(activity.color)
                val backgroundColor = context.resolveThemeColor(com.google.android.material.R.attr.colorSurfaceContainer)
                setCardBackgroundColor(backgroundColor)
            }
            binding.buttonDelete.setOnClickListener { onActivityDelete(activity) }
        }
    }
}

class DeletionDiffCallback : DiffUtil.ItemCallback<DeletionItem>() {
    override fun areItemsTheSame(oldItem: DeletionItem, newItem: DeletionItem): Boolean {
        return when {
            oldItem is DeletionItem.DataSetItem && newItem is DeletionItem.DataSetItem -> oldItem.dataSet.id == newItem.dataSet.id
            oldItem is DeletionItem.ActivityItem && newItem is DeletionItem.ActivityItem -> oldItem.activity.id == newItem.activity.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: DeletionItem, newItem: DeletionItem): Boolean {
        return oldItem == newItem
    }
}