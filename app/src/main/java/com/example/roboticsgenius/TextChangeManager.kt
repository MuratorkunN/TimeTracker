package com.example.roboticsgenius

import java.util.Stack

class TextChangeManager {

    private val undoStack = Stack<String>()
    private val redoStack = Stack<String>()
    private var isUndoingOrRedoing = false

    var onUndoRedoStateChanged: ((canUndo: Boolean, canRedo: Boolean) -> Unit)? = null

    fun onTextChanged(newText: String) {
        if (isUndoingOrRedoing) {
            isUndoingOrRedoing = false
            return
        }
        // Don't push duplicates
        if (undoStack.isNotEmpty() && undoStack.peek() == newText) return

        undoStack.push(newText)
        redoStack.clear() // Any new change clears the redo history
        notifyStateChange()
    }

    fun undo(currentText: String): String? {
        if (undoStack.isEmpty()) return null

        isUndoingOrRedoing = true
        redoStack.push(currentText)
        val previousText = undoStack.pop()
        notifyStateChange()
        return previousText
    }

    fun redo(currentText: String): String? {
        if (redoStack.isEmpty()) return null

        isUndoingOrRedoing = true
        undoStack.push(currentText)
        val nextText = redoStack.pop()
        notifyStateChange()
        return nextText
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    private fun notifyStateChange() {
        onUndoRedoStateChanged?.invoke(canUndo(), canRedo())
    }

    fun setInitialState(text: String) {
        undoStack.clear()
        redoStack.clear()
        undoStack.push(text) // Start with the initial text
        notifyStateChange()
    }
}