// app/src/main/java/com/example/roboticsgenius/AddDataFragment.kt
package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.roboticsgenius.databinding.FragmentAddDataBinding
import kotlinx.coroutines.launch

class AddDataFragment : Fragment() {
    private var _binding: FragmentAddDataBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ActivitiesViewModel by activityViewModels()
    private val ADD_DATA_SET_ID = -99

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            viewModel.allDataSets.collect { dataSets ->
                if (dataSets.isEmpty()) {
                    showEmptyState()
                } else {
                    showContentState()
                    setupBottomNavigation(dataSets)
                    if (childFragmentManager.findFragmentById(R.id.data_set_content_container) == null) {
                        showDataSetContent(dataSets.first())
                    }
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.emptyStateText.text = "Add Data Set below to start adding data"
        binding.emptyStateText.isVisible = true
        binding.dataSetContentContainer.isVisible = false
        // Still show the nav bar so the user can add a set
        val menu = binding.bottomNavigationAddData.menu
        menu.clear()
        menu.add(Menu.NONE, ADD_DATA_SET_ID, 0, "Add Data Set").setIcon(R.drawable.ic_nav_add_data)
        binding.bottomNavigationAddData.setOnItemSelectedListener {
            AddDataSetFragment.newInstance().show(parentFragmentManager, "AddDataSetDialog")
            false
        }
    }

    private fun showContentState() {
        binding.emptyStateText.isVisible = false
        binding.dataSetContentContainer.isVisible = true
    }

    private fun getResourceIdForIconName(iconName: String): Int {
        val resourceId = requireContext().resources.getIdentifier(iconName, "drawable", requireContext().packageName)
        return if (resourceId == 0) R.drawable.ic_nav_my_data else resourceId
    }

    private fun setupBottomNavigation(dataSets: List<DataSet>) {
        val menu = binding.bottomNavigationAddData.menu
        menu.clear()

        dataSets.forEachIndexed { index, dataSet ->
            // FIX: Use the correct icon
            val iconResId = getResourceIdForIconName(dataSet.iconName)
            menu.add(Menu.NONE, dataSet.id, index, dataSet.name).setIcon(iconResId)
        }
        menu.add(Menu.NONE, ADD_DATA_SET_ID, dataSets.size, "Add Set").setIcon(R.drawable.ic_nav_add_data)

        binding.bottomNavigationAddData.setOnItemSelectedListener { item ->
            if (item.itemId == ADD_DATA_SET_ID) {
                AddDataSetFragment.newInstance().show(parentFragmentManager, "AddDataSetDialog")
                return@setOnItemSelectedListener false
            } else {
                val selectedDataSet = dataSets.find { it.id == item.itemId }
                selectedDataSet?.let { showDataSetContent(it) }
                return@setOnItemSelectedListener true
            }
        }

        if (dataSets.isNotEmpty() && binding.bottomNavigationAddData.selectedItemId != dataSets.first().id) {
            binding.bottomNavigationAddData.selectedItemId = dataSets.first().id
        }
    }

    private fun showDataSetContent(dataSet: DataSet) {
        // NEW: This will now show the new content fragment
        val fragment = AddDataContentFragment.newInstance(dataSet.id)
        childFragmentManager.commit {
            replace(R.id.data_set_content_container, fragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}