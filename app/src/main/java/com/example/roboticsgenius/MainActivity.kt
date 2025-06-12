package com.example.roboticsgenius

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var activityDao: ActivityDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- Database and RecyclerView Setup ---
        db = AppDatabase.getDatabase(applicationContext)
        activityDao = db.activityDao()
        val adapter = ActivityAdapter()

        binding.recyclerViewActivities.adapter = adapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(this)

        // --- Observe data from database and update the list ---
        lifecycleScope.launch {
            activityDao.getAllActivities().collect { activities ->
                adapter.submitList(activities)
            }
        }

        // --- Floating Action Button click listener ---
        binding.fabAddActivity.setOnClickListener {
            showAddActivityDialog()
        }
    }

    private fun showAddActivityDialog() {
        val editText = EditText(this)
        editText.hint = "e.g. Work, Study, Gym"

        AlertDialog.Builder(this)
            .setTitle("Add New Activity")
            .setView(editText)
            .setPositiveButton("Add") { dialog, _ ->
                val name = editText.text.toString()
                if (name.isNotBlank()) {
                    lifecycleScope.launch {
                        activityDao.insert(Activity(name = name))
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}