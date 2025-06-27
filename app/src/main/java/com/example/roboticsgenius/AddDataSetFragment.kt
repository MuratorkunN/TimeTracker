// app/src/main/java/com/example/roboticsgenius/AddDataSetFragment.kt

package com.example.roboticsgenius

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.roboticsgenius.databinding.DialogAddDataSetBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddDataSetFragment : DialogFragment() {

    private var _binding: DialogAddDataSetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by activityViewModels()
    private var selectedColor: String = "#808080"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAddDataSetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateColorPreview()

        binding.colorSectionContainer.setOnClickListener {
            showRgbColorPickerDialog(
                title = "Select Data Set Color",
                initialColor = selectedColor
            ) { newColorHex ->
                selectedColor = newColorHex
                updateColorPreview()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnCreate.setOnClickListener {
            val name = binding.editTextDataSetName.text.toString()
            if (name.isBlank()) {
                Toast.makeText(context, "Data Set name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newDataSet = DataSet(
                name = name,
                color = selectedColor,
                iconName = "ic_default_dataset"
            )
            viewModel.addDataSet(newDataSet)
            dismiss()
        }
    }

    private fun updateColorPreview() {
        (binding.viewColorPreview.background as GradientDrawable).setColor(Color.parseColor(selectedColor))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): AddDataSetFragment {
            return AddDataSetFragment()
        }
    }

    // This is the new, self-contained color picker logic
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