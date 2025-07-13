package com.example.roboticsgenius

import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ClickableSpan
import android.view.View
import java.util.regex.Pattern

object NoteFormattingHelper {

    // THE FIX: Using Unicode characters for a cleaner look
    const val BULLET_CHAR = '•'
    const val CHECKBOX_UNCHECKED_CHAR = '☐'
    const val CHECKBOX_CHECKED_CHAR = '☑'
    private const val PREFIX_REGEX = "^([\\s]*)(?:([0-9]+[.])|([$BULLET_CHAR]))[\\s]"

    fun getLinePrefix(line: CharSequence): String? {
        val pattern = Pattern.compile(PREFIX_REGEX)
        val matcher = pattern.matcher(line)
        return if (matcher.find()) matcher.group(0) else null
    }

    fun getNextListPrefix(currentLine: String): String? {
        val pattern = Pattern.compile(PREFIX_REGEX)
        val matcher = pattern.matcher(currentLine)
        if (!matcher.find()) return null

        val indentation = matcher.group(1) ?: ""
        val numberGroup = matcher.group(2)
        val bulletGroup = matcher.group(3)

        return when {
            numberGroup != null -> {
                val number = numberGroup.removeSuffix(".").toIntOrNull() ?: 0
                "$indentation${number + 1}. "
            }
            bulletGroup != null -> "$indentation$BULLET_CHAR "
            else -> null
        }
    }

    fun applyListFormatting(editable: Editable, prefix: String, start: Int) {
        editable.insert(start, prefix)
    }

    fun applyCheckbox(editable: Editable, selectionStart: Int) {
        val textToInsert = "$CHECKBOX_UNCHECKED_CHAR "
        editable.insert(selectionStart, textToInsert)
        // After inserting, immediately apply the clickable span to the new checkbox
        reapplyAllSpans(editable)
    }

    fun reapplyAllSpans(editable: Spannable) {
        // Remove old spans to avoid duplicates
        val oldSpans = editable.getSpans(0, editable.length, CheckboxClickableSpan::class.java)
        for (span in oldSpans) {
            editable.removeSpan(span)
        }

        // Apply new spans
        var index = editable.indexOf(CHECKBOX_UNCHECKED_CHAR, 0)
        while (index >= 0) {
            editable.setSpan(CheckboxClickableSpan(), index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            index = editable.indexOf(CHECKBOX_UNCHECKED_CHAR, index + 1)
        }

        index = editable.indexOf(CHECKBOX_CHECKED_CHAR, 0)
        while (index >= 0) {
            editable.setSpan(CheckboxClickableSpan(), index, index + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            index = editable.indexOf(CHECKBOX_CHECKED_CHAR, index + 1)
        }
    }
}

class CheckboxClickableSpan : ClickableSpan() {
    override fun onClick(widget: View) {
        val tv = widget as? android.widget.TextView
        val text = tv?.text as? SpannableStringBuilder ?: return

        val spanStart = text.getSpanStart(this)
        if (spanStart == -1) return

        val currentText = text.subSequence(spanStart, spanStart + 1).toString()
        val newText = if (currentText[0] == NoteFormattingHelper.CHECKBOX_UNCHECKED_CHAR) {
            NoteFormattingHelper.CHECKBOX_CHECKED_CHAR.toString()
        } else {
            NoteFormattingHelper.CHECKBOX_UNCHECKED_CHAR.toString()
        }
        text.replace(spanStart, spanStart + 1, newText)

        // Re-apply spans after modification
        NoteFormattingHelper.reapplyAllSpans(text)
    }
}