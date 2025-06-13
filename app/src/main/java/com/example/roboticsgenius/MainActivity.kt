package com.example.roboticsgenius

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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

        // --- NEW: Set up the toolbar ---
        setSupportActionBar(binding.toolbar)

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

    private fun startTimerForActivity(activity: Activity) {
        if (TimerService.isRunning) return
        val serviceIntent = Intent(this, TimerService::class.java)
        serviceIntent.putExtra("ACTIVITY_ID", activity.id)
        startService(serviceIntent)
    }

    private fun showAddActivityDialog() { /* ... same as before ... */ }

    // --- NEW: Inflate the menu ---
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // --- NEW: Handle menu item clicks ---
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}