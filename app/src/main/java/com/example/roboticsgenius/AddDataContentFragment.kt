// app/src/main/java/com/example/roboticsgenius/AddDataContentFragment.kt
package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.FragmentAddDataContentBinding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Collections
import java.util.Locale

class AddDataContentFragment : Fragment() {

    private var _binding: FragmentAddDataContentBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddDataViewModel by viewModels()
    private lateinit var dataActivityAdapter: DataActivityAdapter
    private var isDragging = false

    companion object {
        private const val ARG_DATASET_ID = "dataset_id"
        fun newInstance(dataSetId: Int): AddDataContentFragment {
            return AddDataContentFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DATASET_ID, dataSetId)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddDataContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataSetId = arguments?.getInt(ARG_DATASET_ID) ?: -1
        viewModel.setDataSetId(dataSetId)

        setupRecyclerView()
        setupDragAndDrop()

        binding.buttonCreateActivity.setOnClickListener {
            CreateDataActivityFragment.newInstance(dataSetId).show(parentFragmentManager, "CreateDataActivity")
        }

        lifecycleScope.launch {
            viewModel.selectedDate.collectLatest { calendar ->
                updateDateDisplay(calendar)
            }
        }

        lifecycleScope.launch {
            viewModel.isPreviousEnabled.collect { binding.buttonPrev.isEnabled = it }
        }
        lifecycleScope.launch {
            viewModel.isNextEnabled.collect { binding.buttonNext.isEnabled = it }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { uiModels ->
                if (!isDragging) {
                    dataActivityAdapter.submitList(uiModels)
                }
            }
        }

        setupDateNavigator()
    }

    private fun setupDateNavigator() {
        binding.buttonPrev.setOnClickListener { viewModel.changeDate(-1) }
        binding.buttonNext.setOnClickListener { viewModel.changeDate(1) }
        binding.textViewDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        // NEW: Constrain the date picker with global start date and today.
        val constraints = CalendarConstraints.Builder()
            .setStart(GlobalSettings.getAppStartDate().timeInMillis)
            .setEnd(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(viewModel.selectedDate.value.timeInMillis)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val selectedCalendar = Calendar.getInstance().apply { timeInMillis = selection }
            val year = selectedCalendar.get(Calendar.YEAR)
            val month = selectedCalendar.get(Calendar.MONTH)
            val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)
            viewModel.setDate(year, month, day)
        }
        datePicker.show(parentFragmentManager, "DatePicker")
    }

    private fun setupRecyclerView() {
        dataActivityAdapter = DataActivityAdapter(
            onSave = { activityId, value, existingEntryId, onSuccess ->
                viewModel.upsertDataEntry(activityId, value, existingEntryId, onSuccess)
            },
            onGetSuggestions = { activityId ->
                viewModel.getSuggestions(activityId)
            }
        )
        binding.recyclerViewDataActivities.apply {
            adapter = dataActivityAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupDragAndDrop() {
        val simpleCallback = object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition
                if (fromPosition != RecyclerView.NO_POSITION && toPosition != RecyclerView.NO_POSITION) {
                    val currentList = dataActivityAdapter.currentList.toMutableList()
                    Collections.swap(currentList, fromPosition, toPosition)
                    dataActivityAdapter.submitList(currentList)
                }
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

            override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(viewHolder, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true
                }
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                isDragging = false
                viewModel.saveDataActivityOrder(dataActivityAdapter.currentList)
            }
        }
        ItemTouchHelper(simpleCallback).attachToRecyclerView(binding.recyclerViewDataActivities)
    }

    private fun updateDateDisplay(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        binding.textViewDate.text = dateFormat.format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}