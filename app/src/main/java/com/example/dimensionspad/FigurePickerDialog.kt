package com.example.dimensionspad

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dimensionspad.databinding.DialogFigurePickerBinding
import com.example.dimensionspad.databinding.ItemFigureBinding

class FigurePickerDialog(private val onFigureSelected: (FigureCatalogue.Figure) -> Unit) : androidx.fragment.app.DialogFragment() {

    private lateinit var adapter: FigureAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    private var _binding: DialogFigurePickerBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFigurePickerBinding.inflate(inflater, container, false)
        
        adapter = FigureAdapter(FigureCatalogue.all) { figure ->
            onFigureSelected(figure)
            dismiss()
        }
        
        binding.recyclerFigures.layoutManager = LinearLayoutManager(context)
        binding.recyclerFigures.adapter = adapter
        
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText)
                return true
            }
        })
        
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    private class FigureAdapter(
        private val allFigures: List<FigureCatalogue.Figure>,
        private val onClick: (FigureCatalogue.Figure) -> Unit
    ) : RecyclerView.Adapter<FigureAdapter.ViewHolder>() {

        private var filteredFigures = allFigures

        fun filter(query: String?) {
            filteredFigures = if (query.isNullOrBlank()) {
                allFigures
            } else {
                allFigures.filter {
                    it.name.contains(query, ignoreCase = true) || 
                    it.id.toString().contains(query)
                }
            }
            notifyDataSetChanged()
        }

        class ViewHolder(val binding: ItemFigureBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemFigureBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val figure = filteredFigures[position]
            holder.binding.textFigureName.text = figure.name
            holder.binding.textFigureId.text = "ID: ${figure.id}"
            holder.itemView.setOnClickListener { onClick(figure) }
        }

        override fun getItemCount() = filteredFigures.size
    }
}
