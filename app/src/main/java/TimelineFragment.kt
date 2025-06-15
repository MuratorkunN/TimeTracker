// app/src/main/java/com/example/roboticsgenius/TimelineFragment.kt

package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentTimelineBinding
import kotlinx.coroutines.launch

class TimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null
    private val binding get() = _binding!!

    // NEW: Use the ViewModel
    private val viewModel: TimelineViewModel by viewModels()
    private lateinit var timelineAdapter: TimelineAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimelineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe the list from the ViewModel
                viewModel.timelineItems.collect { items ->
                    timelineAdapter.submitList(items)
                }
            }
        }
    }

    private fun setupRecyclerView() {
        // Pass the new long-click handler to the adapter
        timelineAdapter = TimelineAdapter { logId ->
            showDeleteConfirmation(logId)
        }
        binding.recyclerViewTimeline.apply {
            adapter = timelineAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    // NEW: Function to show the deletion dialog
    private fun showDeleteConfirmation(logId: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Log Entry")
            .setMessage("Are you sure you want to delete this log entry? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteLog(logId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}