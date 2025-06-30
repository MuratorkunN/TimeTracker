// app/src/main/java/com/example/roboticsgenius/MyDataFragment.kt
package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.DialogMyDataSettingsBinding
import com.example.roboticsgenius.databinding.DialogReorderColumnsBinding
import com.example.roboticsgenius.databinding.FragmentMyDataBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class MyDataFragment : Fragment() {

    private var _binding: FragmentMyDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyDataViewModel by viewModels()

    private var isProgrammaticScrollH = false
    private var isProgrammaticScrollV = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        setupScrollSync()

        binding.buttonPrev.setOnClickListener { viewModel.navigateDate(-1) }
        binding.buttonNext.setOnClickListener { viewModel.navigateDate(1) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.allDataSets.collect { dataSets ->
                        setupBottomNavigation(dataSets, binding.bottomNavigationMyData)
                        if (dataSets.isNotEmpty() && viewModel.uiState.value.selectedDataSet == null) {
                            viewModel.selectDataSet(dataSets.first().id)
                        } else if (dataSets.isEmpty()) {
                            binding.tableContainer.isVisible = false
                            binding.progressBar.isVisible = false
                            binding.emptyStateText.isVisible = true
                            binding.dateNavigator.isVisible = false
                        }
                    }
                }

                launch {
                    viewModel.uiState.collectLatest { state ->
                        binding.emptyStateText.isVisible = state.showEmptyState
                        binding.tableContainer.isVisible = !state.showEmptyState && !state.isLoading
                        binding.dateNavigator.isVisible = !state.showEmptyState && !state.isLoading
                        binding.progressBar.isVisible = state.isLoading

                        binding.textViewDateRange.text = state.dateNavigatorLabel
                        binding.buttonPrev.isVisible = state.showDateNavigatorArrows
                        binding.buttonNext.isVisible = state.showDateNavigatorArrows

                        if (!state.isLoading && !state.showEmptyState) {
                            updateTable(state)
                        }
                    }
                }

                launch {
                    viewModel.uiState.map { it.selectedDataSet?.id }.collect { selectedId ->
                        if (selectedId != null && binding.bottomNavigationMyData.selectedItemId != selectedId) {
                            binding.bottomNavigationMyData.selectedItemId = selectedId
                        }
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.my_data_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return if (menuItem.itemId == R.id.action_my_data_settings) {
                    showSettingsDialog()
                    true
                } else {
                    false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showSettingsDialog() {
        val settingsBinding = DialogMyDataSettingsBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(settingsBinding.root)
            .create()

        val currentSettings = viewModel.settings.value
        var tempStartDate = Calendar.getInstance().apply { timeInMillis = currentSettings.customStartDate }
        var tempEndDate = Calendar.getInstance().apply { timeInMillis = currentSettings.customEndDate }
        val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

        fun updateDateButtons() {
            settingsBinding.btnStartDate.text = dateFormat.format(tempStartDate.time)
            settingsBinding.btnEndDate.text = dateFormat.format(tempEndDate.time)
        }

        // Pre-fill dialog with current settings
        updateDateButtons()
        settingsBinding.chipGroupDateRange.check(
            when (currentSettings.timePeriod) {
                MyDataTimePeriod.WEEKLY -> settingsBinding.chipWeekly.id
                MyDataTimePeriod.MONTHLY -> settingsBinding.chipMonthly.id
                MyDataTimePeriod.YEARLY -> settingsBinding.chipYearly.id
                MyDataTimePeriod.CUSTOM -> settingsBinding.chipCustom.id
            }
        )
        settingsBinding.customDateContainer.isVisible = currentSettings.timePeriod == MyDataTimePeriod.CUSTOM

        // Listeners
        settingsBinding.chipGroupDateRange.setOnCheckedChangeListener { _, checkedId ->
            settingsBinding.customDateContainer.isVisible = checkedId == settingsBinding.chipCustom.id
        }

        settingsBinding.btnStartDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setEnd(tempEndDate.timeInMillis) // Cannot be after the end date
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Start Date")
                .setSelection(tempStartDate.timeInMillis)
                .setCalendarConstraints(constraints)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                // Adjust for timezone to avoid off-by-one day issues
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
                tempStartDate.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH))
                updateDateButtons()
            }
            picker.show(parentFragmentManager, "StartDatePicker")
        }

        settingsBinding.btnEndDate.setOnClickListener {
            val constraints = CalendarConstraints.Builder()
                .setStart(tempStartDate.timeInMillis) // Cannot be before the start date
                .setEnd(MaterialDatePicker.todayInUtcMilliseconds()) // Cannot be after today
                .build()

            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select End Date")
                .setSelection(tempEndDate.timeInMillis)
                .setCalendarConstraints(constraints)
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = selection }
                tempEndDate.set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH))
                updateDateButtons()
            }
            picker.show(parentFragmentManager, "EndDatePicker")
        }

        settingsBinding.btnReorder.setOnClickListener { showReorderDialog() }

        settingsBinding.btnDownloadCsv.setOnClickListener {
            Toast.makeText(context, "CSV Download coming soon!", Toast.LENGTH_SHORT).show()
        }

        settingsBinding.btnCancel.setOnClickListener { dialog.dismiss() }

        settingsBinding.btnApply.setOnClickListener {
            val newPeriod = when (settingsBinding.chipGroupDateRange.checkedChipId) {
                settingsBinding.chipWeekly.id -> MyDataTimePeriod.WEEKLY
                settingsBinding.chipYearly.id -> MyDataTimePeriod.YEARLY
                settingsBinding.chipCustom.id -> MyDataTimePeriod.CUSTOM
                else -> MyDataTimePeriod.MONTHLY
            }
            viewModel.applySettings(
                MyDataSettings(
                    timePeriod = newPeriod,
                    customStartDate = tempStartDate.timeInMillis,
                    customEndDate = tempEndDate.timeInMillis
                )
            )
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showReorderDialog() {
        lifecycleScope.launch {
            val activities = viewModel.uiState.first().activities
            if (activities.isEmpty()) {
                Toast.makeText(context, "No columns to reorder.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val reorderBinding = DialogReorderColumnsBinding.inflate(layoutInflater)
            val adapter = ReorderAdapter()
            reorderBinding.reorderRecyclerView.layoutManager = LinearLayoutManager(requireContext())
            reorderBinding.reorderRecyclerView.adapter = adapter
            adapter.submitList(activities)

            val touchHelper = ItemTouchHelper(ItemMoveCallback(adapter))
            touchHelper.attachToRecyclerView(reorderBinding.reorderRecyclerView)

            AlertDialog.Builder(requireContext())
                .setView(reorderBinding.root)
                .setPositiveButton("Save") { _, _ ->
                    val reorderedList = adapter.currentList
                    viewModel.updateActivityOrder(reorderedList)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun updateTable(state: MyDataUiState) {
        val dataSetColor = state.selectedDataSet?.color ?: "#808080"
        (binding.tableBorder.background as? GradientDrawable)?.setStroke(
            (8 * resources.displayMetrics.density).toInt(),
            Color.parseColor(dataSetColor)
        )
        populateHeaders(state.activities)
        populateDateColumn(state.dateLabels, dataSetColor)
        populateDataGrid(state.activities, state.dataGrid)
    }

    private fun populateHeaders(activities: List<Activity>) {
        binding.headerContainer.removeAllViews()
        val cellWidth = resources.getDimensionPixelSize(R.dimen.my_data_cell_width)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.my_data_header_row_height)
        activities.forEach { activity ->
            val headerView = createTextView(activity.name, cellWidth, cellHeight, true)
            headerView.background = createCellBorder(activity.color, true)
            binding.headerContainer.addView(headerView)
        }
    }

    private fun populateDateColumn(dateLabels: List<String>, dataSetColor: String) {
        binding.dateColumnContainer.removeAllViews()
        val cellWidth = resources.getDimensionPixelSize(R.dimen.my_data_date_column_width)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.my_data_cell_height)
        dateLabels.forEach { label ->
            val dateView = createTextView(label, cellWidth, cellHeight, true)
            dateView.background = createCellBorder(dataSetColor, true)
            binding.dateColumnContainer.addView(dateView)
        }
    }

    private fun populateDataGrid(activities: List<Activity>, gridData: List<List<String>>) {
        binding.dataGridContainer.removeAllViews()
        val cellWidth = resources.getDimensionPixelSize(R.dimen.my_data_cell_width)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.my_data_cell_height)
        gridData.forEach { rowData ->
            val rowLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, cellHeight)
            }
            rowData.forEachIndexed { colIndex, cellData ->
                val activity = activities[colIndex]
                val cellView = createTextView(cellData, cellWidth, cellHeight)
                cellView.background = createCellBorder(activity.color, false)
                rowLayout.addView(cellView)
            }
            binding.dataGridContainer.addView(rowLayout)
        }
    }

    private fun createTextView(text: String, width: Int, height: Int, isHeader: Boolean = false): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(width, height)
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if(isHeader) 14f else 12f)
            setPadding(resources.getDimensionPixelSize(R.dimen.my_data_cell_padding))
        }
    }

    private fun createCellBorder(hexColor: String, isHeader: Boolean): GradientDrawable {
        val cellBackgroundColor = if (isHeader) "#333333" else "#212121"
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(1, Color.parseColor(hexColor))
            setColor(Color.parseColor(cellBackgroundColor))
        }
    }

    private fun setupScrollSync() {
        binding.headerScrollView.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (!isProgrammaticScrollH) {
                isProgrammaticScrollH = true
                binding.dataGridHorizontalScroll.scrollTo(scrollX, 0)
            }
            isProgrammaticScrollH = false
        }
        binding.dataGridHorizontalScroll.setOnScrollChangeListener { _, scrollX, _, _, _ ->
            if (!isProgrammaticScrollH) {
                isProgrammaticScrollH = true
                binding.headerScrollView.scrollTo(scrollX, 0)
            }
            isProgrammaticScrollH = false
        }
        binding.dateColumnScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (!isProgrammaticScrollV) {
                isProgrammaticScrollV = true
                binding.dataGridVerticalScroll.scrollTo(0, scrollY)
            }
            isProgrammaticScrollV = false
        }
        binding.dataGridVerticalScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            if (!isProgrammaticScrollV) {
                isProgrammaticScrollV = true
                binding.dateColumnScrollView.scrollTo(0, scrollY)
            }
            isProgrammaticScrollV = false
        }
    }

    private fun getResourceIdForIconName(iconName: String): Int {
        return requireContext().resources.getIdentifier(
            iconName, "drawable", requireContext().packageName
        ).let { if (it == 0) R.drawable.ic_nav_my_data else it }
    }

    private fun setupBottomNavigation(dataSets: List<DataSet>, navView: BottomNavigationView) {
        val currentSelectedId = navView.selectedItemId
        val hasSelection = navView.menu.findItem(currentSelectedId) != null
        navView.menu.clear()
        if (dataSets.isEmpty()) return
        dataSets.forEachIndexed { index, dataSet ->
            navView.menu.add(Menu.NONE, dataSet.id, index, dataSet.name)
                .setIcon(getResourceIdForIconName(dataSet.iconName))
        }
        if (hasSelection && dataSets.any { it.id == currentSelectedId }) {
            navView.selectedItemId = currentSelectedId
        } else if (dataSets.isNotEmpty()) {
            navView.selectedItemId = dataSets.first().id
            viewModel.selectDataSet(dataSets.first().id)
        }
        navView.setOnItemSelectedListener { item ->
            viewModel.selectDataSet(item.itemId)
            true
        }
        navView.setOnItemReselectedListener {}
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}