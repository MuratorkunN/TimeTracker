// app/src/main/java/com/example/roboticsgenius/MyDataFragment.kt
package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.roboticsgenius.databinding.FragmentMyDataBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MyDataFragment : Fragment() {

    private var _binding: FragmentMyDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MyDataViewModel by viewModels()

    // Flags to prevent infinite scroll loops
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

        setupScrollSync()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Observer for the list of DataSets to populate the bottom navigation
                launch {
                    viewModel.allDataSets.collect { dataSets ->
                        setupBottomNavigation(dataSets, binding.bottomNavigationMyData)
                        if (dataSets.isNotEmpty() && viewModel.uiState.value.selectedDataSet == null) {
                            viewModel.selectDataSet(dataSets.first().id)
                        } else if (dataSets.isEmpty()) {
                            binding.tableContainer.isVisible = false
                            binding.progressBar.isVisible = false
                            binding.emptyStateText.isVisible = true
                        }
                    }
                }

                // Main observer for the entire UI state
                launch {
                    viewModel.uiState.collectLatest { state ->
                        binding.emptyStateText.isVisible = state.showEmptyState
                        binding.tableContainer.isVisible = !state.showEmptyState && !state.isLoading
                        binding.progressBar.isVisible = state.isLoading

                        if (!state.isLoading && !state.showEmptyState) {
                            updateTable(state)
                        }
                    }
                }

                // Observer to update bottom nav selection
                // REMOVED redundant distinctUntilChanged()
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

    private fun updateTable(state: MyDataUiState) {
        // Set the outer border color
        (binding.tableBorder.background as? GradientDrawable)?.setStroke(
            (2 * resources.displayMetrics.density).toInt(), // 2dp stroke
            Color.parseColor(state.selectedDataSet?.color ?: "#808080")
        )

        populateHeaders(state.activities)
        populateDateColumn(state.dateLabels)
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

    private fun populateDateColumn(dateLabels: List<String>) {
        binding.dateColumnContainer.removeAllViews()
        val cellWidth = resources.getDimensionPixelSize(R.dimen.my_data_date_column_width)
        val cellHeight = resources.getDimensionPixelSize(R.dimen.my_data_cell_height)

        dateLabels.forEach { label ->
            val dateView = createTextView(label, cellWidth, cellHeight, true)
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
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    cellHeight
                )
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
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setStroke(1, Color.parseColor(hexColor))
            setColor(if (isHeader) Color.parseColor("#333333") else Color.parseColor("#424242"))
        }
    }

    private fun setupScrollSync() {
        // Horizontal Sync
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

        // Vertical Sync
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

        if (dataSets.isEmpty()) {
            return
        }

        dataSets.forEachIndexed { index, dataSet ->
            navView.menu.add(Menu.NONE, dataSet.id, index, dataSet.name)
                .setIcon(getResourceIdForIconName(dataSet.iconName))
        }

        // Restore selection if possible, otherwise select the first
        if (hasSelection && dataSets.any { it.id == currentSelectedId }) {
            navView.selectedItemId = currentSelectedId
        } else if (dataSets.isNotEmpty()) {
            navView.selectedItemId = dataSets.first().id
            viewModel.selectDataSet(dataSets.first().id) // Explicitly trigger update
        }

        navView.setOnItemSelectedListener { item ->
            viewModel.selectDataSet(item.itemId)
            true
        }
        navView.setOnItemReselectedListener {
            // Do nothing to prevent re-triggering the flow
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}