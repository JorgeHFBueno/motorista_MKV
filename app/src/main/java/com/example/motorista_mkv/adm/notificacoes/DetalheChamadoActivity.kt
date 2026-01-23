package com.example.motorista_mkv.adm.notificacoes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class DetalheChamadoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var backButton: Button

    // Lista de notificações e Adapter
    private val listaNotificacoes = mutableListOf<NotificacaoModel>()
    private lateinit var notificacaoAdapter: NotificacaoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhe_chamado)

        // Instancia o Firestore
        firestore = FirebaseFirestore.getInstance()

        // Obtém referências dos componentes da tela
        backButton = findViewById(R.id.backButton)
        val recyclerView: RecyclerView = findViewById(R.id.chamadoRecyclerView)

        // Configura o botão de voltar
        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 1000)
            finish()
        }

        // Configuração do RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        notificacaoAdapter = NotificacaoAdapter(listaNotificacoes)
        recyclerView.adapter = notificacaoAdapter

        // Carrega notificações em tempo real
        carregarNotificacoesAbertas()
    }

    private fun carregarNotificacoesAbertas() {
        firestore.collection("00-chamados")
            .whereEqualTo("status", "ABERTO")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // Log.e("DetalheChamado", "Erro: $error")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    listaNotificacoes.clear()
                    for (doc in snapshots.documents) {
                        val chamadoId = doc.id
                        // Lê o Timestamp e converte para String no formato desejado
                        val timestamp = doc.getTimestamp("data")
                        val dataString = if (timestamp != null) {
                            val date = timestamp.toDate()
                            val dateFormat = SimpleDateFormat("dd/MM - HH:mm", Locale.getDefault())
                            dateFormat.format(date)
                        } else {
                            ""
                        }

                        // Campos inteiros
                        val dfrncl = doc.getLong("dfrncl")?.toInt() ?: 0
                        val mntntInfrmd = doc.getLong("mntntInfrmd")?.toInt() ?: 0
                        val montanteInicial = doc.getLong("montanteInicial")?.toInt() ?: 0

                        // Campos string
                        val tipo = doc.getString("tipo") ?: ""
                        val motorista = doc.getString("motorista") ?: ""

                        val notificacao = NotificacaoModel(
                            chamadoId = chamadoId,
                            data = dataString,
                            tipo = tipo,
                            mntntInfrmd = mntntInfrmd,
                            dfrncl = dfrncl,
                            montanteInicial = montanteInicial,
                            motorista = motorista
                        )
                        listaNotificacoes.add(notificacao)
                    }
                    notificacaoAdapter.notifyDataSetChanged()
                }
            }
    }


    private fun disableAllButtons() {
        backButton.isEnabled = false
    }

    private fun enableAllButtons() {
        backButton.isEnabled = true
    }
}
