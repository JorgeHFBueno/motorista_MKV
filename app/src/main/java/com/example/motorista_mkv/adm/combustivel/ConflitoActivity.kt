package com.example.motorista_mkv.adm.combustivel

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.motorista_mkv.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import com.example.motorista_mkv.data.BombasRepository

class ConflitoActivity : AppCompatActivity() {

    private lateinit var abrirChamadoButton: Button
    private lateinit var cancelarButton: Button

    private lateinit var diferencialTextView: TextView
    private lateinit var novaLitragemTextView: TextView
    private lateinit var dataUltimoRegistroTextView: TextView

    private lateinit var progressDialog: ProgressDialog
    private lateinit var firestore: FirebaseFirestore
    private val PREFS_NAME = "LoginPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conflito)

        // Inicializa Firestore
        firestore = FirebaseFirestore.getInstance()

        // Inicializa elementos de UI
        abrirChamadoButton = findViewById(R.id.btnVerificarNovamente)
        cancelarButton = findViewById(R.id.btnCancelar)
        diferencialTextView = findViewById(R.id.tvLitragemAtual)
        novaLitragemTextView = findViewById(R.id.tvNovaLitragem)
        dataUltimoRegistroTextView = findViewById(R.id.tvData)

        // Inicializa ProgressDialog
        progressDialog = ProgressDialog(this).apply {
            setMessage("...")
            setCancelable(false)
        }

        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")
        if (adminStatus == "adm2") {
            // Usuário autorizado
            diferencialTextView.visibility = View.VISIBLE
        } else {
            // Usuário não autorizado: Habilita fuelButton, desabilita admButton
            diferencialTextView.visibility = View.GONE
        }

        // Recebe valores da Intent
        val diferencial = intent.getIntExtra("DIFERENCIAL", -1)
        val novaLitragem = intent.getIntExtra("LITRAGEM_NOVA", -1)

        if (diferencial != -1 && novaLitragem != -1) {
            diferencialTextView.text = "A diferença no montante é de: ${formatValueWithComma(diferencial)} L"
            novaLitragemTextView.text = "Seu Montante: ${formatValueWithComma(novaLitragem)} L"
        } else {
            // Se os valores não foram passados, busca no Firestore
            buscarUltimoChamado()
        }
        abrirChamadoButton.setOnClickListener {
            verificarSeDadosEstaoCorretos(novaLitragem)
        }
        cancelarButton.setOnClickListener {
            finish() // Fecha a Activity
        }
    }

    private fun buscarUltimoChamado() {
        progressDialog.show()

        // Busca o último chamado do tipo "Conflito no Montante" e status "ABERTO"
        firestore.collection("00-chamados")
            .whereEqualTo("tipo", "Conflito no Montante")
            .whereEqualTo("status", "ABERTO")
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { querySnapshot ->
                progressDialog.dismiss()
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    val diferencial = document.getLong("dfrncl")?.toInt() ?: 0
                    val novaLitragem = document.getLong("mntntInfrmd")?.toInt() ?: 0
                    val dataRegistro = document.getTimestamp("data")?.toDate()

                    // Atualiza os TextViews com os valores recuperados
                    diferencialTextView.text = "A diferença no montante é de: ${formatValueWithComma(diferencial)} L"
                    novaLitragemTextView.text = "Seu Montante: ${formatValueWithComma(novaLitragem)} L"

                    // Formata a data antes de exibi-la
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val dataFormatada = dateFormat.format(dataRegistro ?: Date())

                    dataUltimoRegistroTextView.text = "Data do último registro: $dataFormatada"
                } else {
                    Toast.makeText(this, "Nenhum chamado em aberto foi encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Erro ao buscar chamado: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatValueWithComma(value: Int): String {
        val rawString = value.toString()
        return if (rawString.length > 1) {
            val beforeComma = rawString.substring(0, rawString.length - 1)
            val afterComma = rawString.substring(rawString.length - 1)
            "$beforeComma,$afterComma"
        } else {
            "0,$rawString"
        }
    }

    /**
     * Faz nova consulta à coleção "03-combustivel" para obter o último registro.
     * Caso a diferença entre novaLitragem e ultimaLitragem seja <= 9,
     * segue para FuelActivity. Caso contrário, ainda permanece em conflito.
     */
    private fun verificarSeDadosEstaoCorretos(novaLitragem: Int) {
        progressDialog.setMessage("Verificando dados...")
        progressDialog.show()

        fun handleVerification(ultimaLitragem: Int, ultimaDataDate: Date?) {
            val formatador = java.text.SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            val dataFormatada = if (ultimaDataDate != null) {
                formatador.format(ultimaDataDate)
            } else {
                "Data não disponível"
            }
            dataUltimoRegistroTextView.text = "Data do último registro: $dataFormatada"

            val diferenca = kotlin.math.abs(novaLitragem - ultimaLitragem)
            if (diferenca <= 9) {
                val intent = Intent(this, FuelActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(
                    this,
                    "Diferença ainda muito alta! (Diferença = ${formatValueWithComma(diferenca)} L)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun runFallbackQuery() {
            val query = firestore.collection("03-combustivel")
                .orderBy("data", Query.Direction.DESCENDING)
                .limit(1)

            query.get()
                .addOnSuccessListener { querySnapshot ->
                    progressDialog.dismiss()
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        val ultimaLitragem = document.getLong("lf")?.toInt() ?: 0
                        val ultimaDataDate = document.getTimestamp("data")?.toDate()
                        handleVerification(ultimaLitragem, ultimaDataDate)
                    } else {
                        Toast.makeText(this, "Nenhum dado encontrado em 03-combustivel.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Toast.makeText(this, "Falha ao buscar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        BombasRepository.fetchMontanteAtualComUltimoAbastecimento(
            firestore = firestore,
            onSuccess = { montanteAtual, ultimoAbastecimento ->
                progressDialog.dismiss()
                handleVerification(montanteAtual.toInt(), ultimoAbastecimento)
            },
            onFailure = { exception ->
                Log.w(
                    "ConflitoActivity",
                    "Falha ao ler bombas/diesel_patio, usando fallback.",
                    exception
                )
                runFallbackQuery()
            }
        )
    }

}