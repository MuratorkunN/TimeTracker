// app/src/main/java/com/example/roboticsgenius/CreateDataActivityFragment.kt
package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.viewModels
import com.example.roboticsgenius.databinding.FragmentCreateDataActivityBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout

class CreateDataActivityFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCreateDataActivityBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddDataViewModel by viewModels()

    private var initialDataSetId: Int = -1
    private var selectedColorHex: String = "#4CAF50"
    private var decimalPlaces = 0

    companion object {
        private const val ARG_DATASET_ID = "dataset_id"
        fun newInstance(dataSetId: Int): CreateDataActivityFragment {
            return CreateDataActivityFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_DATASET_ID, dataSetId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialDataSetId = it.getInt(ARG_DATASET_ID)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout
            val behavior = BottomSheetBehavior.from(bottomSheet)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateDataActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateColorPreview()
        setupTypeSpinner()
        setupNumberOptions()
        setupMultiselectOptions()
        setupColorPicker()

        binding.btnCreate.setOnClickListener {
            createActivity()
        }
    }

    private fun createActivity() {
        val name = binding.editTextActivityName.text.toString()
        if (name.isBlank()) {
            Toast.makeText(context, "Activity name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val type = binding.spinnerType.selectedItem.toString()
        val isFixedDecimal = binding.checkboxFixedDecimals.isChecked
        val isSingleSelect = binding.checkboxSingleSelection.isChecked

        val multiSelectOptions = if (type == "Multiselect") {
            binding.multiselectOptionsContainer.children
                .mapNotNull { (it as? TextInputLayout)?.editText?.text.toString() }
                .filter { it.isNotBlank() }
                .joinToString(",")
        } else { null }

        if (type == "Multiselect" && (multiSelectOptions == null || multiSelectOptions.isEmpty())) {
            Toast.makeText(context, "Please add at least one option for Multiselect", Toast.LENGTH_SHORT).show()
            return
        }

        val newActivity = Activity(
            name = name,
            color = selectedColorHex,
            dataSetId = initialDataSetId,
            isTimeTrackerActivity = false,
            dataType = type,
            dataOptions = multiSelectOptions,
            decimalPlaces = if (isFixedDecimal) decimalPlaces else -1,
            isSingleSelection = isSingleSelect,
            targetDurationSeconds = 0,
            targetPeriod = "",
            orderIndex = 0
        )

        viewModel.createDataActivity(newActivity)
        dismiss()
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

    private fun setupNumberOptions() {
        binding.textDecimalPlaces.text = decimalPlaces.toString()
        binding.buttonDecimalMinus.setOnClickListener {
            if (decimalPlaces > 0) {
                decimalPlaces--
                binding.textDecimalPlaces.text = decimalPlaces.toString()
            }
        }
        binding.buttonDecimalPlus.setOnClickListener {
            decimalPlaces++
            binding.textDecimalPlaces.text = decimalPlaces.toString()
        }
        binding.checkboxFixedDecimals.setOnCheckedChangeListener { _, isChecked ->
            binding.buttonDecimalMinus.isEnabled = isChecked
            binding.buttonDecimalPlus.isEnabled = isChecked
            binding.textDecimalPlaces.alpha = if (isChecked) 1.0f else 0.5f
        }
        binding.checkboxFixedDecimals.isChecked = false
        binding.buttonDecimalMinus.isEnabled = false
        binding.buttonDecimalPlus.isEnabled = false
        binding.textDecimalPlaces.alpha = 0.5f
    }

    private fun setupMultiselectOptions() {
        binding.multiselectOptionsContainer.removeAllViews()
        addMultiSelectOptionView("Option 1")
        addMultiSelectOptionView("Option 2")

        binding.buttonAddOption.setOnClickListener {
            addMultiSelectOptionView("Option ${binding.multiselectOptionsContainer.childCount + 1}")
        }
    }

    private fun addMultiSelectOptionView(hint: String) {
        val inflater = LayoutInflater.from(requireContext())
        val optionView = inflater.inflate(R.layout.item_multiselect_option, binding.multiselectOptionsContainer, false) as TextInputLayout
        optionView.hint = hint
        binding.multiselectOptionsContainer.addView(optionView)
    }

    private fun setupTypeSpinner() {
        val types = listOf("Text", "Number", "Checkbox", "Multiselect")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, types)
        binding.spinnerType.adapter = adapter

        binding.spinnerType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (types[position]) {
                    "Number" -> {
                        binding.numberOptions.visibility = View.VISIBLE
                        binding.multiselectOptions.visibility = View.GONE
                    }
                    "Multiselect" -> {
                        binding.numberOptions.visibility = View.GONE
                        binding.multiselectOptions.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.numberOptions.visibility = View.GONE
                        binding.multiselectOptions.visibility = View.GONE
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
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
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) updateColor() }
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