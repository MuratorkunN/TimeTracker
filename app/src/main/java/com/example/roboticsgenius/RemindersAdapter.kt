package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemReminderBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RemindersAdapter(
    private val onReminderClick: (Reminder) -> Unit
) : ListAdapter<Reminder, RemindersAdapter.ReminderViewHolder>(ReminderDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val binding = ItemReminderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReminderViewHolder(binding, onReminderClick)
    }

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReminderViewHolder(
        private val binding: ItemReminderBinding,
        private val onReminderClick: (Reminder) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")

        fun bind(reminder: Reminder) {
            binding.textViewReminderMessage.text = reminder.message
            // THE FIX: Correctly cast to MaterialCardView to set the stroke color
            (binding.cardView).strokeColor = Color.parseColor(reminder.color)


            val calendar = Calendar.getInstance().apply { timeInMillis = reminder.nextTriggerTime }
            binding.textViewNextDate.text = dateFormat.format(calendar.time)
            binding.textViewNextTime.text = timeFormat.format(calendar.time)

            binding.daysContainer.removeAllViews()
            if (reminder.repeatDays > 0) {
                // Display repeating days
                for (i in 0..6) {
                    val dayBit = 1 shl i // SUN=1, MON=2, TUE=4, etc.
                    val isDaySelected = (reminder.repeatDays and dayBit) != 0
                    val dayView = createDayView(isDaySelected, dayLabels[i])
                    binding.daysContainer.addView(dayView)
                }
            } else {
                val noRepeat = TextView(itemView.context).apply {
                    text = "Does not repeat"
                    // THE FIX: Use the locally defined, reliable style
                    setTextAppearance(R.style.TextAppearance_App_BodySmall)
                    alpha = 0.7f
                }
                binding.daysContainer.addView(noRepeat)
            }

            itemView.setOnClickListener {
                onReminderClick(reminder)
            }
        }

        private fun createDayView(isSelected: Boolean, label: String): TextView {
            return TextView(itemView.context).apply {
                text = label
                gravity = Gravity.CENTER
                minWidth = (24 * resources.displayMetrics.density).toInt()
                minHeight = (24 * resources.displayMetrics.density).toInt()
                // THE FIX: Use the locally defined, reliable style
                setTextAppearance(R.style.TextAppearance_App_LabelSmall)

                val layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParams.marginEnd = (4 * resources.displayMetrics.density).toInt()
                this.layoutParams = layoutParams

                if (isSelected) {
                    background = ContextCompat.getDrawable(context, R.drawable.circle_background)
                    (background as? GradientDrawable)?.setColor(Color.parseColor("#007AFF"))
                    setTextColor(Color.WHITE)
                } else {
                    background = ContextCompat.getDrawable(context, R.drawable.circle_background)
                    (background as? GradientDrawable)?.let {
                        it.setStroke((1 * resources.displayMetrics.density).toInt(), Color.parseColor("#007AFF"))
                        it.setColor(Color.TRANSPARENT)
                    }
                    setTextColor(Color.parseColor("#007AFF"))
                }
            }
        }
    }
}

class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {
    override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean = oldItem == newItem
}