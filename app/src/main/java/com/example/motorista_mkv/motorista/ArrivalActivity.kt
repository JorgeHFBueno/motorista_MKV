package com.example.motorista_mkv.motorista

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.motorista_mkv.MainActivity
import com.example.motorista_mkv.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ArrivalActivity : AppCompatActivity() { //

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var plateAutoComplete: AutoCompleteTextView
    private lateinit var kmEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private var previousKm: Int = 0 // Variável para armazenar a quilometragem anterior
    private var currentKm: Int = 0
    private val PREFS_NAME = "LoginPrefs"
    private val platesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arrival)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        plateAutoComplete = findViewById(R.id.plateAutoCompleteTextView)
        plateAutoComplete.imeOptions = EditorInfo.IME_ACTION_NEXT
        plateAutoComplete.setSingleLine(true)

        kmEditText = findViewById(R.id.kmArrival)
        saveButton = findViewById(R.id.saveArrivalButton)
        backButton = findViewById(R.id.backArrivalButton)

        saveButton.isEnabled = false // Inicializar o botão como desabilitado
        saveButton.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )
        //botão de Voltar
        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 2500) // Reativa após X segundo
            finish() // Finaliza a Activity atual para não deixá-la na pilha
        }

        //setupFieldValidation()
        setupEditTextsAndWatchers()
        fetchPlates()

        plateAutoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlate = parent.getItemAtPosition(position).toString()
            plateAutoComplete.error = null
            fetchLatestKmForPlate(selectedPlate)
            validateFields()
        }
        plateAutoComplete.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(plateAutoComplete.windowToken, 0)
                plateAutoComplete.clearFocus()
                true
            } else {
                false
            }
        }

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2"){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        saveButton.setOnClickListener {
            disableAllButtons()

            val plate = plateAutoComplete.text.toString()
            val km = currentKm
            val currentUser = auth.currentUser
            val currentDate = Date()
            val finalEmail = currentUser?.uid // Agora finalEmail recebe o localId diretamente

            if (isConnected()) {
                // Se estiver online: salva direto no Firestore
                saveArrivalData(plate, km, finalEmail, currentDate)
            } else {
                // Offline: salva localmente e mostra overlay de sucesso
                saveArrivalDataOffline(plate, km, finalEmail, currentDate)
                showSuccessOverlay()
            }
            saveButton.postDelayed({
                enableAllButtons()
            }, 5000) // Reativa após 1 segundo
        }

        kmEditText.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 1) Perder o foco
                kmEditText.clearFocus()

                // 2) Opcional: esconder teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(kmEditText.windowToken, 0)

                true // Indica que tratamos a ação
            } else {
                false // Não tratamos outras ações
            }
        }
    }

    private fun setupEditTextsAndWatchers() {
        // KM EDITTEXT: Ao perder foco, formata com pontuação. Ao ganhar foco, mostra valor cru.
        kmEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = kmEditText.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                kmEditText.tag = rawValue

                val formattedKm = formatKmValue(rawValue)
                kmEditText.setText(formattedKm)
            } else {
                val rawValue = (kmEditText.tag as? Int)?.toString() ?: ""
                kmEditText.setText(rawValue)
            }
            kmEditText.setSelection(kmEditText.text.length)
            validateFields()
        }

        kmEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (kmEditText.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    currentKm = input.toIntOrNull() ?: 0
                    kmEditText.tag = currentKm
                }
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun formatKmValue(rawValue: Int): String {
        // Se queremos a última casa como decimal, dividimos por 10.0
        val decimalValue = rawValue / 10.0

        // Usa DecimalFormat em Locale PT-BR com 1 casa decimal
        val numberFormat = NumberFormat.getNumberInstance(Locale("pt", "BR")) as DecimalFormat
        numberFormat.applyPattern("#,###.0")  // 1 casa decimal

        return numberFormat.format(decimalValue)
    }

    private fun fetchPlates() {
        firestore.collection("01-placas")
            .orderBy("placa", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                platesList.clear()
                for (document in result) {
                    val plateNumber = document.getString("placa")
                    if (plateNumber != null) {
                        platesList.add(plateNumber)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, platesList)
                plateAutoComplete.setAdapter(adapter)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro recuperando placas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchLatestKmForPlate(plate: String) {
        firestore.collection("01-placas")
            .whereEqualTo("placa", plate)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.documents[0]
                    val kmValue = document.getLong("km")
                    if (kmValue != null) {
                        val km = kmValue.toInt()
                        previousKm = km
                        kmEditText.tag = km
                        kmEditText.setText(formatKmValue(previousKm))
                        Log.d("Firestore", "Quilometragem encontrada para $plate: $km")
                    } else {
                        Toast.makeText(this, "KM não encontrado para esta placa; insira manualmente.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Nenhum documento encontrado para esta placa.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar placa: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Erro ao buscar quilometragem: ${exception.message}")
            }
    }


    private fun validateFields() {
        val kmValue = (kmEditText.tag as? Int) ?: 0
        val isKmValid = kmValue > previousKm && kmValue > 0
        if (!isKmValid) {
            kmEditText.error = "A quilometragem deve ser maior que ${formatKmValue(previousKm)}"
        } else {
            kmEditText.error = null
        }

         // Verifica se a placa foi selecionada
        val plateInput = plateAutoComplete.text.toString()
        val isPlateValid = plateInput.isNotEmpty() && platesList.contains(plateInput)
        if (!isPlateValid) {
            plateAutoComplete.error = "Placa inválida ou não existente no BD."
        } else {
            plateAutoComplete.error = null
        }

        val allFieldsValid = isKmValid && isPlateValid

        // Habilita/Desabilita o botão e altera a cor de fundo
        saveButton.isEnabled = allFieldsValid
        val color = if (allFieldsValid) {
            ContextCompat.getColor(this, R.color.azul_escuro)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        saveButton.setBackgroundColor(color)
    }

    private fun isConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun saveArrivalDataOffline(plate: String, km: Int, finalEmail: String?, currentDate: Date) {
        val sharedPrefs = getSharedPreferences("OfflineData", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Monta um objeto JSON com os dados
        val arrivalDataJson = JSONObject().apply {
            put("data", currentDate.time) // salvamos o timestamp (long)
            put("destino", "Palmeira das Missões - RS")
            put("km", km)
            put("motivo", "Volta a Fábrica")
            put("motorista", finalEmail ?: "")
            put("placa", plate)
            put("tipo", "chegada")
        }

        // Recupera a lista atual de atividades pendentes
        val existingData = sharedPrefs.getString("offlineArrivals", "[]")
        val jsonArray = JSONArray(existingData)

        // Adiciona o novo objeto ao array
        jsonArray.put(arrivalDataJson)

        // Salva de volta no SharedPreferences
        editor.putString("offlineArrivals", jsonArray.toString())
        editor.apply()

        Log.d("OfflineSave", "Atividade salva localmente (chegada): $arrivalDataJson")
    }

    private fun saveArrivalData(plate: String, km: Int, finalEmail: String?, currentDate: Date) {
        val arrivalData = hashMapOf(
            "data" to currentDate,
            "destino" to "Palmeira das Missões - RS",
            "km" to km,
            "motivo" to "Volta a Fábrica",
            "motorista" to finalEmail,
            "placa" to plate,
            "tipo" to "chegada"
        )

        val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm", Locale.getDefault())
        val dateStr = dateFormat.format(currentDate)
        val docId = "chegada - $dateStr - $plate"

        firestore.collection("atividades")
            .document(docId)
            .set(arrivalData)
            .addOnSuccessListener {
                // ✅ Atualiza o campo "km" da placa
                firestore.collection("01-placas")
                    .whereEqualTo("placa", plate)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        for (doc in snapshot.documents) {
                            firestore.collection("01-placas")
                                .document(doc.id)
                                .update("km", km)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "KM atualizado para $plate: $km")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Erro ao atualizar KM para $plate", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Erro ao buscar documento para atualizar KM: $plate", e)
                    }

                Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                showSuccessOverlay()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao salvar dados; Tente Novamente!", Toast.LENGTH_SHORT).show()
            }
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
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000) // 2000ms = 2 segundos
    }

    private fun disableAllButtons() {
        backButton.isEnabled = false
        saveButton.isEnabled = false
    }
    private fun enableAllButtons() {
        backButton.isEnabled = true
        saveButton.isEnabled = true
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