package com.example.motorista_mkv.adm.combustivel

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R
import java.text.SimpleDateFormat
import java.util.Locale

class BombAdapter(
    private val bombList: List<bombModel>,
    private val onItemClick: (bombModel) -> Unit
) : RecyclerView.Adapter<BombAdapter.BombViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BombViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bomb, parent, false)
        return BombViewHolder(view)
    }

    override fun onBindViewHolder(holder: BombViewHolder, position: Int) {
        val bomba = bombList[position]
        holder.bind(bomba)

        // Dispara o callback ao clicar em um item inteiro
        holder.itemView.setOnClickListener {
            onItemClick(bomba)
        }
    }

    override fun getItemCount(): Int = bombList.size

    inner class BombViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dataTextView: TextView = itemView.findViewById(R.id.tvData)
        private val placaTextView: TextView = itemView.findViewById(R.id.tvPlaca)
        private val MFTextView: TextView    = itemView.findViewById(R.id.tvMF)
        private val QATextView: TextView= itemView.findViewById(R.id.tvQA)

        fun bind(bomba: bombModel) {
            // Formata data se existir
            bomba.data?.let {
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dataTextView.text = dateFormat.format(it.toDate())
            } ?: run {
                dataTextView.text = "Data: --/--"
            }

            placaTextView.text = "${bomba.placa ?: "--"}"
            MFTextView.text    = formatWithComma(bomba.lf)
            QATextView.text = formatWithComma(bomba.qa)
        }

        private fun formatWithComma(value: Long?): String {
            return value?.toString()?.let {
                if (it.length > 1) {
                    val beforeComma = it.substring(0, it.length - 1)
                    val afterComma = it.substring(it.length - 1)
                    "$beforeComma,$afterComma"
                } else {
                    "0,$it"
                }
            } ?: "--"
        }
    }
}