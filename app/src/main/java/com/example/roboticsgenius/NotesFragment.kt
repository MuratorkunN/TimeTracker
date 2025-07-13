package com.example.roboticsgenius

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.roboticsgenius.databinding.FragmentNotesBinding
import kotlinx.coroutines.launch

class NotesFragment : Fragment() {
    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NotesViewModel by activityViewModels()
    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.fabCreateNote.setOnClickListener {
            // Updated to navigate after creation
            viewModel.createNote("New Note", "#BA55D3") { newNoteId ->
                val action = NotesFragmentDirections.actionNotesFragmentToNoteDetailFragment(newNoteId)
                findNavController().navigate(action)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allNotes.collect { notes ->
                    notesAdapter.submitList(notes)
                    binding.emptyStateText.isVisible = notes.isEmpty()
                    binding.recyclerViewNotes.isVisible = notes.isNotEmpty()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter { note ->
            val action = NotesFragmentDirections.actionNotesFragmentToNoteDetailFragment(note.id)
            findNavController().navigate(action)
        }
        binding.recyclerViewNotes.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}