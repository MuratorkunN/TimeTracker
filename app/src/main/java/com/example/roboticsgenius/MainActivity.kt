package com.example.roboticsgenius

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var activityDao: ActivityDao

    // --- NEW: Permission handling ---
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. We can now start the service.
            } else {
                // Explain to the user that the feature is unavailable
            }
        }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    // --- END NEW ---

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        askForNotificationPermission() // Ask for permission on start

        db = AppDatabase.getDatabase(applicationContext)
        activityDao = db.activityDao()
        val adapter = ActivityAdapter { activity ->
            startTimerForActivity(activity)
        }

        binding.recyclerViewActivities.adapter = adapter
        binding.recyclerViewActivities.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            activityDao.getAllActivities().collect { activities ->
                adapter.submitList(activities)
            }
        }

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

    private fun startTimerForActivity(activity: Activity) {
        val serviceIntent = Intent(this, TimerService::class.java)
        startService(serviceIntent)
    }
}