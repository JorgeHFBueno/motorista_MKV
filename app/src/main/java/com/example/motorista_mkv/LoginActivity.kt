package com.example.motorista_mkv

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.content.res.Configuration
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import android.util.Log
import android.widget.CheckBox
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loginButton: Button
    private var saidasListener: ListenerRegistration? = null
    private var chegadasListener: ListenerRegistration? = null
    private val PREFS_NAME = "LoginPrefs"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        loginButton = findViewById(R.id.loginButton)

        val nameEditText: EditText = findViewById(R.id.nameEditText)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val stayLoggedInCheckBox: CheckBox = findViewById(R.id.stayLoggedInCheckBox)

        // Ativa o cache offline do Firestore
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true) // Ativa cache offline
            .build()

        // Verificar se já está logado
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loginButton.setOnClickListener {
            disableAllButtons()
            val name = nameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            loginButton.postDelayed({
                enableAllButtons()
            }, 1000) // Reativa após 1 segundo

            if (name.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(name + "@example.com", password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Salvar estado de "Manter conectado"
                            if (stayLoggedInCheckBox.isChecked) {
                                sharedPreferences.edit()
                                    .putBoolean("isLoggedIn", true)
                                    .apply()
                            }
                            // Salva status de Adm
                            val user = auth.currentUser
                            if (user != null) {
                                fetchAdminStatus(user.email.orEmpty()) { adminStatus ->
                                    sharedPreferences.edit()
                                        .putString("adminStatus", adminStatus)
                                        .apply()
                                    startActivity(Intent(this, MainActivity::class.java))
                                    finish()
                                }
                            }else {
                                Toast.makeText(this, "Erro ao obter usuário!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(baseContext, "Falha no Login!", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Preencha os Campos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disableAllButtons() {
        loginButton.isEnabled = false
    }

    private fun enableAllButtons() {
        loginButton.isEnabled = true
    }

    // Verifica status de Adm
    private fun fetchAdminStatus(email: String, callback: (String) -> Unit) {
        firestore.collection("00-autorizados")
            .document(email)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val isAdmin1 = document.getBoolean("adm1") ?: false
                    val isAdmin2 = document.getBoolean("adm2") ?: false
                    val adminStatus = when {
                        isAdmin1 -> "adm1"
                        isAdmin2 -> "adm2"
                        else -> "user"
                    }
                    callback(adminStatus)
                } else {
                    callback("user")
                }
            }
            .addOnFailureListener {
                callback("user")
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        saidasListener?.remove()
        chegadasListener?.remove()
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