// app/src/main/java/com/example/roboticsgenius/DataSetAdapter.kt

package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemDataSetChoiceBinding

class DataSetAdapter(
    private val onDataSetSelected: (Int) -> Unit,
    private val onAddDataSetClicked: () -> Unit
) : ListAdapter<DataSetAdapter.DataSetChoice, DataSetAdapter.DataSetViewHolder>(DataSetDiffCallback()) {

    sealed class DataSetChoice {
        data class Item(val dataSet: DataSet) : DataSetChoice()
        object AddButton : DataSetChoice()
    }

    private var selectedDataSetId: Int? = null

    // THIS IS THE FIX: A new function to handle the selection change and force a redraw.
    fun updateSelection(newId: Int) {
        if (newId != selectedDataSetId) {
            selectedDataSetId = newId
            notifyDataSetChanged() // This forces the RecyclerView to redraw all items immediately.
        }
    }

    fun submitAndSelect(dataSets: List<DataSet>, selectedId: Int?) {
        val choices = dataSets.map { DataSetChoice.Item(it) } + DataSetChoice.AddButton
        selectedDataSetId = selectedId
        submitList(choices)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataSetViewHolder {
        val binding = ItemDataSetChoiceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DataSetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataSetViewHolder, position: Int) {
        when (val item = getItem(position)) {
            // The click listener now also calls our new updateSelection function.
            is DataSetChoice.Item -> holder.bind(item.dataSet, item.dataSet.id == selectedDataSetId) {
                onDataSetSelected(it)
                updateSelection(it)
            }
            is DataSetChoice.AddButton -> holder.bindAddButton(onAddDataSetClicked)
        }
    }

    class DataSetViewHolder(private val binding: ItemDataSetChoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        // The bind function now takes the click listener lambda directly.
        fun bind(dataSet: DataSet, isSelected: Boolean, onDataSetClicked: (Int) -> Unit) {
            binding.textViewName.text = dataSet.name
            binding.imageViewIcon.setImageResource(android.R.drawable.ic_menu_agenda) // Placeholder
            binding.cardDataSet.setCardBackgroundColor(Color.parseColor(dataSet.color))

            if (isSelected) {
                binding.cardDataSet.strokeColor = ContextCompat.getColor(itemView.context, R.color.white)
                binding.cardDataSet.cardElevation = 4f
            } else {
                binding.cardDataSet.strokeColor = Color.TRANSPARENT
                binding.cardDataSet.cardElevation = 0f
            }

            itemView.setOnClickListener { onDataSetClicked(dataSet.id) }
        }

        fun bindAddButton(onAddDataSetClicked: () -> Unit) {
            binding.textViewName.text = "Add New"
            binding.imageViewIcon.setImageResource(android.R.drawable.ic_menu_add)
            binding.cardDataSet.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.chip_unchecked_background))
            binding.cardDataSet.strokeColor = Color.TRANSPARENT
            binding.cardDataSet.cardElevation = 0f
            itemView.setOnClickListener { onAddDataSetClicked() }
        }
    }

    class DataSetDiffCallback : DiffUtil.ItemCallback<DataSetChoice>() {
        override fun areItemsTheSame(oldItem: DataSetChoice, newItem: DataSetChoice): Boolean {
            return if (oldItem is DataSetChoice.Item && newItem is DataSetChoice.Item) {
                oldItem.dataSet.id == newItem.dataSet.id
            } else {
                oldItem is DataSetChoice.AddButton && newItem is DataSetChoice.AddButton
            }
        }
        override fun areContentsTheSame(oldItem: DataSetChoice, newItem: DataSetChoice): Boolean {
            return if (oldItem is DataSetChoice.Item && newItem is DataSetChoice.Item) {
                oldItem.dataSet == newItem.dataSet
            } else {
                true
            }
        }
    }
}