package com.example.motorista_mkv.motorista

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private val historyList: List<HistoryModel>,
    private val onItemClick: (HistoryModel) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCollectionType: TextView = itemView.findViewById(R.id.tvCollectionType)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvPlaca: TextView = itemView.findViewById(R.id.tvPlaca)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val history = historyList[position]

        // Tipo da coleção
        holder.tvCollectionType.text = history.collectionType

        // Data formatada
        val dateStr = history.data?.let {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dateFormat.format(it.toDate())
        } ?: "--"
        holder.tvDate.text = dateStr

        // Placa
        holder.tvPlaca.text = history.placa

        // Clique no item
        holder.itemView.setOnClickListener {
            onItemClick(history)
        }
    }

    override fun getItemCount(): Int = historyList.size
}
