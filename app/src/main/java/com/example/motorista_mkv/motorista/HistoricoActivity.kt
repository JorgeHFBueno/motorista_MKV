package com.example.motorista_mkv.motorista

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoricoActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var backButton: Button
    private val historyList = mutableListOf<HistoryModel>()
    private val PREFS_NAME = "LoginPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historico)

        firestore = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.historicoRecyclerView)
        backButton = findViewById(R.id.backButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2"){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Carrega dados
        fetchHistoryData()

        // Botão Voltar
        backButton.setOnClickListener {
            backButton.isEnabled = false
            backButton.postDelayed({
                backButton.isEnabled = true
            }, 1000)
            finish()
        }
    }

    private fun fetchHistoryData() {
        // Pega o usuário atual do Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        val finalEmail = currentUser?.uid

        if (finalEmail == null) {
            Toast.makeText(this, "Email do motorista não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        // Cria as queries para cada coleção
        val queryCombustivel = firestore.collection("03-combustivel")
            .whereEqualTo("motorista", finalEmail)
            .orderBy("data", Query.Direction.DESCENDING)
            .get()

        val queryAtividade = firestore.collection("atividades")
            .whereEqualTo("motorista", finalEmail)
            .orderBy("data", Query.Direction.DESCENDING)
            .get()

        // Executa as queries em paralelo
        Tasks.whenAllSuccess<QuerySnapshot>(queryCombustivel, queryAtividade)
            .addOnSuccessListener { results ->
                historyList.clear()

                results.forEachIndexed { index, result ->
                    if (result is QuerySnapshot) {
                        for (document in result.documents) {
                            val dataTimestamp = document.getTimestamp("data") ?: Timestamp.now()
                            val placa = document.getString("placa") ?: "--"

                            // Define o valor de collectionType de acordo com a coleção
                            val collectionType = when (index) {
                                0 -> "Combustível"  // queryCombustivel
                                1 -> document.getString("tipo") ?: "atividades" // queryAtividade
                                else -> "Desconhecido"
                            }

                            val historyItem = HistoryModel(
                                id = document.id,
                                collectionType = collectionType,
                                data = dataTimestamp,
                                placa = placa
                            )
                            historyList.add(historyItem)
                        }
                    }
                }

                // Ordena a lista final por data (decrescente)
                historyList.sortByDescending { it.data?.toDate() }

                // Configura o adapter
                historyAdapter = HistoryAdapter(historyList) { historyItem ->
                    showDetailPopup(historyItem)
                }
                recyclerView.adapter = historyAdapter
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar histórico: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("HistoricoActivity", "Erro: ", exception)
            }
    }

    private fun showDetailPopup(historyItem: HistoryModel) {
        val docRef = firestore.collection(
            if (historyItem.collectionType == "Combustível") "03-combustivel" else "atividades"
        ).document(historyItem.id)

        docRef.get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.dialog_detalhes, null)

                val contentLayout = dialogView.findViewById<LinearLayout>(R.id.contentLayout)
                contentLayout.removeAllViews()

                val tituloTextView = dialogView.findViewById<TextView>(R.id.dialogTitle)
                tituloTextView.text = "Detalhes do ${historyItem.collectionType}"
                tituloTextView.setTextColor(Color.parseColor("#3F51B5"))
                tituloTextView.setTypeface(null, Typeface.BOLD)
                tituloTextView.textSize = 20f

                val labelColor = Color.parseColor("#3F51B5") // azul suave
                val textSizeSp = 18f

                for ((key, value) in document.data ?: emptyMap()) {
                    val formattedValue = when (value) {
                        is Timestamp -> {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                            dateFormat.format(value.toDate())
                        }
                        else -> value.toString()
                    }

                    val displayText = "$key: $formattedValue"
                    val styledText = SpannableString(displayText).apply {
                        setSpan(StyleSpan(Typeface.BOLD), 0, key.length + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                        setSpan(ForegroundColorSpan(labelColor), 0, key.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }

                    val textView = TextView(this).apply {
                        text = styledText
                        textSize = textSizeSp
                        setLineSpacing(4f, 1.1f)
                        setPadding(0, 4, 0, 4)
                    }

                    contentLayout.addView(textView)
                }

                val btnFechar = dialogView.findViewById<Button>(R.id.btnFechar)
                val btnEditar = dialogView.findViewById<Button>(R.id.btnEditar)
                val btnExcluir = dialogView.findViewById<Button>(R.id.btnExcluir)

                val dialog = builder.setView(dialogView).create()


                btnFechar.setOnClickListener {
                    dialog.dismiss()
                }

                btnEditar.setOnClickListener {
                    mostrarDialogoDeTexto(
                        titulo = "Solicitar Edição",
                        hint = "O que você quer que seja arrumado?",
                        tipoChamado = "Edicao",
                        historyItem = historyItem
                    )
                }

                btnExcluir.setOnClickListener {
                    mostrarDialogoDeTexto(
                        titulo = "Solicitar Exclusão",
                        hint = "Por que este registro deve ser excluído?",
                        tipoChamado = "Exclusao",
                        historyItem = historyItem
                    )
                }
                dialog.show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Erro ao buscar detalhes", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDialogoDeTexto( titulo: String, hint: String, tipoChamado: String, historyItem: HistoryModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_text_input, null)
        val tituloView = dialogView.findViewById<TextView>(R.id.dialogTextTitle)
        val inputView = dialogView.findViewById<EditText>(R.id.dialogTextInput)

        tituloView.text = titulo
        inputView.hint = hint

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Enviar") { dialogInterface, _ ->
                val mensagem = inputView.text.toString().trim()
                if (mensagem.isNotEmpty()) {
                    val prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                    val nomeMotorista = prefs.getString("nome", "Motorista")

                    val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm", Locale.getDefault())
                    val dateStr = dateFormat.format(Date())
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "sem_uid"
                    val documentId = "$dateStr $tipoChamado - $uid"

                    val chamado = hashMapOf(
                        "tipo" to tipoChamado,
                        "colecao" to if (historyItem.collectionType == "Combustível") "03-combustivel" else "atividades",
                        "motivo" to mensagem,
                        "status" to "ABERTO",
                        "motorista" to nomeMotorista,
                        "data" to Date()
                    )

                    firestore.collection("00-chamados")
                        .document(documentId)
                        .set(chamado)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Pedido enviado ao administrador.", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao enviar o pedido.", Toast.LENGTH_SHORT).show()
                        }

                    dialogInterface.dismiss()
                } else {
                    Toast.makeText(this, "Preencha o campo antes de enviar.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega dados quando voltar a esta Activity
        fetchHistoryData()
    }

    override fun attachBaseContext(newBase: Context) {
        // Clona a configuração atual
        val overrideConfig: Configuration = Configuration(newBase.resources.configuration)

        // Define a escala de fonte fixa em 1.0 (sem escalonamento adicional)
        overrideConfig.fontScale = 1.0f

        // Cria um novo contexto com essa configuração
        val context = newBase.createConfigurationContext(overrideConfig)
        super.attachBaseContext(context)
    }
}