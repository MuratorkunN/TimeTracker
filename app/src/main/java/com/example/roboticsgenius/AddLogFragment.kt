// app/src/main/java/com/example/roboticsgenius/AddLogFragment.kt

package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.roboticsgenius.databinding.FragmentAddLogBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddLogFragment : DialogFragment() {

    private var _binding: FragmentAddLogBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by activityViewModels()
    private var activityToLog: Activity? = null
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    companion object {
        private const val ARG_ACTIVITY_ID = "activity_id"
        fun newInstance(activityId: Int): AddLogFragment {
            val args = Bundle().apply { putInt(ARG_ACTIVITY_ID, activityId) }
            return AddLogFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityId = arguments?.getInt(ARG_ACTIVITY_ID) ?: -1

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            activityToLog = db.activityDao().getActivityById(activityId)
            activityToLog?.let { binding.textViewAddLogTitle.text = "Add Log for '${it.name}'" }
        }

        setupNumberPickers()
        updateDateTimeButtons()
        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener { saveLog() }
        binding.btnPickDate.setOnClickListener { showDatePicker() }
        binding.btnPickTime.setOnClickListener { showTimePicker() }
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0; binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0; binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0; binding.pickerSeconds.maxValue = 59
    }

    private fun updateDateTimeButtons() {
        binding.btnPickDate.text = dateFormat.format(calendar.time)
        binding.btnPickTime.text = timeFormat.format(calendar.time)
    }

    private fun showDatePicker() {
        // THE FIX: Use setEnd() to disable future dates. This is the correct method.
        val constraintsBuilder = CalendarConstraints.Builder()
            .setEnd(MaterialDatePicker.todayInUtcMilliseconds())

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select End Date")
            .setSelection(calendar.timeInMillis)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // Since MaterialDatePicker works in UTC, we need to account for the timezone offset
            // to prevent selecting "today" from being considered in the future on the device.
            val selectedUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            selectedUtc.timeInMillis = selection

            calendar.set(Calendar.YEAR, selectedUtc.get(Calendar.YEAR))
            calendar.set(Calendar.MONTH, selectedUtc.get(Calendar.MONTH))
            calendar.set(Calendar.DAY_OF_MONTH, selectedUtc.get(Calendar.DAY_OF_MONTH))

            // If the selected date is today, and the user-set time is in the future,
            // reset the time to now to prevent invalid states.
            val now = Calendar.getInstance()
            if (isSameDay(calendar, now) && calendar.after(now)) {
                calendar.timeInMillis = now.timeInMillis
            }

            updateDateTimeButtons()
        }
        datePicker.show(parentFragmentManager, "DatePicker")
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY))
            .setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select End Time")
            .build()

        timePicker.addOnPositiveButtonClickListener {
            val now = Calendar.getInstance()
            // Create a temporary calendar to test the new time
            val tempCal = calendar.clone() as Calendar
            tempCal.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            tempCal.set(Calendar.MINUTE, timePicker.minute)

            if (isSameDay(calendar, now) && tempCal.after(now)) {
                Toast.makeText(context, "Cannot select a future time", Toast.LENGTH_SHORT).show()
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
                calendar.set(Calendar.MINUTE, timePicker.minute)
                updateDateTimeButtons()
            }
        }
        timePicker.show(parentFragmentManager, "TimePicker")
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun saveLog() {
        val activity = activityToLog ?: return
        val durationSeconds = binding.pickerHours.value * 3600 + binding.pickerMinutes.value * 60 + binding.pickerSeconds.value

        if (durationSeconds <= 0) {
            Toast.makeText(context, "Duration must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        val endTimeMillis = calendar.timeInMillis
        if (endTimeMillis > System.currentTimeMillis()) {
            Toast.makeText(context, "Cannot log time in the future", Toast.LENGTH_SHORT).show()
            return
        }

        val startTimeMillis = endTimeMillis - (durationSeconds * 1000L)

        viewModel.addManualLog(activity.id, startTimeMillis, durationSeconds)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}