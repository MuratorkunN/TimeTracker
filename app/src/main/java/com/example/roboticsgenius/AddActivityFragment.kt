// app/src/main/java/com/example/roboticsgenius/AddActivityFragment.kt

package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.roboticsgenius.databinding.FragmentAddActivityBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class AddActivityFragment : BottomSheetDialogFragment() {
    private var _binding: FragmentAddActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by activityViewModels()
    private var existingActivity: Activity? = null
    private lateinit var dataSetAdapter: DataSetAdapter

    private var selectedColorHex: String = "#4CAF50"
    private var selectedDataSetId: Int? = null

    companion object {
        private const val ARG_ACTIVITY_ID = "activity_id"
        fun newInstance(activityId: Int? = null): AddActivityFragment {
            return AddActivityFragment().apply {
                arguments = Bundle().apply {
                    activityId?.let { putInt(ARG_ACTIVITY_ID, it) }
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    // THIS METHOD IS NEW. IT FIXES THE HEIGHT.
    override fun onStart() {
        super.onStart()
        // This ensures the bottom sheet is fully expanded, showing the button without scrolling.
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true // Prevents it from collapsing when dragged.
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDataSetRecyclerView()
        setupNumberPickers()
        setupQuickButtons()
        setupPeriodToggle()
        setupSaveButton()
        setupColorPicker()

        val activityId = arguments?.getInt(ARG_ACTIVITY_ID)
        if (activityId != null && activityId != 0) {
            loadExistingActivity(activityId)
        } else {
            binding.toggleGroupPeriod.check(R.id.btnWeekly)
            updateColorPreview()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allDataSets.collect { dataSets ->
                if (existingActivity == null && selectedDataSetId == null && dataSets.isNotEmpty()) {
                    selectedDataSetId = dataSets.first().id
                }
                dataSetAdapter.submitAndSelect(dataSets, selectedDataSetId)
            }
        }
    }

    private fun loadExistingActivity(activityId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            existingActivity = db.activityDao().getActivityById(activityId)
            existingActivity?.let { activity ->
                binding.textViewTitle.text = "Edit Activity"
                binding.btnSaveActivity.text = "Update Activity"
                binding.editTextActivityName.setText(activity.name)
                selectedColorHex = activity.color
                updateColorPreview()
                selectedDataSetId = activity.dataSetId
                setTime(
                    activity.targetDurationSeconds / 3600,
                    (activity.targetDurationSeconds % 3600) / 60,
                    activity.targetDurationSeconds % 60
                )
                when (activity.targetPeriod) {
                    "Daily" -> binding.toggleGroupPeriod.check(R.id.btnDaily)
                    "Weekly" -> binding.toggleGroupPeriod.check(R.id.btnWeekly)
                    "Monthly" -> binding.toggleGroupPeriod.check(R.id.btnMonthly)
                }
                dataSetAdapter.submitAndSelect(viewModel.allDataSets.value, selectedDataSetId)
            }
        }
    }

    private fun setupColorPicker() {
        binding.colorSectionContainer.setOnClickListener {
            showRgbColorPickerDialog(
                title = "Select Activity Color",
                initialColor = selectedColorHex
            ) { newColorHex ->
                selectedColorHex = newColorHex
                updateColorPreview()
            }
        }
    }

    private fun updateColorPreview() {
        (binding.viewColorPreview.background as? GradientDrawable)?.setColor(Color.parseColor(selectedColorHex))
    }

    private fun setupDataSetRecyclerView() {
        dataSetAdapter = DataSetAdapter(
            onDataSetSelected = { dataSetId ->
                selectedDataSetId = dataSetId
                dataSetAdapter.updateSelection(dataSetId)
            },
            onAddDataSetClicked = {
                AddDataSetFragment.newInstance().show(parentFragmentManager, "AddDataSetDialog")
            }
        )
        binding.recyclerViewDataSets.adapter = dataSetAdapter
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0; binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0; binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0; binding.pickerSeconds.maxValue = 59
    }

    private fun setupQuickButtons() {
        binding.btn5min.setOnClickListener { setTime(0, 5, 0) }
        binding.btn10min.setOnClickListener { setTime(0, 10, 0) }
        binding.btn30min.setOnClickListener { setTime(0, 30, 0) }
        binding.btn1hr.setOnClickListener { setTime(1, 0, 0) }
    }

    private fun setTime(h: Int, m: Int, s: Int) {
        binding.pickerHours.value = h; binding.pickerMinutes.value = m; binding.pickerSeconds.value = s
    }

    private fun setupPeriodToggle() {}

    private fun setupSaveButton() {
        binding.btnSaveActivity.setOnClickListener {
            val name = binding.editTextActivityName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(context, "Activity name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedDataSetId == null) {
                Toast.makeText(context, "Please select or create a Data Set", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val duration = binding.pickerHours.value * 3600 + binding.pickerMinutes.value * 60 + binding.pickerSeconds.value
            val period = when (binding.toggleGroupPeriod.checkedButtonId) {
                R.id.btnDaily -> "Daily"
                R.id.btnWeekly -> "Weekly"
                R.id.btnMonthly -> "Monthly"
                else -> "Weekly"
            }
            val activityToSave = existingActivity?.copy(
                name = name,
                color = selectedColorHex,
                dataSetId = selectedDataSetId!!,
                targetDurationSeconds = duration,
                targetPeriod = period
            ) ?: Activity(
                name = name,
                color = selectedColorHex,
                dataSetId = selectedDataSetId!!,
                targetDurationSeconds = duration,
                targetPeriod = period,
                orderIndex = 0
            )
            viewModel.upsertActivity(activityToSave)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showRgbColorPickerDialog(title: String, initialColor: String, onColorSelected: (String) -> Unit) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_rgb_color_picker, null)

        val previewView = dialogView.findViewById<View>(R.id.color_preview)
        val hexTextView = dialogView.findViewById<TextView>(R.id.hex_text)
        val rSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_r)
        val gSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_g)
        val bSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_b)
        val vSeekBar = dialogView.findViewById<SeekBar>(R.id.seekbar_v)

        var color = Color.parseColor(initialColor)
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)

        rSeekBar.progress = Color.red(color)
        gSeekBar.progress = Color.green(color)
        bSeekBar.progress = Color.blue(color)
        vSeekBar.progress = (hsv[2] * 255).toInt()

        fun updateColor() {
            val r = rSeekBar.progress
            val g = gSeekBar.progress
            val b = bSeekBar.progress
            Color.RGBToHSV(r, g, b, hsv)
            hsv[2] = vSeekBar.progress / 255f
            color = Color.HSVToColor(hsv)
            previewView.setBackgroundColor(color)
            hexTextView.text = String.format("#%06X", 0xFFFFFF and color)
        }

        updateColor()

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) updateColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        rSeekBar.setOnSeekBarChangeListener(listener)
        gSeekBar.setOnSeekBarChangeListener(listener)
        bSeekBar.setOnSeekBarChangeListener(listener)
        vSeekBar.setOnSeekBarChangeListener(listener)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("OK") { _, _ ->
                onColorSelected(String.format("#%06X", 0xFFFFFF and color))
            }
            .show()
    }
}