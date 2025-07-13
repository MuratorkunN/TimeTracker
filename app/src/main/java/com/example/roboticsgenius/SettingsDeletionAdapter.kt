package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemSettingsDeleteBinding
import com.google.android.material.card.MaterialCardView

class SettingsDeletionAdapter(
    private val onDataSetDelete: (DataSet) -> Unit,
    private val onActivityDelete: (Activity) -> Unit
) : ListAdapter<DeletionItem, SettingsDeletionAdapter.DeletionViewHolder>(DeletionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeletionViewHolder {
        val binding = ItemSettingsDeleteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeletionViewHolder(binding, onDataSetDelete, onActivityDelete)
    }

    override fun onBindViewHolder(holder: DeletionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DeletionViewHolder(
        private val binding: ItemSettingsDeleteBinding,
        private val onDataSetDelete: (DataSet) -> Unit,
        private val onActivityDelete: (Activity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DeletionItem) {
            when (item) {
                is DeletionItem.DataSetItem -> bindDataSet(item.dataSet)
                is DeletionItem.ActivityItem -> bindActivity(item.activity)
            }
        }

        private fun bindDataSet(dataSet: DataSet) {
            binding.textViewName.text = dataSet.name
            (binding.cardView as MaterialCardView).apply {
                strokeColor = Color.parseColor(dataSet.color)
                // THE FIX: Resolve the color from the theme attributes
                val backgroundColor = context.resolveThemeColor(com.google.android.material.R.attr.colorSurfaceContainerHigh)
                setCardBackgroundColor(backgroundColor)
            }
            binding.buttonDelete.setOnClickListener { onDataSetDelete(dataSet) }
        }

        private fun bindActivity(activity: Activity) {
            val margin = (32 * itemView.resources.displayMetrics.density).toInt()
            (binding.cardView.layoutParams as ViewGroup.MarginLayoutParams).setMargins(margin, 0, 0, margin/2)

            binding.textViewName.text = activity.name
            (binding.cardView as MaterialCardView).apply {
                strokeColor = Color.parseColor(activity.color)
                // THE FIX: Resolve the color from the theme attributes
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