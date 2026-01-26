package com.example.motorista_mkv

import androidx.appcompat.app.AppCompatActivity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.motorista_mkv.adm.combustivel.CombustivelActivity
import com.example.motorista_mkv.adm.combustivel.ConflitoActivity
import com.example.motorista_mkv.adm.notificacoes.DetalheChamadoActivity
import com.example.motorista_mkv.adm.combustivel.FuelActivity
import com.example.motorista_mkv.frota_mkii.FrotaActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Log
import com.example.motorista_mkv.data.BombasRepository

class AdmActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var backButton: Button
    private lateinit var truckButton: Button
    private lateinit var fuelButton: Button
    private lateinit var notiButton: Button
    private lateinit var combsButton: Button

    private val PREFS_NAME = "LoginPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adm)

        firestore = FirebaseFirestore.getInstance()
        backButton = findViewById(R.id.backButton)
        truckButton = findViewById(R.id.truckButton)
        fuelButton = findViewById(R.id.fuelButton)
        notiButton = findViewById(R.id.notiButton)
        combsButton = findViewById(R.id.CombButt)

        notiButton.isEnabled = false

        checkAuthorization()

        notiButton.setOnClickListener {
            disableAllButtons()
            val intent = Intent(this, DetalheChamadoActivity::class.java)
            startActivity(intent)
            notiButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
        }

        truckButton.setOnClickListener {
            disableAllButtons()
            val intent = Intent(this, FrotaActivity::class.java)
            startActivity(intent)
            truckButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
        }

        combsButton.setOnClickListener {
            disableAllButtons()
            val intent = Intent(this, CombustivelActivity::class.java)
            startActivity(intent)
            combsButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
        }

        //val ultimaLitragem = document.getString("lf")?.toIntOrNull() ?: 0
        fuelButton.setOnClickListener {
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser

            if (currentUser == null) {
                Toast.makeText(this, "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calcula a data limite (8 minutos atrás)
            val eightMinutesAgo = Date(System.currentTimeMillis() - 8 * 60 * 1000)

            // Verifica se existe algum chamado do tipo "Conflito no Montante" com status "ABERTO" nos últimos 8 minutos
            firestore.collection("00-chamados")
                .whereEqualTo("tipo", "Conflito no Montante")
                .whereEqualTo("status", "ABERTO")
                .whereGreaterThan("data", eightMinutesAgo)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        // Existe um chamado aberto dentro do período; direciona diretamente para ConflitoActivity
                        val conflictDoc = querySnapshot.documents[0]
                        val documentId = conflictDoc.id

                        val intent = Intent(this, ConflitoActivity::class.java)
                        intent.putExtra("DOCUMENT_ID", documentId)

                        startActivity(intent)
                    } else {
                        // Se não houver conflito recente, segue o fluxo atual: exibe o popup para input
                        // Infla o layout customizado
                        val inflater = LayoutInflater.from(this)
                        val dialogView = inflater.inflate(R.layout.popup_fuel_verification, null)

                        // Cria o AlertDialog com esse layout
                        val dialog = AlertDialog.Builder(this)
                            .setView(dialogView)
                            .create()

                        // Referências aos elementos do layout
                        val inputEditText = dialogView.findViewById<EditText>(R.id.editTextLitragem)
                        val btnVerificar = dialogView.findViewById<Button>(R.id.btnVerificar)
                        val btnCancelar = dialogView.findViewById<Button>(R.id.btnCancelar)

                        // Listener do botão "Verificar"
                        btnVerificar.setOnClickListener {
                            val novaLitragemStr = inputEditText.text.toString().trim()
                            val novaLitragem = novaLitragemStr.toIntOrNull() ?: 0

                            fun handleVerification(ultimaLitragem: Int, folga: Int) {
                                val diferencial = novaLitragem - ultimaLitragem

                                if (kotlin.math.abs(novaLitragem - ultimaLitragem) <= folga) {
                                    val intent = Intent(this, FuelActivity::class.java)
                                    startActivity(intent)
                                } else {
                                    val chamado = hashMapOf(
                                        "data" to Date(),
                                        "motorista" to currentUser.uid,
                                        "status" to "ABERTO",
                                        "tipo" to "Conflito no Montante",
                                        "mntntInfrmd" to novaLitragem,
                                        "montanteInicial" to ultimaLitragem,
                                        "dfrncl" to diferencial
                                    )

                                    val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm", Locale.getDefault())
                                    val dateStr = dateFormat.format(Date())
                                    val tipo = "Conflito no Montante"
                                    val motorista = currentUser.uid
                                    val documentId = "$dateStr $tipo - $motorista"

                                    firestore.collection("00-chamados")
                                        .document(documentId)
                                        .set(chamado)
                                        .addOnSuccessListener {
                                            val intent = Intent(this, ConflitoActivity::class.java)
                                            intent.putExtra("DOCUMENT_ID", documentId)
                                            intent.putExtra("DIFERENCIAL", diferencial)
                                            intent.putExtra("LITRAGEM_NOVA", novaLitragem)
                                            startActivity(intent)
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                        }
                                }
                                dialog.dismiss()
                            }

                            fun runFallbackQuery() {
                                val query = firestore.collection("03-combustivel")
                                    .orderBy("data", Query.Direction.DESCENDING)
                                    .limit(1)

                                query.get()
                                    .addOnSuccessListener { querySnapshot ->
                                        if (!querySnapshot.isEmpty) {
                                            val document = querySnapshot.documents[0]
                                            val ultimaLitragem = document.getLong("lf")?.toInt() ?: 0
                                            handleVerification(ultimaLitragem, 15)
                                        } else {
                                            Toast.makeText(this, "Nenhum registro encontrado!", Toast.LENGTH_SHORT).show()
                                        }

                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Erro ao buscar dados: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            BombasRepository.fetchMontanteFolga(
                                firestore = firestore,
                                onSuccess = { montanteAtual, folgaLitros ->
                                    handleVerification(montanteAtual.toInt(), folgaLitros.toInt())
                                },
                                onFailure = { exception ->
                                    Log.w(
                                        "AdmActivity",
                                        "Falha ao ler bombas/diesel_patio, usando fallback.",
                                        exception
                                    )
                                    runFallbackQuery()
                                }
                            )
                        }
                        // Listener do botão "Cancelar"
                        btnCancelar.setOnClickListener {
                            dialog.dismiss()
                        }
                        // Exibe o AlertDialog
                        dialog.show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao verificar conflitos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        backButton.setOnClickListener { // botão de Voltar
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo
            finish() // Finaliza a Activity atual para não deixá-la na pilha
        }
    }

    private fun checkAuthorization() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2") {
            // Usuário autorizado
            truckButton.isEnabled = true
            truckButton.alpha = 1f // Botão habilitado (opacidade total)
            truckButton.visibility = View.VISIBLE

            combsButton.isEnabled = true
            combsButton.alpha = 1f // Botão habilitado (opacidade total)
            combsButton.visibility = View.VISIBLE

            notiButton.isEnabled = true
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            truckButton.isEnabled = false
            truckButton.alpha = 0f
            truckButton.visibility = View.GONE

            combsButton.isEnabled = false
            combsButton.alpha = 0f // Botão habilitado (opacidade total)
            combsButton.visibility = View.GONE
            notiButton.isEnabled = false
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private fun disableAllButtons() {
        backButton.isEnabled = false
        truckButton.isEnabled = false
        fuelButton.isEnabled = false
        notiButton.isEnabled = false
        combsButton.isEnabled = false
    }
    private fun enableAllButtons() {
        backButton.isEnabled = true
        truckButton.isEnabled = true
        fuelButton.isEnabled = true
        notiButton.isEnabled = true
        combsButton.isEnabled = true
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