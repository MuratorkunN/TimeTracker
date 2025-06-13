package com.example.roboticsgenius

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.example.roboticsgenius.databinding.FragmentAddActivityBinding
import kotlinx.coroutines.launch

class AddActivityFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase

    private val colors = mapOf(
        "Green" to "#4CAF50", "Red" to "#F44336", "Purple" to "#9C27B0",
        "Blue" to "#2196F3", "Yellow" to "#FFEB3B", "Pink" to "#E91E63"
    )
    private var selectedColorHex: String = colors.values.first()

    // THE BROKEN THEME CODE IS GONE.

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddActivityBinding.inflate(inflater, container, false)
        db = AppDatabase.getDatabase(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupColorChips()
        setupNumberPickers()
        setupQuickButtons()
        setupPeriodToggle()
        setupCreateButton()
    }

    // In AddActivityFragment.kt
    private fun setupColorChips() {
        colors.forEach { (name, hex) ->
            val chip = Chip(context).apply {
                text = name
                isCheckable = true
                // THIS IS THE SIMPLE FIX:
                // Set the background color directly. We'll handle the "checked" state manually.
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(Color.parseColor(hex))

                // We can remove the stroke for now to simplify
                chipStrokeWidth = 0f

                setTextColor(Color.parseColor("#FFFFFF"))

                // The listener now also handles visual changes
                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedColorHex = hex
                        // Make the selected chip stand out
                        chipStrokeColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
                        chipStrokeWidth = 4f // Use a thicker stroke to show selection
                    } else {
                        // Revert to no stroke when unchecked
                        chipStrokeWidth = 0f
                    }
                }
            }
            binding.chipGroupColor.addView(chip)
            if (name == "Green") {
                chip.isChecked = true // Default selection
            }
        }
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0
        binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0
        binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0
        binding.pickerSeconds.maxValue = 59
    }

    private fun setupQuickButtons() {
        binding.btn5min.setOnClickListener { setTime(0, 5, 0) }
        binding.btn10min.setOnClickListener { setTime(0, 10, 0) }
        binding.btn30min.setOnClickListener { setTime(0, 30, 0) }
        binding.btn1hr.setOnClickListener { setTime(1, 0, 0) }
    }

    private fun setTime(h: Int, m: Int, s: Int) {
        binding.pickerHours.value = h
        binding.pickerMinutes.value = m
        binding.pickerSeconds.value = s
    }

    private fun setupPeriodToggle() {
        binding.toggleGroupPeriod.check(R.id.btnWeekly)
    }

    private fun setupCreateButton() {
        binding.btnCreateActivity.setOnClickListener {
            val name = binding.editTextActivityName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(context, "Activity name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val duration = binding.pickerHours.value * 3600 + binding.pickerMinutes.value * 60 + binding.pickerSeconds.value
            val period = when (binding.toggleGroupPeriod.checkedButtonId) {
                R.id.btnDaily -> "Daily"
                R.id.btnWeekly -> "Weekly"
                R.id.btnMonthly -> "Monthly"
                else -> "Weekly"
            }

            val newActivity = Activity(
                name = name,
                color = selectedColorHex,
                targetDurationSeconds = duration,
                targetPeriod = period
            )

            lifecycleScope.launch {
                db.activityDao().insert(newActivity)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}