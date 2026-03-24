package com.example.motorista_mkv

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.Manifest
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.Toast
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.motorista_mkv.motorista.ArrivalActivity
import com.example.motorista_mkv.motorista.DepartureActivity
import com.example.motorista_mkv.motorista.HistoricoActivity
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.firestore.FieldValue
import com.example.motorista_mkv.versioning.AppVersionGatekeeper

class MainActivity : AppCompatActivity() {
    private companion object {
        const val COL_VEICULOS = "veiculos"
        const val FIELD_QUILOMETRAGEM_ULTIMA = "quilometragemUltima"
        const val FIELD_DATA_ATUALIZACAO = "dataUltimaAtualizacao"
    }

    private lateinit var departureButton: Button
    private lateinit var arrivalButton: Button
    private lateinit var admButton: Button
    private lateinit var fuelButton: Button
    private lateinit var logoutButton: Button
    private lateinit var histButton: Button
    private lateinit var feedbackButton: Button
    private lateinit var welcomeTextView: TextView
    private lateinit var versionTextView: TextView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private val PREFS_NAME = "LoginPrefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FirebaseApp.initializeApp(this)

        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        progressBar = findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        if (isConnected()) {
            sendOfflineDataToFirestore()
        }

        // Inicializar os botões após setContentView
        departureButton = findViewById(R.id.departureButton)
        arrivalButton = findViewById(R.id.arrivalButton)
        admButton = findViewById(R.id.admButton)
        welcomeTextView = findViewById(R.id.welcomeTextView)
        versionTextView = findViewById(R.id.versionTextView)
        fuelButton = findViewById(R.id.fuelButton)
        logoutButton = findViewById(R.id.logoutButton)
        histButton = findViewById(R.id.histButton)
        feedbackButton = findViewById(R.id.feedbackButton)
        disableAllButtons()

        checkVersionAndEnforceAccess()

        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")
        Log.d("deBUGonCreateFunc", "Admin Status: $adminStatus") // Log para depuração

        if (adminStatus == "adm2"){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            Log.d("deBUGifFunc", "Admin Status: $adminStatus") // Log para depuração
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            Log.d("deBUGifFunc", "Admin Status: $adminStatus") // Log para depuração
        }

        //Configurar estado inicial do botão fuel e adm
        admButton.isEnabled = false
        admButton.alpha = 0f
        admButton.visibility = View.GONE

        fuelButton.isEnabled = true
        fuelButton.alpha = 1f
        fuelButton.visibility = View.VISIBLE

        // Verificar autorização para o botão de combustível
        checkFuelAuthorization()

        departureButton.setOnClickListener {
            disableAllButtons()
            Toast.makeText(this, "Saida selecionada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, DepartureActivity::class.java))
            // Reativa o botão após a ação ser concluída (se necessário)
            departureButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }

        arrivalButton.setOnClickListener {
            disableAllButtons()
            Toast.makeText(this, "Chegada selecionada", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ArrivalActivity::class.java)
            startActivity(intent)
            arrivalButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }

        fuelButton.setOnClickListener {
            disableAllButtons()
            //checkRecentChamadoAndShowPopup()
            fuelButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }
        /*BACKUP
        feedbackButton.setOnClickListener {
            disableAllButtons()
            showFeedPopup()
            feedbackButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }*/

        feedbackButton.setOnClickListener {
            disableAllButtons()
            Toast.makeText(this, "DICK ASS", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AssActivity::class.java)
            startActivity(intent)
            feedbackButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }

        admButton.setOnClickListener {
            disableAllButtons()
            Toast.makeText(this, "Administração Selecionado", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AdmActivity::class.java)
            startActivity(intent)
            admButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }

        histButton.setOnClickListener {
            disableAllButtons()
            Toast.makeText(this, "Histórico selecionado", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, HistoricoActivity::class.java))
            // Reativa o botão após a ação ser concluída (se necessário)
            histButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
        }

        logoutButton.setOnClickListener {
            disableAllButtons()
            // Limpar preferências de "Manter conectado"
            val sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("isLoggedIn", false)
            editor.apply()
            // Fazer logout no Firebase
            FirebaseAuth.getInstance().signOut()
            // Redirecionar para a tela de login
            startActivity(Intent(this, LoginActivity::class.java))
            logoutButton.postDelayed({
                enableAllButtons()
            }, 3000) // Reativa após 1 segundo
            finish()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1001
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")
        Log.d("DEBUGonResumeFunc", "Admin Status: $adminStatus") // Log para depuração

        // Revalida bloqueio de versão ao retornar para a MainActivity
        checkVersionAndEnforceAccess()
        // Verificar novamente o status do admin ao retornar para a MainActivity
        checkFuelAuthorization()
        if (isConnected()) {
            sendOfflineDataToFirestore()
        }
    }

    private fun checkVersionAndEnforceAccess() {
        AppVersionGatekeeper.fetchVersionGateConfig(firestore, this) { config ->
            val localVersionCode = AppVersionGatekeeper.getInstalledVersionCode(this)
            val localVersionName = AppVersionGatekeeper.getInstalledVersionName(this)

            Log.i(
                "VersionGate",
                "Main check | localVersionCode=$localVersionCode | localVersionName=$localVersionName | minVersionCode=${config.minVersionCode}"
            )

            if (localVersionCode < config.minVersionCode) {
                Log.w("VersionGate", "Versão bloqueada. Encerrando fluxo da MainActivity.")
                val blockedIntent = Intent(this, AppBlockedActivity::class.java).apply {
                    putExtra(AppBlockedActivity.EXTRA_BLOCK_MESSAGE, config.blockedMessage)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(blockedIntent)
                finish()
                return@fetchVersionGateConfig
            }
            progressBar.visibility = View.GONE
            versionTextView.text = "Versão $localVersionName (${localVersionCode})"
            versionTextView.visibility = View.VISIBLE
            enableAllButtons()
            checkFuelAuthorization()
        }
    }

    private fun checkFuelAuthorization() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2") {
            // Inscreve no tópico "admins" se for ou adm2
            FirebaseMessaging.getInstance().subscribeToTopic("admins")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("FCM", "Inscrito no tópico 'admins'")
                    } else {
                        Log.e("FCM", "Falha ao inscrever no tópico: ${task.exception?.message}")
                    }
                }

        }

        if (adminStatus == "adm1" || adminStatus == "adm2") {
           // Usuário autorizado
            admButton.isEnabled = true
            admButton.alpha = 1f // Botão habilitado (opacidade total)
            admButton.visibility = View.VISIBLE
            fuelButton.isEnabled = false
            fuelButton.alpha = 0f
            fuelButton.visibility = View.GONE
        } else {
            // Usuário não autorizado: Habilita fuelButton, desabilita admButton
            admButton.isEnabled = false
            admButton.alpha = 0f
            admButton.visibility = View.GONE

            fuelButton.isEnabled = true
            fuelButton.alpha = 1f
            fuelButton.visibility = View.VISIBLE
        }
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun sendOfflineDataToFirestore() {
        val sharedPrefs = getSharedPreferences("OfflineData", Context.MODE_PRIVATE)
        val dataString = sharedPrefs.getString("offlineArrivals", "[]")

        val originalJsonArray = JSONArray(dataString)
        if (originalJsonArray.length() == 0) return // sem pendências

        val failedItems = JSONArray()

        for (i in 0 until originalJsonArray.length()) {
            val item = originalJsonArray.getJSONObject(i)

            val dataMillis = item.getLong("data")
            val destino = item.getString("destino")
            val km = item.getInt("km")
            val motivo = item.getString("motivo")
            val motorista = item.optString("motorista", "")
            val placa = item.getString("placa")
            val tipo = item.getString("tipo")

            val currentDate = Date(dataMillis)

            val arrivalData = hashMapOf(
                "data" to currentDate,
                "destino" to destino,
                "km" to km,
                "motivo" to motivo,
                "motorista" to motorista,
                "placa" to placa,
                "tipo" to tipo
            )

            val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm", Locale.getDefault())
            val dateStr = dateFormat.format(currentDate)
            val docId = "$tipo - $dateStr - $placa"

            firestore.collection("atividades")
                .document(docId)
                .set(arrivalData)
                .addOnSuccessListener {
                    Log.d("OfflineSync", "Sincronizado com sucesso: $docId")

                    firestore.collection(COL_VEICULOS)
                        .document(placa)
                        .update(
                            FIELD_QUILOMETRAGEM_ULTIMA,
                            km,
                            FIELD_DATA_ATUALIZACAO,
                            FieldValue.serverTimestamp()
                        )
                        .addOnSuccessListener {
                            Log.d("OfflineSync", "Quilometragem atualizada em $placa: $km")
                        }
                        .addOnFailureListener { e ->
                            Log.e("OfflineSync", "Erro ao atualizar quilometragem em $placa", e)                        }
                }
                .addOnFailureListener { e ->
                    Log.e("OfflineSync", "Erro ao sincronizar $docId", e)
                    failedItems.put(item)
                }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val editor = sharedPrefs.edit()
            editor.putString("offlineArrivals", failedItems.toString())
            editor.apply()
            Log.d("OfflineSync", "SharedPreferences atualizado com os itens que falharam.")
        }, 3000)
    }

    private fun disableAllButtons() {
        departureButton.isEnabled = false
        arrivalButton.isEnabled = false
        fuelButton.isEnabled = false
        admButton.isEnabled = false
        logoutButton.isEnabled = false
        histButton.isEnabled = false
        feedbackButton.isEnabled = false
    }

    private fun enableAllButtons() {
        departureButton.isEnabled = true
        arrivalButton.isEnabled = true
        fuelButton.isEnabled = true
        logoutButton.isEnabled = true
        admButton.isEnabled = true
        histButton.isEnabled = true
        feedbackButton.isEnabled = true
    }

    private fun showFeedPopup() {
        // Construtor do AlertDialog
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater

        // Infla o layout personalizado
        val dialogView = inflater.inflate(R.layout.feedback_dialog, null)
        builder.setView(dialogView)

        // Obtém referências aos views do layout
        val feedbackEditText = dialogView.findViewById<EditText>(R.id.feedbackEditText)
        val sendButton = dialogView.findViewById<Button>(R.id.sendButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

        // Cria o AlertDialog
        val alertDialog = builder.create()

        // Ação do botão "Enviar"
        sendButton.setOnClickListener {
            val feedbackText = feedbackEditText.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                // Pegando o email do usuário atual (caso queira registrar junto ao feedback)
                val currentUser = auth.currentUser
                val finalEmail = currentUser?.uid
                val currentDate = Date()

                // Montando o mapa conforme solicitado
                val novoChamado = hashMapOf(
                    "data" to currentDate,
                    "status" to "FeedBack",
                    "mensagem" to feedbackText,
                    "titulo" to "FeedBack!",
                    "motorista" to finalEmail
                )

                val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm-ss", Locale.getDefault())
                val dateStr = dateFormat.format(currentDate)
                // Monta o ID do documento (ex.: "2023-09-18_12-30-45 usuarioUid")
                val docId = "Feedback $dateStr $finalEmail"
                // Salvando no Firestore (exemplo na coleção "chamados")
                firestore.collection("00-chamados")
                    .document(docId)
                    .set(novoChamado)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Feedback enviado com sucesso!", Toast.LENGTH_SHORT).show()
                        showSuccessOverlay()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Falha ao enviar feedback: ${e.message}", Toast.LENGTH_SHORT).show()
                    }

                alertDialog.dismiss()
            } else {
                Toast.makeText(this, "Por favor, insira seu feedback.", Toast.LENGTH_SHORT).show()
            }
        }

        // Ação do botão "Cancelar"
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        // Exibe o dialog
        alertDialog.show()
    }

    private fun showSuccessOverlay() {
        val overlay = findViewById<FrameLayout>(R.id.successOverlay)
        val gifView = findViewById<ImageView>(R.id.successGif)

        // Usar Glide para carregar o GIF animado
        Glide.with(this)
            .asGif()
            .load(R.drawable.thumbs_up) // Substitua por seu arquivo GIF
            .into(gifView)

        // Mostra o GIF
        overlay.visibility = View.VISIBLE

        // Aguarda 2 segundos e volta para a tela inicial
        overlay.postDelayed({
            overlay.visibility = View.GONE
            /*val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()*/
        }, 3000) // 2000ms = 2 segundos
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