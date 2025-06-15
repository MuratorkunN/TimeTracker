package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.DialogInsightsSettingsBinding
import com.example.roboticsgenius.databinding.FragmentInsightsBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class InsightsFragment : Fragment() {
    private var _binding: FragmentInsightsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: InsightsViewModel by viewModels()
    private lateinit var summaryAdapter: InsightsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsightsBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupChart(binding.lineChart)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonSettings.setOnClickListener { showSettingsDialog() }
        binding.buttonNext.setOnClickListener { viewModel.navigateForward() }
        binding.buttonPrev.setOnClickListener { viewModel.navigateBackward() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.dateNavigator.isVisible = true // THE FIX: Arrows are ALWAYS visible.
                binding.textViewDateRange.text = state.dateLabel

                // THE "LABELS" FIX: Force the chart to show the full range of labels.
                binding.lineChart.xAxis.apply {
                    labelCount = state.xAxisLabels.size
                    axisMinimum = 0f
                    axisMaximum = (state.xAxisLabels.size - 1).toFloat().coerceAtLeast(0f)
                    valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String {
                            return state.xAxisLabels.getOrNull(value.toInt()) ?: ""
                        }
                    }
                }

                binding.lineChart.axisLeft.axisMaximum = state.yAxisMax
                binding.lineChart.data = state.lineData
                binding.lineChart.invalidate()
                summaryAdapter.submitList(state.summaryList)
            }
        }
    }

    private fun showSettingsDialog() {
        lifecycleScope.launch {
            val dialogBinding = DialogInsightsSettingsBinding.inflate(layoutInflater)
            val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()
            val currentFilterState = viewModel.filterState.first()
            val allActivities = viewModel.allActivities.first()
            val tempSelectedIds = currentFilterState.activityIds.toMutableSet()

            dialogBinding.chipGroupTimeRange.check(when (currentFilterState.timeRange) {
                TimeRange.WEEK -> dialogBinding.chipWeek.id
                TimeRange.MONTH -> dialogBinding.chipMonth.id
                TimeRange.SIX_MONTHS -> dialogBinding.chip6Months.id
                TimeRange.YEAR -> dialogBinding.chipYear.id
            })

            allActivities.forEach { activity ->
                dialogBinding.activityCheckboxContainer.addView(MaterialCheckBox(requireContext()).apply {
                    text = activity.name
                    isChecked = tempSelectedIds.contains(activity.id)
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) tempSelectedIds.add(activity.id) else tempSelectedIds.remove(activity.id)
                    }
                })
            }

            dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
            dialogBinding.btnApply.setOnClickListener {
                val newTimeRange = when (dialogBinding.chipGroupTimeRange.checkedChipId) {
                    dialogBinding.chipMonth.id -> TimeRange.MONTH
                    dialogBinding.chip6Months.id -> TimeRange.SIX_MONTHS
                    dialogBinding.chipYear.id -> TimeRange.YEAR
                    else -> TimeRange.WEEK
                }
                viewModel.applyFilters(newTimeRange, tempSelectedIds)
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    private fun setupRecyclerView() {
        summaryAdapter = InsightsAdapter()
        binding.recyclerViewSummary.apply { adapter = summaryAdapter; layoutManager = LinearLayoutManager(requireContext()) }
    }

    private fun setupChart(chart: LineChart) {
        val textColor = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_on_surface_emphasis_medium)
        chart.description.isEnabled = false
        chart.setDrawGridBackground(false)
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            this.textColor = textColor
            setDrawGridLines(false)
        }
        chart.axisLeft.apply {
            axisMinimum = 0f
            this.textColor = textColor
            setDrawGridLines(true)
        }
        chart.axisRight.isEnabled = false
        chart.legend.textColor = textColor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}