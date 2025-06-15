package com.example.roboticsgenius

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
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
                binding.textViewDateRange.text = state.dateLabel

                binding.lineChart.xAxis.apply {
                    labelCount = state.xAxisLabels.size
                    axisMinimum = 0f
                    axisMaximum = (state.xAxisLabels.size - 1).toFloat().coerceAtLeast(0f)

                    valueFormatter = if (viewModel.filterState.value.timeRange == TimeRange.MONTH) {
                        MonthlyAxisFormatter(state.xAxisLabels)
                    } else {
                        DefaultAxisFormatter(state.xAxisLabels)
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
                TimeRange.YEAR -> dialogBinding.chipYear.id
            })

            allActivities.forEach { activity ->
                dialogBinding.activityCheckboxContainer.addView(MaterialCheckBox(requireContext()).apply {
                    text = activity.name
                    isChecked = tempSelectedIds.contains(activity.id)
                    buttonTintList = ColorStateList.valueOf(Color.parseColor(activity.color))
                    setOnCheckedChangeListener { _, isChecked ->
                        if (isChecked) tempSelectedIds.add(activity.id) else tempSelectedIds.remove(activity.id)
                    }
                })
            }

            dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
            dialogBinding.btnApply.setOnClickListener {
                val newTimeRange = when (dialogBinding.chipGroupTimeRange.checkedChipId) {
                    dialogBinding.chipMonth.id -> TimeRange.MONTH
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
            spaceTop = 20f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    if (value < 1) return "0m"
                    val hours = (value / 60).toInt()
                    val minutes = (value % 60).toInt()
                    return when {
                        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                        hours > 0 -> "${hours}h"
                        else -> "${minutes}m"
                    }
                }
            }
        }
        chart.axisRight.isEnabled = false
        chart.legend.textColor = textColor
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class DefaultAxisFormatter(private val labels: List<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return labels.getOrNull(value.toInt()) ?: ""
        }
    }

    class MonthlyAxisFormatter(private val labels: List<String>) : ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            val label = labels.getOrNull(value.toInt()) ?: return ""
            // THE FIX: Return the non-null label, or an empty string if it's not in the set.
            return if (label in setOf("1", "8", "15", "22", "29")) {
                label
            } else {
                ""
            }
        }
    }
}