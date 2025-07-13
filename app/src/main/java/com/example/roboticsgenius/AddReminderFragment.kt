package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.roboticsgenius.databinding.DialogAddReminderBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddReminderFragment : BottomSheetDialogFragment() {
    private var _binding: DialogAddReminderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RemindersViewModel by activityViewModels()
    private var existingReminder: Reminder? = null

    private var selectedColorHex: String = "#007AFF"
    private val selectedDateTime = Calendar.getInstance()

    companion object {
        private const val ARG_REMINDER_ID = "reminder_id"
        fun newInstance(reminderId: Int? = null): AddReminderFragment {
            return AddReminderFragment().apply {
                arguments = Bundle().apply {
                    reminderId?.let { putInt(ARG_REMINDER_ID, it) }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPickers()
        updateDateTimeButton()
        updateColorPreview()
        setupListeners()

        val reminderId = arguments?.getInt(ARG_REMINDER_ID)
        if (reminderId != null && reminderId != 0) {
            loadExistingReminder(reminderId)
        }
    }

    private fun loadExistingReminder(reminderId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            existingReminder = viewModel.getReminderById(reminderId)
            existingReminder?.let { reminder ->
                binding.textViewTitle.text = "Edit Reminder"
                binding.btnSave.text = "Apply"
                binding.btnDelete.isVisible = true

                binding.editTextReminderMessage.setText(reminder.message)
                selectedDateTime.timeInMillis = reminder.nextTriggerTime
                updateDateTimeButton()
                binding.pickerHour.value = selectedDateTime.get(Calendar.HOUR_OF_DAY)
                binding.pickerMinute.value = selectedDateTime.get(Calendar.MINUTE)

                selectedColorHex = reminder.color
                updateColorPreview()

                // Set repeat day chips
                for (i in 0..6) {
                    val chip = binding.chipGroupDays.getChildAt(i) as Chip
                    val dayBit = 1 shl i
                    chip.isChecked = (reminder.repeatDays and dayBit) != 0
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { saveReminder() }
        binding.btnDelete.setOnClickListener { deleteReminder() }

        binding.colorSectionContainer.setOnClickListener {
            showRgbColorPickerDialog(
                title = "Select Reminder Color",
                initialColor = selectedColorHex
            ) { newColorHex ->
                selectedColorHex = newColorHex
                updateColorPreview()
            }
        }
    }

    private fun deleteReminder() {
        existingReminder?.let {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    viewModel.deleteReminder(it.id)
                    dismiss()
                }
                .show()
        }
    }

    private fun saveReminder() {
        val message = binding.editTextReminderMessage.text.toString()
        if (message.isBlank()) {
            Toast.makeText(requireContext(), "Reminder text cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        selectedDateTime.set(Calendar.HOUR_OF_DAY, binding.pickerHour.value)
        selectedDateTime.set(Calendar.MINUTE, binding.pickerMinute.value)

        var repeatDays = 0
        val dayChips = listOf(
            binding.chipSun, binding.chipMon, binding.chipTue, binding.chipWed,
            binding.chipThu, binding.chipFri, binding.chipSat
        )
        dayChips.forEachIndexed { index, chip ->
            if (chip.isChecked) {
                repeatDays = repeatDays or (1 shl index)
            }
        }

        val nextTrigger = ReminderManager.calculateNextTriggerTime(selectedDateTime.timeInMillis, repeatDays)

        val reminderToSave = existingReminder?.copy(
            message = message,
            nextTriggerTime = nextTrigger,
            repeatDays = repeatDays,
            color = selectedColorHex
        ) ?: Reminder(
            message = message,
            nextTriggerTime = nextTrigger,
            repeatDays = repeatDays,
            color = selectedColorHex
        )

        viewModel.upsertReminder(reminderToSave)
        dismiss()
    }

    private fun setupPickers() {
        binding.pickerHour.minValue = 0
        binding.pickerHour.maxValue = 23
        binding.pickerMinute.minValue = 0
        binding.pickerMinute.maxValue = 59

        binding.pickerHour.value = selectedDateTime.get(Calendar.HOUR_OF_DAY)
        binding.pickerMinute.value = selectedDateTime.get(Calendar.MINUTE)
    }

    private fun updateDateTimeButton() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.btnPickDate.text = dateFormat.format(selectedDateTime.time)
    }

    private fun showDatePicker() {
        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(selectedDateTime.timeInMillis)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
            selectedDateTime.set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH)
            )
            updateDateTimeButton()
        }
        datePicker.show(parentFragmentManager, "DatePicker")
    }

    private fun updateColorPreview() {
        (binding.viewColorPreview.background as? GradientDrawable)?.setColor(Color.parseColor(selectedColorHex))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showRgbColorPickerDialog(title: String, initialColor: String, onColorSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rgb_color_picker, null)
        val previewView = dialogView.findViewById<View>(R.id.color_preview)
        val hexTextView = dialogView.findViewById<TextView>(R.id.hex_text)
        val rSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_r)
        val gSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_g)
        val bSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_b)
        val vSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_v)
        var color = Color.parseColor(initialColor)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        rSeekBar.progress = Color.red(color)
        gSeekBar.progress = Color.green(color)
        bSeekBar.progress = Color.blue(color)
        vSeekBar.progress = (hsv[2] * 255).toInt()
        fun updateColor() {
            val r = rSeekBar.progress
            val g = gSeekBar.progress
            val b = bSeekBar.progress
            Color.RGBToHSV(r, g, b, hsv)
            hsv[2] = vSeekBar.progress / 255f
            color = Color.HSVToColor(hsv)
            previewView.setBackgroundColor(color)
            hexTextView.text = String.format("#%06X", 0xFFFFFF and color)
        }
        updateColor()
        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) updateColor() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        rSeekBar.setOnSeekBarChangeListener(listener)
        gSeekBar.setOnSeekBarChangeListener(listener)
        bSeekBar.setOnSeekBarChangeListener(listener)
        vSeekBar.setOnSeekBarChangeListener(listener)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                onColorSelected(String.format("#%06X", 0xFFFFFF and color))
            }
            .show()
    }
}