package com.example.roboticsgenius

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentRemindersBinding
import kotlinx.coroutines.launch

class RemindersFragment : Fragment() {

    private var _binding: FragmentRemindersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RemindersViewModel by activityViewModels()
    private lateinit var remindersAdapter: RemindersAdapter

    // THE FIX: ActivityResultLauncher for the notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted. No action needed, system will now allow notifications.
        } else {
            // Optionally, you can show a message explaining why the feature is limited.
            // For now, we do nothing if denied.
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRemindersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        askNotificationPermission() // THE FIX: Ask for permission when the view is created

        binding.buttonCreateReminder.setOnClickListener {
            AddReminderFragment.newInstance().show(parentFragmentManager, "AddReminderDialog")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeReminders.collect { reminders ->
                    remindersAdapter.submitList(reminders)
                    binding.emptyStateText.isVisible = reminders.isEmpty()
                    binding.recyclerViewReminders.isVisible = reminders.isNotEmpty()
                }
            }
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level 33 (Android 13) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Directly ask for the permission. The result is handled by the launcher.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    private fun setupRecyclerView() {
        remindersAdapter = RemindersAdapter { reminder ->
            // Open the edit dialog when a reminder is clicked
            AddReminderFragment.newInstance(reminder.id).show(parentFragmentManager, "EditReminderDialog")
        }
        binding.recyclerViewReminders.apply {
            adapter = remindersAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}