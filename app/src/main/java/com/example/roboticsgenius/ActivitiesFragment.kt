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
import kotlinx.coroutines.launch

class ActivitiesFragment : Fragment() {

    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getDatabase(requireContext())
        val activityDao = db.activityDao()
        val adapter = ActivityAdapter { activity ->
            startTimerForActivity(activity)
        }
        binding.recyclerViewActivities.adapter = adapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(requireContext())

        lifecycleScope.launch {
            activityDao.getAllActivities().collect { activities ->
                adapter.submitList(activities)
            }
        }

        // --- THIS IS THE CORRECTED CLICK LISTENER ---
        binding.fabAddActivity.setOnClickListener {
            val addActivityFragment = AddActivityFragment()
            addActivityFragment.show(parentFragmentManager, "AddActivityDialog")
        }
    }

    private fun startTimerForActivity(activity: Activity) {
        if (TimerService.isRunning) return
        val serviceIntent = Intent(requireContext(), TimerService::class.java)
        serviceIntent.putExtra("ACTIVITY_ID", activity.id)
        requireContext().startService(serviceIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}