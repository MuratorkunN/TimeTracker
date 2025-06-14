// app/src/main/java/com/example/roboticsgenius/EditTargetFragment.kt

package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.roboticsgenius.databinding.FragmentEditTargetBinding
import kotlinx.coroutines.launch

class EditTargetFragment : DialogFragment() {

    private var _binding: FragmentEditTargetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by activityViewModels()
    private var activityToEdit: Activity? = null

    companion object {
        private const val ARG_ACTIVITY_ID = "activity_id"

        fun newInstance(activityId: Int): EditTargetFragment {
            val fragment = EditTargetFragment()
            val args = Bundle()
            args.putInt(ARG_ACTIVITY_ID, activityId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditTargetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activityId = arguments?.getInt(ARG_ACTIVITY_ID) ?: -1

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(requireContext())
            activityToEdit = db.activityDao().getActivityById(activityId)
            activityToEdit?.let { setupInitialValues(it) }
        }

        binding.btnCancel.setOnClickListener { dismiss() }
        binding.btnUpdate.setOnClickListener { updateActivity() }
    }

    private fun setupInitialValues(activity: Activity) {
        binding.textViewEditTargetTitle.text = "Edit Target for '${activity.name}'"
        setupNumberPickers()
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
    }

    private fun setupNumberPickers() {
        binding.pickerHours.minValue = 0; binding.pickerHours.maxValue = 23
        binding.pickerMinutes.minValue = 0; binding.pickerMinutes.maxValue = 59
        binding.pickerSeconds.minValue = 0; binding.pickerSeconds.maxValue = 59
    }

    private fun setTime(h: Int, m: Int, s: Int) {
        binding.pickerHours.value = h
        binding.pickerMinutes.value = m
        binding.pickerSeconds.value = s
    }

    private fun updateActivity() {
        val currentActivity = activityToEdit ?: return
        val newDuration = binding.pickerHours.value * 3600 + binding.pickerMinutes.value * 60 + binding.pickerSeconds.value
        val newPeriod = when (binding.toggleGroupPeriod.checkedButtonId) {
            R.id.btnDaily -> "Daily"
            R.id.btnWeekly -> "Weekly"
            R.id.btnMonthly -> "Monthly"
            else -> "Weekly"
        }

        val updatedActivity = currentActivity.copy(
            targetDurationSeconds = newDuration,
            targetPeriod = newPeriod
        )

        viewModel.updateActivity(updatedActivity)
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}