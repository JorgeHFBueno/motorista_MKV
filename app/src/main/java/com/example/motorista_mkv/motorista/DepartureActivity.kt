package com.example.motorista_mkv.motorista

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import android.widget.TextView
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.motorista_mkv.MainActivity
import com.example.motorista_mkv.R
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class DepartureActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var plateAutoComplete: AutoCompleteTextView
    private lateinit var kmEditText: EditText
    private lateinit var destinyAutoCompleteTextView: AutoCompleteTextView
    private lateinit var reasonEditText: AutoCompleteTextView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button
    private var previousKm: Int = 0 // Variável para armazenar a quilometragem anterior
    private var currentKm: Int = 0
    private val PREFS_NAME = "LoginPrefs"
    private val platesList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_departure)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        plateAutoComplete = findViewById(R.id.plateAutoCompleteTextView)
        plateAutoComplete.imeOptions = EditorInfo.IME_ACTION_NEXT
        plateAutoComplete.setSingleLine(true)

        kmEditText = findViewById(R.id.kmEditText)
        destinyAutoCompleteTextView = findViewById(R.id.destinyAutoCompleteTextView)
        reasonEditText = findViewById(R.id.reasonEditText)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backDepartureButton)

        // Desativar o botão inicialmente
        saveButton.isEnabled = false
        saveButton.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2"){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Adicionar TextWatcher e Spinner Listener
        setupFieldValidation()

        //botão de Voltar
        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 2500) // Reativa após X segundo
            finish() // Finaliza a Activity atual para não deixá-la na pilha
        }

        // Adicionando TextWatcher para formatar o campo kmEditText e validar o valor
        kmEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().replace("[^0-9]".toRegex(), "")
                val newKm = input.toIntOrNull() ?: 0

                // Validate the input against the previous value
                if (newKm < previousKm) {
                    kmEditText.error = "A quilometragem deve ser maior ou igual a ${
                        NumberFormat.getNumberInstance(Locale("pt", "BR")).format(previousKm)
                    }"
                } else {
                    currentKm = newKm // Update the current value
                    kmEditText.error = null // Clear error if valid
                }
            }
        })

        kmEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // Format the value on losing focus
                val formattedKm =
                    NumberFormat.getNumberInstance(Locale("pt", "BR")).format(currentKm)
                kmEditText.setText(formattedKm)
            } else {
                // Restore the raw value on gaining focus
                val rawKm = currentKm.toString()
                kmEditText.setText(rawKm)
            }

            // Ensure the cursor is at the end of the text
            kmEditText.setSelection(kmEditText.text.length)
        }

        // Fetch plates and set up the spinner
        fetchPlates()
        setupEditTextsAndWatchers()

        setupDestinationAutoComplete()
        setupMotivationAutoComplete()

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

        // 1. Ao pressionar "Next", esconde o teclado e foca no campo de destino
        reasonEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(reasonEditText.windowToken, 0)
                reasonEditText.clearFocus()
                true
            } else {
                false
            }
        }

        // 2. Ao ganhar foco, exibe o dropdown da lista de motivos
        reasonEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                reasonEditText.showDropDown()
            }
        }

        // 3. Adiciona listener para quando o usuário clicar em uma opção do autocomplete
        // Linha alterada: definir o texto selecionado, limpar o foco e chamar validateFields()
        reasonEditText.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            reasonEditText.setText(selected)
            reasonEditText.clearFocus()
            validateFields()
        }


        // (Opcional) Adiciona listener para o AutoComplete de destino, se necessário
        destinyAutoCompleteTextView.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            destinyAutoCompleteTextView.setText(selected)
            destinyAutoCompleteTextView.clearFocus()
            validateFields()
        }

        saveButton.setOnClickListener {
            disableAllButtons()
            val destiny = destinyAutoCompleteTextView.text.toString()
            val plate = plateAutoComplete.text.toString()
            val km = currentKm
            val reason = reasonEditText.text.toString()
            val currentDate = Date()
            val currentUser = auth.currentUser
            val finalEmail = currentUser?.uid // Agora finalEmail recebe o localId diretamente

            if (isConnected()) {
                // Se estiver online: salva direto no Firestore
                saveDepartureData(destiny, plate, km, reason, finalEmail, currentDate)
            } else {
                // Offline: salva localmente e mostra overlay de sucesso
                saveDepartureDataOffline(destiny, plate, km, reason, finalEmail, currentDate)
                showSuccessOverlay()
            }
            saveButton.postDelayed({
                enableAllButtons()
            }, 5000) // Reativa após 1 segundo
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
                val formattedKm =
                    NumberFormat.getNumberInstance(Locale("pt", "BR")).format(rawValue)
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
                        val formattedKm = NumberFormat.getNumberInstance(Locale("pt", "BR")).format(km)
                        kmEditText.setText(formattedKm)
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


    private fun setupDestinationAutoComplete() {
        val destinations = resources.getStringArray(R.array.options_array_destino)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, destinations)
        destinyAutoCompleteTextView.setAdapter(adapter)
    }

    private fun setupMotivationAutoComplete() {
        reasonEditText.threshold = 0
        val reasons = resources.getStringArray(R.array.options_array_motivos)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reasons)
        reasonEditText.setAdapter(adapter)
    }

    private fun setupFieldValidation() { // Adicionando TextWatcher nos campos de texto
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        destinyAutoCompleteTextView.addTextChangedListener(textWatcher)
        kmEditText.addTextChangedListener(textWatcher)
        reasonEditText.addTextChangedListener(textWatcher)
    }

    private fun validateFields() {
        val kmValue = (kmEditText.tag as? Int) ?: 0
        val isKmValid = kmValue >= previousKm && kmValue > 0
        if (!isKmValid) {
            kmEditText.error = "A quilometragem deve ser maior que ${
                NumberFormat.getNumberInstance(Locale("pt", "BR")).format(previousKm)
            }"
        } else {
            kmEditText.error = null
        }

        val isDestinyValid = destinyAutoCompleteTextView.text.toString().length >=3
        if (!isDestinyValid) {
            destinyAutoCompleteTextView.error = "O destino deve ser valido."
        }else {
            destinyAutoCompleteTextView.error = null
        }

        val isReasonValid = reasonEditText.text.toString().length >=3
        if (!isReasonValid) {
            reasonEditText.error = "O motivo deve ser valido."
        }else {
            reasonEditText.error = null
        }
        // Habilitar ou desabilitar o botão com base na validação
        val plateInput = plateAutoComplete.text.toString()
        val isPlateValid = plateInput.isNotEmpty() && platesList.contains(plateInput)
        if (!isPlateValid) {
            plateAutoComplete.error = "Placa inválida ou não existente no BD."
        } else {
            plateAutoComplete.error = null
        }

        val allFieldsValid = isKmValid && isPlateValid && isDestinyValid && isReasonValid

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

    private fun saveDepartureDataOffline(destiny: String, plate: String, km: Int, reason: String?, modifiedEmail: String?, currentDate: Date) {
        val sharedPrefs = getSharedPreferences("OfflineData", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Monta um objeto JSON com os dados
        val arrivalDataJson = JSONObject().apply {
            put("data", currentDate.time) // salvamos o timestamp (long)
            put("destino", destiny ?: "")
            put("km", km)
            put("motivo", reason)
            put("motorista", modifiedEmail ?: "")
            put("placa", plate)
            put("tipo", "saida")
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

    private fun saveDepartureData(destiny: String, plate: String, km: Int, reason: String?, modifiedEmail: String?, currentDate: Date) {
        val departureData = hashMapOf(
            "data" to currentDate,
            "destino" to destiny,
            "km" to km,
            "motivo" to reason,
            "motorista" to modifiedEmail,
            "placa" to plate,
            "tipo" to "saida"
        )

        val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm", Locale.getDefault())
        val dateStr = dateFormat.format(currentDate)
        val docId = "saida - $dateStr - $plate"

        firestore.collection("atividades")
            .document(docId)
            .set(departureData)
            .addOnSuccessListener {
                // Atualiza o campo "km" na coleção "01-placas"
                firestore.collection("01-placas")
                    .whereEqualTo("placa", plate)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        for (document in querySnapshot) {
                            firestore.collection("01-placas")
                                .document(document.id)
                                .update("km", km)
                        }
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