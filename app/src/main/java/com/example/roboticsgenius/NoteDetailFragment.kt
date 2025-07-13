package com.example.roboticsgenius

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.roboticsgenius.databinding.FragmentNoteDetailBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NoteDetailFragment : Fragment() {

    private var _binding: FragmentNoteDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by activityViewModels()
    private val args: NoteDetailFragmentArgs by navArgs()

    private var currentNote: Note? = null
    private var autoSaveJob: Job? = null
    private var undoMenuItem: MenuItem? = null
    private var redoMenuItem: MenuItem? = null

    private val textChangeManager = TextChangeManager()
    private var isProgrammaticChange = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNoteDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenu()
        loadNote()
        setupTextWatchers()
        setupBackButton()

        binding.editTextContent.movementMethod = LinkMovementMethod.getInstance()

        textChangeManager.onUndoRedoStateChanged = { canUndo, canRedo ->
            undoMenuItem?.isEnabled = canUndo
            redoMenuItem?.isEnabled = canRedo
        }
    }

    private fun loadNote() {
        lifecycleScope.launch {
            currentNote = viewModel.getNoteById(args.noteId)
            currentNote?.let {
                binding.editTextTitle.setText(it.title)
                binding.editTextContent.setText(it.content)
                NoteFormattingHelper.reapplyAllSpans(binding.editTextContent.text)
                textChangeManager.setInitialState(it.content)
            }
        }
    }

    private fun setupTextWatchers() {
        val contentWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (isProgrammaticChange) return
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isProgrammaticChange) return

                if (count > before && s?.get(start) == '\n') {
                    handleEnterKeyPress(s, start)
                }

                NoteFormattingHelper.reapplyAllSpans(binding.editTextContent.text)
            }

            override fun afterTextChanged(s: Editable?) {
                if (isProgrammaticChange) return

                autoSaveJob?.cancel()
                autoSaveJob = lifecycleScope.launch {
                    delay(500)
                    saveNote()
                }
                textChangeManager.onTextChanged(s.toString())
            }
        }

        binding.editTextTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                autoSaveJob?.cancel()
                autoSaveJob = lifecycleScope.launch {
                    delay(500)
                    saveNote()
                }
            }
        })
        binding.editTextContent.addTextChangedListener(contentWatcher)
    }

    private fun handleEnterKeyPress(s: CharSequence, cursorPosition: Int) {
        val text = s.toString()
        val lineStart = text.lastIndexOf('\n', cursorPosition - 1) + 1
        val currentLine = text.substring(lineStart, cursorPosition)

        NoteFormattingHelper.getNextListPrefix(currentLine)?.let { prefix ->
            isProgrammaticChange = true
            binding.editTextContent.text.insert(cursorPosition + 1, prefix)
            isProgrammaticChange = false
        }
    }

    private fun saveNote() {
        val noteId = args.noteId
        val newTitle = binding.editTextTitle.text.toString()
        val newContent = binding.editTextContent.text.toString()

        if (newTitle.isNotBlank() || newContent.isNotBlank()) {
            viewModel.updateNote(noteId, newTitle, newContent)
        } else {
            viewModel.deleteNote(noteId)
            try { findNavController().popBackStack() } catch (e: Exception) { /* Ignore */ }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.note_detail_menu, menu)
                undoMenuItem = menu.findItem(R.id.action_undo)
                redoMenuItem = menu.findItem(R.id.action_redo)
                undoMenuItem?.isEnabled = textChangeManager.canUndo()
                redoMenuItem?.isEnabled = textChangeManager.canRedo()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_delete_note -> {
                        showDeleteConfirmation()
                        true
                    }
                    R.id.action_add_element -> {
                        showFormattingMenu(requireActivity().findViewById(R.id.action_add_element))
                        true
                    }
                    R.id.action_undo -> {
                        performUndo()
                        true
                    }
                    R.id.action_redo -> {
                        performRedo()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to permanently delete this note?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteNote(args.noteId)
                findNavController().popBackStack()
            }
            .show()
    }

    private fun performUndo() {
        val currentText = binding.editTextContent.text.toString()
        val previousText = textChangeManager.undo(currentText)
        if (previousText != null) {
            isProgrammaticChange = true
            binding.editTextContent.setText(previousText)
            binding.editTextContent.setSelection(previousText.length)
            NoteFormattingHelper.reapplyAllSpans(binding.editTextContent.text)
            isProgrammaticChange = false
        }
    }

    private fun performRedo() {
        val currentText = binding.editTextContent.text.toString()
        val nextText = textChangeManager.redo(currentText)
        if (nextText != null) {
            isProgrammaticChange = true
            binding.editTextContent.setText(nextText)
            binding.editTextContent.setSelection(nextText.length)
            NoteFormattingHelper.reapplyAllSpans(binding.editTextContent.text)
            isProgrammaticChange = false
        }
    }


    private fun showFormattingMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Bullet List")
        popup.menu.add("Numbered List")
        popup.menu.add("Checkbox")

        popup.setOnMenuItemClickListener { item ->
            val selectionStart = binding.editTextContent.selectionStart.coerceAtLeast(0)
            val editable = binding.editTextContent.text

            when (item.title) {
                "Bullet List" -> editable.insert(selectionStart, "${NoteFormattingHelper.BULLET_CHAR} ")
                "Numbered List" -> editable.insert(selectionStart, "1. ")
                "Checkbox" -> NoteFormattingHelper.applyCheckbox(editable, selectionStart)
            }
            true
        }
        popup.show()
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            autoSaveJob?.cancel()
            saveNote()
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        autoSaveJob?.cancel()
        saveNote()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        undoMenuItem = null
        redoMenuItem = null
        _binding = null
    }
}