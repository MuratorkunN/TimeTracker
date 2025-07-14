package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentSettingsBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var deletionAdapter: SettingsDeletionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        observeSettings()
    }

    private fun observeSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            val format = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
            viewModel.appStartDate.collectLatest { calendar ->
                binding.appStartDateValue.text = format.format(calendar.time)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deletionItems.collectLatest { items ->
                deletionAdapter.submitList(items)
            }
        }
    }

    private fun setupRecyclerView() {
        deletionAdapter = SettingsDeletionAdapter(
            onDataSetDelete = { dataSet ->
                showConfirmationDialog(
                    "Delete Dataset '${dataSet.name}'?",
                    "This will also delete all associated activities and their data. This cannot be undone."
                ) {
                    viewModel.deleteDataSet(dataSet)
                }
            },
            onActivityDelete = { activity ->
                showConfirmationDialog(
                    "Delete Activity '${activity.name}'?",
                    "This will delete the activity and all its logged data. This cannot be undone."
                ) {
                    viewModel.deleteActivity(activity)
                }
            }
        )
        binding.recyclerViewDelete.apply {
            adapter = deletionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            // we keep nestedScrollingEnabled="false" in XML so parent ScrollView won't intercept
        }
    }

    private fun setupClickListeners() {
        binding.settingAppStartDate.setOnClickListener { showStartDatePicker() }
    }

    private fun showStartDatePicker() {
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val currentStartDate = viewModel.appStartDate.value

        val constraints = CalendarConstraints.Builder()
            .setEnd(today)
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select First Day for Logging")
            .setSelection(currentStartDate.timeInMillis)
            .setCalendarConstraints(constraints)
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val newStartDate = Calendar.getInstance().apply { timeInMillis = selection }
            viewModel.setAppStartDate(newStartDate)
        }
        picker.show(parentFragmentManager, "AppStartDatePicker")
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ -> onConfirm() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
