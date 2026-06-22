package com.example.dimensionspad

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val apiClient = PadApiClient("http://localhost:8031")
    private lateinit var adapter: SlotAdapter
    private val slots = mutableListOf<PadSlot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editHost = findViewById<EditText>(R.id.editHost)
        val btnConnect = findViewById<Button>(R.id.btnConnect)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)
        val recycler = findViewById<RecyclerView>(R.id.recyclerSlots)

        adapter = SlotAdapter(slots, this::onSlotActionClicked)
        
        // 7 slots total. We can just use a LinearLayoutManager, or a GridLayoutManager.
        // Let's use a LinearLayoutManager for simplicity.
        recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        recycler.adapter = adapter

        btnConnect.setOnClickListener {
            val host = editHost.text.toString().trim()
            if (host.isNotEmpty()) {
                apiClient.setHost(host)
                refreshSlots()
            } else {
                Toast.makeText(this, "Enter a valid IP address", Toast.LENGTH_SHORT).show()
            }
        }

        btnRefresh.setOnClickListener {
            refreshSlots()
        }
    }

    private fun refreshSlots() {
        lifecycleScope.launch {
            try {
                val newSlots = apiClient.listSlots()
                slots.clear()
                // Sort by index so they are in consistent order 0..6
                slots.addAll(newSlots.sortedBy { it.index })
                adapter.notifyDataSetChanged()
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Failed to refresh: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onSlotActionClicked(slot: PadSlot) {
        if (slot.occupied) {
            // Remove
            lifecycleScope.launch {
                try {
                    val res = apiClient.remove(slot.index)
                    if (res.ok) {
                        refreshSlots()
                    } else {
                        Toast.makeText(this@MainActivity, "Remove failed: ${res.error}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Place -> show picker
            val picker = FigurePickerDialog { figure ->
                lifecycleScope.launch {
                    try {
                        val res = apiClient.place(slot.index, figure.id)
                        if (res.ok) {
                            refreshSlots()
                        } else {
                            Toast.makeText(this@MainActivity, "Place failed: ${res.error}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            picker.show(supportFragmentManager, "FigurePicker")
        }
    }

    private class SlotAdapter(
        private val items: List<PadSlot>,
        private val onActionClick: (PadSlot) -> Unit
    ) : RecyclerView.Adapter<SlotAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val slotName: TextView = view.findViewById(R.id.textSlotName)
            val figureName: TextView = view.findViewById(R.id.textFigureName)
            val btnAction: Button = view.findViewById(R.id.btnAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val slot = items[position]
            
            val positionName = when(slot.index) {
                0 -> "Center Left Hero"
                1 -> "Center Main Hero"
                2 -> "Center Right Hero"
                3 -> "Left Adventure"
                4 -> "Left Ability"
                5 -> "Right Adventure"
                6 -> "Right Ability"
                else -> "Slot ${slot.index}"
            }
            
            holder.slotName.text = "Pad ${slot.pad} - $positionName"
            
            if (slot.occupied) {
                holder.figureName.text = slot.figureName
                holder.btnAction.text = "Remove"
                // Red tint for remove
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
            } else {
                holder.figureName.text = "Empty"
                holder.btnAction.text = "Place Figure"
                // Default accent tint for place
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.purple_500)
                )
            }

            holder.btnAction.setOnClickListener { onActionClick(slot) }
        }

        override fun getItemCount() = items.size
    }
}
