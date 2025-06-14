// app/src/main/java/com/example/roboticsgenius/ActivitiesFragment.kt
package com.example.roboticsgenius

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentActivitiesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ActivitiesFragment : Fragment() {
    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityAdapter: ActivityAdapter
    private val viewModel: ActivitiesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.fabAddActivity.setOnClickListener {
            AddActivityFragment().show(parentFragmentManager, "AddActivityDialog")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // This coroutine collects the list of activities from the ViewModel
                // and submits it to the adapter.
                launch {
                    viewModel.activitiesUiModel.collectLatest { activities ->
                        activityAdapter.submitList(activities)
                    }
                }

                // This second coroutine observes the timer state from the Service
                // and passes it to the adapter for real-time UI updates (like the ticking clock)
                launch {
                    TimerService.activeActivityId.collect { id ->
                        activityAdapter.setActiveTimerState(activeId = id)
                    }
                }
                launch {
                    TimerService.timeElapsed.collect { time ->
                        activityAdapter.setActiveTimerState(time = time)
                    }
                }
                launch {
                    TimerService.isPaused.collect { isPaused ->
                        activityAdapter.setActiveTimerState(isPaused = isPaused)
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter(
            onStartClick = { activity -> startTimerForActivity(activity) },
            onStopClick = { sendServiceAction(TimerService.ACTION_STOP) },
            onPauseResumeClick = { isPaused ->
                val action = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                sendServiceAction(action)
            },
            onCancelClick = { sendServiceAction(TimerService.ACTION_CANCEL) }
        )
        binding.recyclerViewActivities.adapter = activityAdapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun sendServiceAction(action: String) {
        Intent(requireContext(), TimerService::class.java).also {
            it.action = action
            requireContext().startService(it)
        }
    }

    private fun startTimerForActivity(activity: Activity) {
        Intent(requireContext(), TimerService::class.java).also {
            it.action = TimerService.ACTION_START
            it.putExtra(TimerService.EXTRA_ACTIVITY_ID, activity.id)
            requireContext().startService(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}