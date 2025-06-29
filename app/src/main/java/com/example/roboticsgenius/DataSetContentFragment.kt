// app/src/main/java/com/example/roboticsgenius/DataSetContentFragment.kt
package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.roboticsgenius.databinding.FragmentDatasetContentBinding

class DataSetContentFragment : Fragment() {

    private var _binding: FragmentDatasetContentBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val ARG_DATASET_NAME = "dataset_name"

        fun newInstance(dataSetName: String): DataSetContentFragment {
            return DataSetContentFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATASET_NAME, dataSetName)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDatasetContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val dataSetName = arguments?.getString(ARG_DATASET_NAME) ?: "Error"
        binding.textViewContent.text = "$dataSetName experimental page"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}