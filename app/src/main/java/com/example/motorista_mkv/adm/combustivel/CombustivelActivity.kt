package com.example.motorista_mkv.adm.combustivel

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.motorista_mkv.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CombustivelActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var recyclerView: RecyclerView
    private lateinit var bombAdapter: BombAdapter
    private lateinit var backButton: Button
    private lateinit var compButton: Button
    private val bombList = mutableListOf<bombModel>()
    private lateinit var tvDiesel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_combustivel)

        firestore = FirebaseFirestore.getInstance()
        recyclerView = findViewById(R.id.fuelRecyclerView)
        backButton = findViewById(R.id.backButton)
        compButton = findViewById(R.id.addButton)
        tvDiesel = findViewById(R.id.tvDiesel)

        recyclerView.layoutManager = LinearLayoutManager(this)

        fetchCombustivelData()

        compButton.setOnClickListener {
            disableAllButtons()
            val intent = Intent(this, FuelActivity::class.java)
            intent.putExtra("debug", true)
            startActivity(intent)
            compButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
        }

        // Configura botão de voltar
        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
            finish()
        }
    }

    private fun fetchCombustivelData() {
        val sharedPreferences = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user") ?: "user"

        Log.d("DEBUG_ADMIN", "AdminStatus: $adminStatus")

        // Exemplo de query com ordenação pela data desc
        val query = firestore.collection("03-combustivel")
            .orderBy("data", Query.Direction.DESCENDING)

        // Executa a query
        query.get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val latestDoc = result.documents[0]
                    val dieselValue = latestDoc.getLong("diesel") ?: 0L

                    tvDiesel.text = formatDiesel(dieselValue)
                } else {
                    tvDiesel.text = "Nenhum documento encontrado"
                }

                bombList.clear()
                    for (document in result) {
                        val bomba = document.toObject(bombModel::class.java)
                        bomba.id = document.id // salva o id do documento
                        bombList.add(bomba)
                    }

                    // Instancia o adapter passando a lista e a ação de clique
                    bombAdapter = BombAdapter(bombList) { bomba ->
                        // Mostra popup ao clicar no item
                        showPopup(bomba)
                    }
                    recyclerView.adapter = bombAdapter

            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar dados: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun formatDiesel(value: Long): String {
        val strValue = value.toString()
        // Se tiver mais de 1 dígito, insere um ponto antes do último
        return if (strValue.length > 1) {
            val beforeDot = strValue.substring(0, strValue.length - 1)
            val afterDot = strValue.substring(strValue.length - 1)
            "$beforeDot.$afterDot"
        } else {
            // Se tiver só 1 dígito, formata como "0.X"
            "0.$strValue"
        }
    }
    private fun showPopup(bomba: bombModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_combus_details, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val dataTextView: TextView = dialogView.findViewById(R.id.popupDataTextView)
        val kmTextView: TextView   = dialogView.findViewById(R.id.popupKMTextView)
        val lfTextView: TextView   = dialogView.findViewById(R.id.popupLFTextView)
        val liTextView: TextView   = dialogView.findViewById(R.id.popupLITextView)
        val ARLATextView: TextView   = dialogView.findViewById(R.id.popupARLATextView)
        val motivoTextView: TextView = dialogView.findViewById(R.id.popupMotivoTextView)
        val paraQuemTextView: TextView = dialogView.findViewById(R.id.popupParaQuemTextView)
        val placaTextView: TextView = dialogView.findViewById(R.id.popupPlacaTextView)
        val qaTextView: TextView   = dialogView.findViewById(R.id.popupQaTextView)
        val motoristaTextView: TextView = dialogView.findViewById(R.id.popupMotoristaTextView)
        val obsTextView: TextView       = dialogView.findViewById(R.id.popupObsTextView)

        val btnFechar = dialogView.findViewById<Button>(R.id.backButton)
        val btnEditar = dialogView.findViewById<Button>(R.id.editButton)
        val btnDelete = dialogView.findViewById<Button>(R.id.delButton)


        // Formatação de data
        bomba.data?.let {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dataTextView.text = "Data: ${dateFormat.format(it.toDate())}"
        } ?: run {
            dataTextView.text = "Data: --/--"
        }

        kmTextView.text = "${bomba.km ?: bomba.semKm}"
        lfTextView.text = formatWithComma(bomba.lf)
        liTextView.text = formatWithComma(bomba.li)
        ARLATextView.text = formatWithComma(bomba.arla)
        motivoTextView.text = "${bomba.motivo ?: "--"}"
        paraQuemTextView.text = "${bomba.para_quem ?: "--"}"
        placaTextView.text = "${bomba.placa ?: "--"}"
        qaTextView.text = formatWithComma(bomba.qa)
        motoristaTextView.text = "${bomba.motorista ?: "--"}"
        obsTextView.text = "${bomba.observacao ?: "--"}"

        btnFechar.setOnClickListener {
            dialog.dismiss()
        }

        btnEditar.setOnClickListener {
            openEditScreen(bomba)
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmação")
                .setMessage("Tem certeza que deseja excluir este documento?")
                .setPositiveButton("Excluir") { confirmDialog, _ ->
                    performBackupAndDelete(bomba, dialog)
                    confirmDialog.dismiss()
                }
                .setNegativeButton("Cancelar") { confirmDialog, _ ->
                    confirmDialog.dismiss()
                }
                .show()
        }

        dialog.show()

        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        val window = dialog.window
        val newWidth = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            (screenWidth * 0.75).toInt() // 75% da largura da tela no modo horizontal
        } else {
            (screenWidth * 0.85).toInt() // 85% da largura da tela no modo vertical
        }

        window?.setLayout(newWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun openEditScreen(bomba: bombModel) {
        //val intent = Intent(this, EditCombustivelActivity::class.java)
        val intent = Intent(this, FuelActivity::class.java)
        intent.putExtra("documentId", bomba.id)

        // Converter Timestamp -> Long para poder enviar por intent
        val dataMillis = bomba.data?.toDate()?.time ?: -1L
        intent.putExtra("data", dataMillis)
        intent.putExtra("km", bomba.km ?: 0L)
        intent.putExtra("lf", bomba.lf ?: 0L)
        intent.putExtra("li", bomba.li ?: 0L)
        intent.putExtra("motivo", bomba.motivo ?: "")
        intent.putExtra("para_quem", bomba.para_quem ?: "")
        intent.putExtra("placa", bomba.placa ?: "")
        intent.putExtra("qa", bomba.qa ?: 0L)
        intent.putExtra("motorista", bomba.motorista ?: "")
        intent.putExtra("observacao", bomba.observacao ?: "")
        intent.putExtra("arla", bomba.arla ?: 0L)
        intent.putExtra("diesel", bomba.diesel ?: 0L)
        intent.putExtra("semKm", bomba.semKm ?: "")
        intent.putExtra("local", bomba.local ?: "")
        intent.putExtra("tipoPlaca", bomba.tipoPlaca)

        intent.putExtra("edicao", true)

        startActivity(intent)
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

    private fun performBackupAndDelete(bomba: bombModel, parentDialog: AlertDialog) {
        // Formata as datas: a data original do documento e a data de exclusão (atual)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())
        val docDateStr = bomba.data?.let { dateFormat.format(it.toDate()) } ?: "noDate"
        val deletionDateStr = dateFormat.format(Date())
        val backupDocId = "$docDateStr EX_COMBUSTIVEL $deletionDateStr"

        // Cria um mapa com os dados do documento (incluindo os novos campos "semKm" e "tipoPlaca")
        val backupData = hashMapOf<String, Any?>(
            "id" to bomba.id,
            "data" to bomba.data,
            "km" to bomba.km,
            "lf" to bomba.lf,
            "li" to bomba.li,
            "motivo" to bomba.motivo,
            "para_quem" to bomba.para_quem,
            "placa" to bomba.placa,
            "qa" to bomba.qa,
            "motorista" to bomba.motorista,
            "observacao" to bomba.observacao,
            "arla" to bomba.arla,
            "semKm" to bomba.semKm,
            "tipoPlaca" to bomba.tipoPlaca
        )

        // Primeiro, faz o backup para a coleção "04-backup"
        firestore.collection("04-backup").document(backupDocId)
            .set(backupData)
            .addOnSuccessListener {
                // Se o backup for bem-sucedido, procede com a exclusão
                if (bomba.id != null) {
                    firestore.collection("03-combustivel").document(bomba.id!!)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Documento excluído com sucesso!", Toast.LENGTH_SHORT).show()
                            onResume()
                            parentDialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Erro ao excluir: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Documento sem ID, não pode ser excluído.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao fazer backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onResume() {
        super.onResume()
        // Limpa a lista (para não duplicar os itens)
        bombList.clear()
        // Busca novamente os dados
        fetchCombustivelData()
    }

    private fun disableAllButtons() {
        backButton.isEnabled = false
        compButton.isEnabled = false
    }
    private fun enableAllButtons() {
        backButton.isEnabled = true
        compButton.isEnabled = true
    }
}