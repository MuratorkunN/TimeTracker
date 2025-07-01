package com.example.roboticsgenius

/**
 * A simple data class to hold all the necessary information for a CSV export.
 * @param headers A list of strings for the first row (e.g., "Date", "Activity 1", "Activity 2").
 * @param rows A list of lists, where each inner list represents a data row.
 */
data class CsvExportData(
    val headers: List<String>,
    val rows: List<List<String>>
)