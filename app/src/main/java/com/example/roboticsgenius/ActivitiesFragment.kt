// ActivitiesFragment.kt
package com.example.roboticsgenius
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentActivitiesBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ActivitiesFragment : Fragment() {
    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityAdapter: ActivityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        val db = AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch {
            combine(
                db.activityDao().getAllActivities(),
                TimerService.activeActivityId,
                TimerService.timeElapsed
            ) { activities, activeId, time -> Triple(activities, activeId, time) }
                .collect { (activities, activeId, time) ->
                    activityAdapter.setUiState(activeId, time)
                    activityAdapter.submitList(activities)
                }
        }
        binding.fabAddActivity.setOnClickListener { AddActivityFragment().show(parentFragmentManager, "AddActivityDialog") }
    }

    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter(
            onStartClick = { activity -> startTimerForActivity(activity) },
            onStopClick = { stopTimer() }
        )
        binding.recyclerViewActivities.adapter = activityAdapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun startTimerForActivity(activity: Activity) {
        Intent(requireContext(), TimerService::class.java).also {
            it.action = TimerService.ACTION_START
            it.putExtra(TimerService.EXTRA_ACTIVITY_ID, activity.id)
            requireContext().startService(it)
        }
    }

    private fun stopTimer() {
        Intent(requireContext(), TimerService::class.java).also {
            it.action = TimerService.ACTION_STOP
            requireContext().startService(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}