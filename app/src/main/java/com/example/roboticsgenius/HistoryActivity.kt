package com.example.roboticsgenius

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.ActivityHistoryBinding
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // --- SETUP ---
        db = AppDatabase.getDatabase(applicationContext)
        val historyAdapter = HistoryAdapter()

        binding.recyclerViewHistory.apply {
            adapter = historyAdapter
            layoutManager = LinearLayoutManager(this@HistoryActivity)
        }

        // --- OBSERVE AND SUBMIT DATA ---
        lifecycleScope.launch {
            db.activityDao().getAllLogsWithActivityNames().collect { logList ->
                // This is where the magic happens.
                // Every time the data changes in the DB, this list will update automatically.
                historyAdapter.submitList(logList)
            }
        }
    }
}