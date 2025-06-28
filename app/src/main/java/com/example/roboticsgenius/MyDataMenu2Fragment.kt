package com.example.roboticsgenius
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.roboticsgenius.databinding.FragmentPlaceholderBinding

class MyDataMenu2Fragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        binding.placeholderText.text = "Menu 2 Experimental"
        return binding.root
    }
}