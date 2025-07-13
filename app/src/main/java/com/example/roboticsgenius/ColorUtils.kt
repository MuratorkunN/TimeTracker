package com.example.roboticsgenius

import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes

/**
 * Resolves a color attribute from the current theme and returns its integer value.
 * @param attr The attribute resource ID (e.g., com.google.android.material.R.attr.colorSurface).
 */
fun Context.resolveThemeColor(@AttrRes attr: Int): Int {
    val typedValue = TypedValue()
    theme.resolveAttribute(attr, typedValue, true)
    return typedValue.data
}