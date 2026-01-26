package com.example.motorista_mkv.adm.notificacoes

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.motorista_mkv.AdmActivity
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.motorista_mkv.R
import com.example.motorista_mkv.adm.combustivel.FuelActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date
import com.example.motorista_mkv.data.BombasRepository

class ConflitoMontanteActivity : AppCompatActivity() {

    private lateinit var edtData: TextView
    private lateinit var edtMotorista: TextView
    private lateinit var edtTipo: TextView
    private lateinit var edtMntntInfrmd: TextView
    private lateinit var edtMontanteInicial: TextView
    private lateinit var edtDfrncl: TextView

    private lateinit var scrollViewCorpo: ScrollView
    private lateinit var scrollViewBtn: ScrollView
    private lateinit var scrollViewCorpoComp: ScrollView
    private lateinit var msgConc: TextView

    private lateinit var backButton: Button
    private lateinit var LiberarButton: Button
    private lateinit var NegarButton: Button
    private lateinit var editButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)//enableEdgeToEdge()
        setContentView(R.layout.activity_conflito_montante)

        // Inicializa os EditTexts
        edtData = findViewById(R.id.dataTXT)
        edtMotorista = findViewById(R.id.motoristaTXT)
        edtTipo = findViewById(R.id.edtTipo)
        edtMntntInfrmd = findViewById(R.id.edtMntntInfrmd)
        edtMontanteInicial = findViewById(R.id.edtMontanteInicial)
        edtDfrncl = findViewById(R.id.edtDfrncl)

        scrollViewCorpo = findViewById(R.id.scrollViewCorpo)
        scrollViewBtn = findViewById(R.id.scrollViewBtn)
        scrollViewCorpoComp = findViewById(R.id.scrollViewCorpoComp)
        msgConc = findViewById(R.id.msgConc)

        backButton = findViewById(R.id.backButton)
        backButton.visibility = View.INVISIBLE
        scrollViewCorpoComp.visibility = View.GONE // Começa invisível

        LiberarButton = findViewById(R.id.LiberarButton)
        NegarButton = findViewById(R.id.NegarButton)
        editButton = findViewById(R.id.editButton)

        val chamadoId = intent.getStringExtra("CHAMADO_ID") ?: run {
            finish() // Finaliza a Activity caso o ID não seja passado
            return
        }

        // Busca os dados no Firestore
        buscarChamado(chamadoId)

        backButton.setOnClickListener {
            disableAllButtons()
            val intent = Intent(this, AdmActivity::class.java)
            startActivity(intent)
            backButton.postDelayed({
                    enableAllButtons()
            }, 1500)
        }
        LiberarButton.setOnClickListener {
            disableAllButtons()
            liberarChamado(chamadoId)
        }
        NegarButton.setOnClickListener {
            disableAllButtons()
            negarChamado(chamadoId)
        }
        editButton.setOnClickListener {
            disableAllButtons()

            val db = FirebaseFirestore.getInstance()
            val chamadoRef = db.collection("00-chamados").document(chamadoId)

            chamadoRef.update("status", "ADICIONAR")
                .addOnSuccessListener {
                    // Busca o valor de dfrncl antes de iniciar a Activity
                    chamadoRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            val dfrncl = document.getLong("dfrncl")?.toInt() ?: 0

                            val intent = Intent(this, FuelActivity::class.java)
                            intent.putExtra("DFRNCL_VALUE", dfrncl) // Passa o valor como extra
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Erro ao buscar chamado", Toast.LENGTH_SHORT).show()
                        }
                        enableAllButtons()
                    }.addOnFailureListener {
                        Toast.makeText(this, "Erro ao obter dados do chamado", Toast.LENGTH_SHORT).show()
                        enableAllButtons()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao atualizar status para ADICIONAR", Toast.LENGTH_SHORT).show()
                    enableAllButtons()
                }
        }

    }

    private fun buscarChamado(chamadoId: String) {
        val db = FirebaseFirestore.getInstance()
        val chamadoRef = db.collection("00-chamados").document(chamadoId)

        chamadoRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val status = document.getString("status") ?: "DESCONHECIDO"
                val dataTimestamp = document.getTimestamp("data")
                val motorista = document.getString("motorista") ?: "N/A"
                val tipo = document.getString("tipo") ?: "N/A"

                // Formata o timestamp para uma string legível
                val dataFormatada = if (dataTimestamp != null) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    sdf.format(dataTimestamp.toDate())
                } else {
                    "N/A"
                }
                edtData.text = dataFormatada
                edtMotorista.text = motorista
                edtTipo.text = tipo

                // Verifica se o chamado ainda está aberto
                if (status != "ABERTO") {
                    scrollViewCorpo.visibility = View.GONE
                    scrollViewBtn.visibility = View.GONE
                    backButton.visibility = View.VISIBLE // Torna o botão visível
                    scrollViewCorpoComp.visibility = View.VISIBLE
                    msgConc.text = "Chamado Já $status" // Define a mensagem correta
                } else {
                    val mntntInfrmd = document.getLong("mntntInfrmd")?.toInt() ?: 0
                    val montanteInicial = document.getLong("montanteInicial")?.toInt() ?: 0
                    val dfrncl = document.getLong("dfrncl")?.toInt() ?: 0

                    // Atualiza os campos na tela
                    edtMntntInfrmd.text = formatValueWithComma(mntntInfrmd)
                    edtMontanteInicial.text = formatValueWithComma(montanteInicial)
                    edtDfrncl.text = formatValueWithComma(dfrncl)
                }
            } else {
                Log.e("ConflitoMontante", "Chamado não encontrado!")
                finish() // Fecha a Activity se o chamado não existir
            }
        }.addOnFailureListener { e ->
            Log.e("ConflitoMontante", "Erro ao buscar dados: ", e)
            finish() // Fecha a Activity se houver erro
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

    private fun liberarChamado(chamadoId: String) {
        val db = FirebaseFirestore.getInstance()
        val chamadoRef = db.collection("00-chamados").document(chamadoId)

        chamadoRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                // Extrai os dados do chamado
                val dataTimestamp = doc.getTimestamp("data")
                val dataDate = dataTimestamp?.toDate() ?: Date()
                val motorista = doc.getString("motorista") ?: ""
                val montanteInicial = doc.getLong("montanteInicial")?.toInt() ?: 0
                val dfrncl = doc.getLong("dfrncl")?.toInt() ?: 0
                val mntntInfrmd = doc.getLong("mntntInfrmd")?.toInt() ?: 0

                val dataToSave = hashMapOf<String, Any>(
                    "placa" to "",
                    "local" to "",
                    "motivo" to "",
                    "semKm" to "",
                    "arla" to 0,
                    "data" to dataDate,
                    "motorista" to motorista,
                    "li" to montanteInicial,
                    "qa" to dfrncl,
                    "lf" to mntntInfrmd,
                    "observacao" to "Descobrir quem fez esse abastecimento",
                    "para_quem" to "ERRO"
                )

                val ref = db.collection("03-combustivel")
                fun handleDiesel(lastDiesel: Int) {
                    val newDieselValue = lastDiesel + dfrncl
                    dataToSave["diesel"] = newDieselValue

                    val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm-ss", Locale.getDefault())
                    val dateStr = dateFormat.format(Date())
                    val docId = "$dateStr $motorista"

                    ref.document(docId).set(dataToSave)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Registrado na tabela 03-combustivel com sucesso!",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.d("LiberarChamado", "Documento salvo com ID: $docId")
                            // Atualiza o status do chamado para LIBERADO
                            chamadoRef.update("status", "LIBERADO")
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Chamado atualizado para LIBERADO!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    enableAllButtons()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        this,
                                        "Erro ao atualizar status para LIBERADO",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    enableAllButtons()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erro ao salvar dados em 03-combustivel",
                                Toast.LENGTH_SHORT
                            ).show()
                            Log.e("LiberarChamado", "Erro ao salvar: ", e)
                            enableAllButtons()
                        }
                }

                fun runFallbackQuery() {
                    ref.orderBy("data", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val lastDiesel = if (!snapshot.isEmpty) {
                                snapshot.documents[0].getLong("diesel")?.toInt() ?: 0
                            } else {
                                0
                            }
                            handleDiesel(lastDiesel)
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                this,
                                "Erro ao buscar último diesel; Tente novamente!",
                                Toast.LENGTH_SHORT
                            ).show()
                            enableAllButtons()
                        }
                }

                BombasRepository.fetchEstoqueAtual(
                    firestore = db,
                    onSuccess = { estoqueAtual ->
                        handleDiesel(estoqueAtual.toInt())
                    },
                    onFailure = { exception ->
                        Log.w(
                            "ConflitoMontanteActivity",
                            "Falha ao ler bombas/diesel_patio, usando fallback.",
                            exception
                        )
                        runFallbackQuery()
                    }
                )
            } else {
                Toast.makeText(this, "Chamado inexistente.", Toast.LENGTH_SHORT).show()
                enableAllButtons()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Erro ao acessar o chamado.", Toast.LENGTH_SHORT).show()
            enableAllButtons()
        }
    }

    private fun negarChamado(chamadoId: String) {
        val db = FirebaseFirestore.getInstance()
        val chamadoRef = db.collection("00-chamados").document(chamadoId)

        chamadoRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                chamadoRef.update("status", "NEGADO")
                    .addOnSuccessListener {
                        Toast.makeText(this, "Chamado negado com sucesso!", Toast.LENGTH_SHORT).show()
                        Log.d("NegarChamado", "Chamado $chamadoId atualizado para NEGADO.")
                        enableAllButtons()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao negar o chamado!", Toast.LENGTH_SHORT).show()
                        Log.e("NegarChamado", "Erro ao atualizar chamado para NEGADO: ", e)
                        enableAllButtons()
                    }
            } else {
                Log.e("ConflitoMontante", "Chamado não encontrado!")
                enableAllButtons()
                finish()
            }
        }.addOnFailureListener { e ->
            Log.e("ConflitoMontante", "Erro ao buscar dados: ", e)
            enableAllButtons()
            finish()
        }
    }

    private fun disableAllButtons() {
        backButton.isEnabled = false
        LiberarButton.isEnabled = false
        NegarButton.isEnabled = false
        editButton.isEnabled = false
    }
    private fun enableAllButtons() {
        backButton.isEnabled = true
        LiberarButton.isEnabled = true
        NegarButton.isEnabled = true
        editButton.isEnabled = true
        finish()
    }
}