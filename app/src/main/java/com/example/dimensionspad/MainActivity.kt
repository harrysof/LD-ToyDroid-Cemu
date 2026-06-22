package com.example.dimensionspad

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.dimensionspad.databinding.ActivityMainBinding
import com.example.dimensionspad.databinding.ItemSlotBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val apiClient = PadApiClient("http://localhost:8031")
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SlotAdapter
    private val slots = mutableListOf<PadSlot>()
    
    private var pollingJob: Job? = null
    private var hasHost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastHost = prefs.getString("last_host", null)
        if (lastHost != null) {
            binding.editHost.setText(lastHost)
        }

        adapter = SlotAdapter(
            items = slots,
            onActionClick = this::onSlotActionClicked,
            onMoveClick = this::onMoveClicked
        )
        
        binding.recyclerSlots.layoutManager = LinearLayoutManager(this)
        binding.recyclerSlots.adapter = adapter

        binding.btnConnect.setOnClickListener {
            val host = binding.editHost.text.toString().trim()
            if (host.isNotEmpty()) {
                apiClient.setHost(host)
                hasHost = true
                prefs.edit().putString("last_host", host).apply()
                refreshSlots()
                startPolling()
            } else {
                Toast.makeText(this, "Enter a valid IP address", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnRefresh.setOnClickListener {
            refreshSlots()
        }
    }

    override fun onResume() {
        super.onResume()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        pollingJob?.cancel()
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                if (hasHost) {
                    try {
                        fetchAndUpdateSlots()
                    } catch (e: Exception) {
                        // Silently ignore polling failures
                    }
                }
                delay(3000)
            }
        }
    }

    private suspend fun fetchAndUpdateSlots() {
        val newSlots = apiClient.listSlots()
        slots.clear()
        slots.addAll(newSlots.map { slot ->
            if (slot.occupied && slot.figureName.isBlank()) {
                val localName = FigureCatalogue.all.find { it.id == slot.figureId }?.name ?: "Unknown"
                slot.copy(figureName = localName)
            } else {
                slot
            }
        }.sortedBy { it.index })
        adapter.notifyDataSetChanged()
    }

    private fun refreshSlots(silent: Boolean = false) {
        lifecycleScope.launch {
            try {
                fetchAndUpdateSlots()
            } catch (e: Exception) {
                if (!silent) {
                    Toast.makeText(this@MainActivity, "Failed to refresh: ${e.message}", Toast.LENGTH_LONG).show()
                }
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

    private fun onMoveClicked(slot: PadSlot) {
        val otherSlots = (0..6).filter { it != slot.index }
        val slotNames = otherSlots.map { getPositionName(it) }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Move To...")
            .setItems(slotNames) { _, which ->
                val toSlot = otherSlots[which]
                lifecycleScope.launch {
                    try {
                        val res = apiClient.move(slot.index, toSlot)
                        if (res.ok) {
                            refreshSlots()
                        } else {
                            Toast.makeText(this@MainActivity, "Move failed: ${res.error}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun getPositionName(index: Int): String {
        return when(index) {
            0 -> "Center Left Hero"
            1 -> "Center Main Hero"
            2 -> "Center Right Hero"
            3 -> "Left Adventure"
            4 -> "Left Ability"
            5 -> "Right Adventure"
            6 -> "Right Ability"
            else -> "Slot $index"
        }
    }

    private inner class SlotAdapter(
        private val items: List<PadSlot>,
        private val onActionClick: (PadSlot) -> Unit,
        private val onMoveClick: (PadSlot) -> Unit
    ) : RecyclerView.Adapter<SlotAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemSlotBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val slot = items[position]
            
            holder.binding.textSlotName.text = "Pad ${slot.pad} - ${getPositionName(slot.index)}"
            
            // Set zone color
            val colorRes = when(slot.index) {
                0, 1, 2 -> R.color.zone_center_blue
                3, 4 -> R.color.zone_left_green
                5, 6 -> R.color.zone_right_orange
                else -> android.R.color.white
            }
            holder.binding.root.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, colorRes))
            
            if (slot.occupied) {
                holder.binding.textFigureName.text = slot.figureName
                holder.binding.btnAction.text = "Remove"
                // Red tint for remove
                holder.binding.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(android.R.color.holo_red_dark)
                )
                holder.binding.btnMove.visibility = View.VISIBLE
            } else {
                holder.binding.textFigureName.text = "Empty"
                holder.binding.btnAction.text = "Place Figure"
                // Default accent tint for place
                holder.binding.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    holder.itemView.context.getColor(R.color.purple_500)
                )
                holder.binding.btnMove.visibility = View.GONE
            }

            holder.binding.btnAction.setOnClickListener { onActionClick(slot) }
            holder.binding.btnMove.setOnClickListener { onMoveClick(slot) }
        }

        override fun getItemCount() = items.size
    }
}
