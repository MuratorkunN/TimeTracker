// app/src/main/java/com/example/roboticsgenius/ActivitiesFragment.kt

package com.example.roboticsgenius

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.FragmentActivitiesBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Collections

class ActivitiesFragment : Fragment() {
    private var _binding: FragmentActivitiesBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityAdapter: ActivityAdapter
    private val viewModel: ActivitiesViewModel by activityViewModels()
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var isDragging = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActivitiesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupDragAndDrop()
        binding.fabAddActivity.setOnClickListener {
            AddActivityFragment.newInstance().show(parentFragmentManager, "AddActivityDialog")
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.activitiesUiModel.collectLatest { activities ->
                        if (!isDragging) {
                            activityAdapter.submitList(activities)
                        }
                    }
                }
                launch { TimerService.activeActivityId.collect { id -> activityAdapter.setActiveTimerState(activeId = id) } }
                launch { TimerService.timeElapsed.collect { time -> activityAdapter.setActiveTimerState(time = time) } }
                launch { TimerService.isPaused.collect { isPaused -> activityAdapter.setActiveTimerState(isPaused = isPaused) } }
            }
        }
    }

    private fun setupDragAndDrop() {
        val simpleCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                val currentList = activityAdapter.currentList.toMutableList()
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    Collections.swap(currentList, fromPosition, toPosition)
                    activityAdapter.submitList(currentList, false)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showDeleteConfirmation(activityAdapter.currentList[position].activity)
                    activityAdapter.notifyItemChanged(position)
                }
            }

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) { isDragging = true }
            }



            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                isDragging = false
                viewModel.updateActivityOrder(activityAdapter.currentList)
            }

            override fun isLongPressDragEnabled(): Boolean = TimerService.activeActivityId.value == null
            override fun isItemViewSwipeEnabled(): Boolean = TimerService.activeActivityId.value == null
        }
        itemTouchHelper = ItemTouchHelper(simpleCallback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewActivities)
    }

    private fun showDeleteConfirmation(activity: Activity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Activity")
            .setMessage("Are you sure you want to delete '${activity.name}' and all its logged time? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteActivity(activity)
                Snackbar.make(binding.root, "'${activity.name}' deleted", Snackbar.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                activityAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setOnCancelListener { activityAdapter.notifyDataSetChanged() }
            .show()
    }

    // This now opens the unified Add/Edit fragment in edit mode
    private fun showEditActivityDialog(activityId: Int) {
        AddActivityFragment.newInstance(activityId).show(parentFragmentManager, "EditActivityDialog")
    }

    private fun showAddLogDialog(activityId: Int) {
        AddLogFragment.newInstance(activityId).show(parentFragmentManager, "AddLogDialog")
    }

    private fun setupRecyclerView() {
        activityAdapter = ActivityAdapter(
            onStartClick = { uiModel -> startTimerForActivity(uiModel.activity) },
            onStopClick = { sendServiceAction(TimerService.ACTION_STOP) },
            onPauseResumeClick = { isPaused ->
                val action = if (isPaused) TimerService.ACTION_RESUME else TimerService.ACTION_PAUSE
                sendServiceAction(action)
            },
            onCancelClick = { sendServiceAction(TimerService.ACTION_CANCEL) },
            onEditTargetClick = { uiModel -> showEditActivityDialog(uiModel.activity.id) },
            onAddLogClick = { uiModel -> showAddLogDialog(uiModel.activity.id) }
        )
        binding.recyclerViewActivities.adapter = activityAdapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun sendServiceAction(action: String) {
        Intent(requireContext(), TimerService::class.java).also { it.action = action; requireContext().startService(it) }
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