// app/src/main/java/com/example/roboticsgenius/DataActivityAdapter.kt
package com.example.roboticsgenius

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.view.children
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.roboticsgenius.databinding.ItemDataActivityBinding
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.math.BigDecimal

class DataActivityAdapter(
    private val onSave: (activityId: Int, value: String, existingEntryId: Int?, onSuccess: () -> Unit) -> Unit,
    private val onGetSuggestions: suspend (activityId: Int) -> List<String>
) : ListAdapter<DataActivityUiModel, DataActivityAdapter.DataActivityViewHolder>(DiffCallback) {

    // THIS IS THE FIX. This function is called when an item is actually visible on screen.
    override fun onViewAttachedToWindow(holder: DataActivityViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttached() // We tell the ViewHolder it's now safe to load data.
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataActivityViewHolder {
        val binding = ItemDataActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DataActivityViewHolder(binding, onSave, onGetSuggestions)
    }

    override fun onBindViewHolder(holder: DataActivityViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DataActivityViewHolder(
        private val binding: ItemDataActivityBinding,
        private val onSave: (activityId: Int, value: String, existingEntryId: Int?, onSuccess: () -> Unit) -> Unit,
        private val onGetSuggestions: suspend (activityId: Int) -> List<String>
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentUiModel: DataActivityUiModel? = null
        private var isEditing: Boolean = false

        init {
            binding.buttonSave.setOnClickListener {
                currentUiModel?.let { model ->
                    val value = getInputValue(model.activity)
                    if (isInputValid(value, model.activity)) {
                        onSave(model.activity.id, value, model.entry?.id) {
                            // On success, switch back to non-editing mode.
                            isEditing = false
                            updateUiState()
                        }
                    }
                }
            }
            binding.buttonEdit.setOnClickListener {
                isEditing = true
                updateUiState()
            }
        }

        fun bind(uiModel: DataActivityUiModel) {
            currentUiModel = uiModel
            isEditing = (uiModel.entry == null) // A new item always starts in editing mode.

            val activity = uiModel.activity
            binding.textViewActivityName.text = activity.name
            (binding.cardView as MaterialCardView).strokeColor = Color.parseColor(activity.color)

            configureInputs(activity, uiModel.entry)
            updateUiState()
        }

        // This is called by the adapter when the view is on screen and has a LifecycleOwner.
        fun onAttached() {
            // If the view is in editing mode when it appears, load suggestions.
            if(isEditing) {
                loadSuggestions()
            }
        }

        private fun updateUiState() {
            binding.buttonEdit.visibility = if (isEditing) View.GONE else View.VISIBLE
            binding.buttonSave.visibility = if (isEditing) View.VISIBLE else View.GONE
            setInputsEnabled(isEditing)

            if (isEditing) {
                loadSuggestions()
            } else {
                binding.suggestionsContainer.visibility = View.GONE
            }
        }

        private fun loadSuggestions() {
            val activity = currentUiModel?.activity ?: return
            if (activity.dataType != "Text" && activity.dataType != "Number") {
                binding.suggestionsContainer.visibility = View.GONE
                return
            }

            itemView.findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                val suggestions = onGetSuggestions(activity.id)
                if (suggestions.isNotEmpty()) {
                    binding.suggestionsContainer.visibility = View.VISIBLE
                    binding.suggestionsChipGroup.removeAllViews()
                    suggestions.forEach { suggestionText ->
                        val chip = (LayoutInflater.from(itemView.context)
                            .inflate(R.layout.item_suggestion_chip, binding.suggestionsChipGroup, false) as Chip).apply {
                            text = suggestionText
                            setOnClickListener {
                                binding.textInputEditText.setText(suggestionText)
                                binding.numberInputEditText.setText(suggestionText)
                            }
                        }
                        binding.suggestionsChipGroup.addView(chip)
                    }
                } else {
                    binding.suggestionsContainer.visibility = View.GONE
                }
            }
        }

        private fun setInputsEnabled(isEnabled: Boolean) {
            binding.textInputEditText.isEnabled = isEnabled
            binding.numberInputEditText.isEnabled = isEnabled
            binding.checkboxInput.isEnabled = isEnabled
            binding.multiselectContainer.children.forEach { view ->
                view.isEnabled = isEnabled
                if (view is RadioGroup) {
                    view.children.forEach { radio -> radio.isEnabled = isEnabled }
                }
            }
        }

        private fun configureInputs(activity: Activity, entry: DataEntry?) {
            // Hide all first
            binding.textInputLayout.visibility = View.GONE
            binding.numberInputLayout.visibility = View.GONE
            binding.checkboxInput.visibility = View.GONE
            binding.multiselectContainer.visibility = View.GONE

            when (activity.dataType) {
                "Text" -> {
                    binding.textInputLayout.visibility = View.VISIBLE
                    binding.textInputEditText.setText(entry?.value ?: "")
                }
                "Number" -> {
                    binding.numberInputLayout.visibility = View.VISIBLE
                    binding.numberInputEditText.setText(entry?.value ?: "")
                }
                "Checkbox" -> {
                    binding.checkboxInput.visibility = View.VISIBLE
                    binding.checkboxInput.isChecked = entry?.value.toBoolean()
                }
                "Multiselect" -> {
                    binding.multiselectContainer.visibility = View.VISIBLE
                    binding.multiselectContainer.removeAllViews()

                    val savedOptions = entry?.value?.split(",")?.toSet() ?: emptySet()
                    if (activity.isSingleSelection) {
                        val radioGroup = RadioGroup(itemView.context)
                        activity.dataOptions?.split(",")?.forEach { optionText ->
                            val radioButton = RadioButton(itemView.context).apply { text = optionText; isChecked = savedOptions.contains(optionText) }
                            radioGroup.addView(radioButton)
                        }
                        binding.multiselectContainer.addView(radioGroup)
                    } else {
                        activity.dataOptions?.split(",")?.forEach { optionText ->
                            val checkBox = CheckBox(itemView.context).apply { text = optionText; isChecked = savedOptions.contains(optionText) }
                            binding.multiselectContainer.addView(checkBox)
                        }
                    }
                }
            }
        }

        private fun getInputValue(activity: Activity): String {
            return when (activity.dataType) {
                "Text" -> binding.textInputEditText.text.toString().trim()
                "Number" -> binding.numberInputEditText.text.toString().trim()
                "Checkbox" -> binding.checkboxInput.isChecked.toString()
                "Multiselect" -> {
                    if (activity.isSingleSelection) {
                        (binding.multiselectContainer.getChildAt(0) as? RadioGroup)?.let { radioGroup ->
                            val checkedId = radioGroup.checkedRadioButtonId
                            if (checkedId != -1) {
                                itemView.findViewById<RadioButton>(checkedId)?.text.toString()
                            } else { "" }
                        } ?: ""
                    } else {
                        binding.multiselectContainer.children
                            .filterIsInstance<CheckBox>()
                            .filter { it.isChecked }
                            .map { it.text.toString() }
                            .joinToString(",")
                    }
                }
                else -> ""
            }
        }

        private fun isInputValid(value: String, activity: Activity): Boolean {
            binding.textInputLayout.error = null
            binding.numberInputLayout.error = null

            if (activity.dataType == "Text" && value.isBlank()) {
                binding.textInputLayout.error = "Cannot be blank"
                return false
            }
            if (activity.dataType == "Number") {
                val number = value.toBigDecimalOrNull()
                if (number == null) {
                    binding.numberInputLayout.error = "Please provide a number"
                    return false
                }
                if (activity.decimalPlaces != -1) {
                    if (value.contains(".") && number.scale() != activity.decimalPlaces) {
                        binding.numberInputLayout.error = "Must have exactly ${activity.decimalPlaces} decimal places"
                        return false
                    }
                    if (!value.contains(".") && activity.decimalPlaces > 0) {
                        binding.numberInputLayout.error = "Must have exactly ${activity.decimalPlaces} decimal places"
                        return false
                    }
                }
            }
            return true
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<DataActivityUiModel>() {
        override fun areItemsTheSame(oldItem: DataActivityUiModel, newItem: DataActivityUiModel): Boolean =
            oldItem.activity.id == newItem.activity.id

        override fun areContentsTheSame(oldItem: DataActivityUiModel, newItem: DataActivityUiModel): Boolean =
            oldItem == newItem
    }
}