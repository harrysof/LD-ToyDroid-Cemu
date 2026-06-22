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

class FigurePickerDialog(private val onFigureSelected: (FigureCatalogue.Figure) -> Unit) : androidx.fragment.app.DialogFragment() {

    private lateinit var adapter: FigureAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_figure_picker, container, false)
        
        val searchView = view.findViewById<SearchView>(R.id.searchView)
        val recycler = view.findViewById<RecyclerView>(R.id.recyclerFigures)
        
        adapter = FigureAdapter(FigureCatalogue.all) { figure ->
            onFigureSelected(figure)
            dismiss()
        }
        
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = adapter
        
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText)
                return true
            }
        })
        
        return view
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

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.textFigureName)
            val id: TextView = view.findViewById(R.id.textFigureId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_figure, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val figure = filteredFigures[position]
            holder.name.text = figure.name
            holder.id.text = "ID: ${figure.id}"
            holder.itemView.setOnClickListener { onClick(figure) }
        }

        override fun getItemCount() = filteredFigures.size
    }
}
