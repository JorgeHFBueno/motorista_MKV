package com.example.motorista_mkv.adm.notificacoes

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R

class NotificacaoAdapter(private val notificacoes: List<NotificacaoModel>)
    : RecyclerView.Adapter<NotificacaoAdapter.NotificacaoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificacaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificacao, parent, false)
        return NotificacaoViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificacaoViewHolder, position: Int) {
        val notificacao = notificacoes[position]
        holder.bind(notificacao)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ConflitoMontanteActivity::class.java)
            intent.putExtra("CHAMADO_ID", notificacao.chamadoId) // Passa o ID do doc
            context.startActivity(intent)
        }

    }

    override fun getItemCount(): Int {
        return notificacoes.size
    }

    class NotificacaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewData: TextView = itemView.findViewById(R.id.textViewData)
        private val textViewTipo: TextView = itemView.findViewById(R.id.textViewTipo)
        private val textViewMntntInfrmd: TextView = itemView.findViewById(R.id.textViewMntntInfrmd)
        private val textViewDfrncl: TextView = itemView.findViewById(R.id.textViewDfrncl)
        private val textViewMontanteInicial: TextView = itemView.findViewById(R.id.textViewMontanteInicial)
        private val textViewMotorista: TextView = itemView.findViewById(R.id.textViewMotorista)

        fun bind(notificacao: NotificacaoModel) {
            textViewData.text = notificacao.data
            textViewTipo.text = notificacao.tipo
            textViewMntntInfrmd.text = "M. Informado: ${notificacao.mntntInfrmd}"
            textViewDfrncl.text = "Diferença: ${notificacao.dfrncl}"
            textViewMontanteInicial.text = "M. Atual: ${notificacao.montanteInicial}"
            textViewMotorista.text = notificacao.motorista
        }
    }
}
