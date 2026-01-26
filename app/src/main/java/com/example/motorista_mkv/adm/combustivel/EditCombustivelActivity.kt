package com.example.motorista_mkv.adm.combustivel

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.motorista_mkv.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.motorista_mkv.data.BombasRepository

class EditCombustivelActivity : AppCompatActivity() {
    private companion object {
        const val COL_VEICULOS = "veiculos"
        const val FIELD_CATEGORIA = "categoria"
        const val FIELD_IDENTIFICADOR = "identificador"
        const val FIELD_ATIVO = "ativo"
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var documentId: String? = null

    // Campo de placa como AutoCompleteTextView
    private lateinit var placaAutoCompleteTextView: AutoCompleteTextView
    private lateinit var kmEditText: EditText
    private lateinit var paraQuemEditText: AutoCompleteTextView
    private lateinit var motivoEditText: AutoCompleteTextView
    private lateinit var localEditText: AutoCompleteTextView

    private lateinit var liEditText: EditText
    private lateinit var qaEditText: EditText
    private lateinit var lfAutoCompleteTextView: AutoCompleteTextView
    private lateinit var EditTextArla: EditText

    private lateinit var motoristaEditText: AutoCompleteTextView
    private lateinit var observacaoEditText: EditText


    private lateinit var dataEditText: EditText // Novo campo para data
    private var selectedDateMillis: Long = -1L // Variável para armazenar a data selecionada (em milissegundos)

    // Checkboxes:
    // - checkBoxExtra: define o tipo de placa (se busca pelo campo "extra" ou pelo campo "placa")
    // - checkBoxkm, checkBoxGalao e checkBoxHNF: definem o tipo de medição de KM.
    private lateinit var checkBoxExtra: CheckBox
    private lateinit var checkBoxkm: CheckBox
    private lateinit var checkBoxGalao: CheckBox
    private lateinit var checkBoxHNF: CheckBox

    private lateinit var saveButton: Button
    private lateinit var backButton: Button

    private var previousKm: Int = 0
    private var currentKm: Int = 0
    // Removemos a variável de última litragem, pois não será utilizada aqui.
    private var isLfManuallyModified: Boolean = false
    private val PREFS_NAME = "LoginPrefs"
    private var docDateStr: String = "noDate"

    // Lista para armazenar as placas recuperadas do BD
    private var platesList: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_combustivel)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Inicialização dos campos (IDs devem estar de acordo com o layout)
        placaAutoCompleteTextView = findViewById(R.id.editPlacaEditText)
        kmEditText = findViewById(R.id.kmEditText)
        paraQuemEditText = findViewById(R.id.paraQuemEditText)
        motivoEditText = findViewById(R.id.motivoEditText)
        localEditText = findViewById(R.id.localEditText)

        liEditText = findViewById(R.id.liEditText)
        qaEditText = findViewById(R.id.qaEditText)
        lfAutoCompleteTextView = findViewById(R.id.lfAutoCompleteTextView)
        EditTextArla = findViewById(R.id.EditTextArla)

        motoristaEditText = findViewById(R.id.motoristaEditText)
        observacaoEditText = findViewById(R.id.observacaoEditText)

        dataEditText = findViewById(R.id.dataEditText)

        checkBoxExtra = findViewById(R.id.checkBoxExtra)
        checkBoxkm = findViewById(R.id.checkBoxkm)
        checkBoxGalao = findViewById(R.id.checkBoxGalao)
        checkBoxHNF = findViewById(R.id.checkBoxHNF)

        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backFuelButton)

        // Recupera os dados enviados via Intent
        documentId = intent.getStringExtra("documentId")
        val kmFromIntent = intent.getLongExtra("km", 0L)
        val lfFromIntent = intent.getLongExtra("lf", 0L)
        val liFromIntent = intent.getLongExtra("li", 0L)
        val qaFromIntent = intent.getLongExtra("qa", 0L)
        val motivo = intent.getStringExtra("motivo") ?: ""
        val paraQuem = intent.getStringExtra("para_quem") ?: ""
        val placa = intent.getStringExtra("placa") ?: ""
        val motorista = intent.getStringExtra("motorista") ?: ""
        val observacao = intent.getStringExtra("observacao") ?: ""
        val dslFromIntent = intent.getLongExtra("diesel", 0L)
        val localFromIntent = intent.getStringExtra("local") ?: ""

        // Extras para definir o tipo de placa e de KM:
        val semKm = intent.getStringExtra("semKm") ?: ""
        val tipoPlaca = intent.getBooleanExtra("tipoPlaca", true)
        val dataLong = intent.getLongExtra("data", -1L)

        val dataMillis = intent.getLongExtra("data", -1L)
        if (dataMillis != -1L) {
            selectedDateMillis = dataMillis
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            dataEditText.setText(dateFormat.format(Date(dataMillis)))
        }

        // Configura para que o campo de data abra um DatePicker ao ser clicado
        dataEditText.setOnClickListener {
            showDateTimePicker()
        }
        //Log.d("DEBUG", "diesel Status: $dslFromIntent") // Depuração
        // Configura o AutoCompleteTextView para a placa
        placaAutoCompleteTextView.setText(placa)
        placaAutoCompleteTextView.threshold = 1  // As sugestões começam a partir do 1º caractere

        kmEditText.tag = kmFromIntent.toInt()
        kmEditText.setText(NumberFormat.getNumberInstance(Locale("pt", "BR")).format(kmFromIntent.toInt()))

        liEditText.tag = liFromIntent.toInt()
        liEditText.setText(formatValueWithComma(liFromIntent.toInt()))

        qaEditText.tag = qaFromIntent.toInt()
        qaEditText.setText(formatValueWithComma(qaFromIntent.toInt()))

        lfAutoCompleteTextView.tag = lfFromIntent.toInt()
        lfAutoCompleteTextView.setText(formatValueWithComma(lfFromIntent.toInt()))

        // Se o campo arla não tiver valor, assume 0
        EditTextArla.tag = 0
        EditTextArla.setText(formatValueWithComma(0))

        paraQuemEditText.setText(paraQuem)
        motivoEditText.setText(motivo)
        localEditText.setText(localFromIntent) // Agora o campo "local" é preenchido com o dado recebido

        motoristaEditText.setText(motorista)
        observacaoEditText.setText(observacao)

        setupDestinationAutoComplete()
        setupMotivationAutoComplete()
        setupMotoristaAutoComplete()

        // Configura o checkbox de placa (checkBoxExtra)
        // Se o tipo recebido for "extra", marca o checkbox e carrega as opções de placas extra;
        // caso contrário, carrega as opções padrão.
        checkBoxExtra.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                fetchPlatesExtra()
            } else {
                fetchPlates()
            }
        }

        if (!tipoPlaca) {  // false indica "extra"
            checkBoxExtra.isChecked = true
            fetchPlatesExtra()
        } else {
            checkBoxExtra.isChecked = false
            fetchPlates()
        }

        // Configuração dos checkboxes para KM:
        // Se o registro veio com valor em "semKm", então marca a opção correspondente;
        // caso contrário, assume que foi gravado com KM.
        if (semKm.isNotEmpty()) {
            checkBoxkm.isChecked = false
            when (semKm) {
                "Galão" -> {
                    checkBoxGalao.isChecked = true
                    checkBoxHNF.isChecked = false
                }
                "Sem Odômetro" -> {
                    checkBoxHNF.isChecked = true
                    checkBoxGalao.isChecked = false
                }
                else -> {
                    // Valor inesperado: marca Galão por default
                    checkBoxGalao.isChecked = true
                    checkBoxHNF.isChecked = false
                }
            }
        } else {
            checkBoxkm.isChecked = true
            checkBoxGalao.isChecked = false
            checkBoxHNF.isChecked = false
        }
        // Configura o campo KM (habilitado se o checkbox km estiver marcado)
        kmEditText.isEnabled = checkBoxkm.isChecked

        // Configura os watchers e validações
        setupEditTextsAndWatchers()

        // Configuração dos checkboxes de KM (obrigatoriedade: ao menos um deve estar selecionado)
        checkBoxkm.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !checkBoxGalao.isChecked && !checkBoxHNF.isChecked) {
                // Impede que seja desmarcada se for a única opção selecionada
                checkBoxkm.isChecked = true
            } else if (isChecked) {
                // Se "km" for selecionado, desmarca os outros
                if (checkBoxGalao.isChecked) checkBoxGalao.isChecked = false
                if (checkBoxHNF.isChecked) checkBoxHNF.isChecked = false
                kmEditText.isEnabled = true
            } else {
                kmEditText.isEnabled = false
            }
            validateFields()
        }
        checkBoxGalao.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !checkBoxkm.isChecked && !checkBoxHNF.isChecked) {
                checkBoxGalao.isChecked = true
            } else if (isChecked) {
                if (checkBoxkm.isChecked) checkBoxkm.isChecked = false
                if (checkBoxHNF.isChecked) checkBoxHNF.isChecked = false
                kmEditText.isEnabled = false
            }
            validateFields()
        }
        checkBoxHNF.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !checkBoxkm.isChecked && !checkBoxGalao.isChecked) {
                checkBoxHNF.isChecked = true
            } else if (isChecked) {
                if (checkBoxkm.isChecked) checkBoxkm.isChecked = false
                if (checkBoxGalao.isChecked) checkBoxGalao.isChecked = false
                kmEditText.isEnabled = false
            }
            validateFields()
        }

        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({ enableAllButtons() }, 1000)
            finish()
        }
        saveButton.setOnClickListener {
            disableAllButtons()
            updateRecord()
            saveButton.postDelayed({ enableAllButtons() }, 1000)
        }
    }

    private fun showDateTimePicker() {
        val calendar = Calendar.getInstance()
        if (selectedDateMillis != -1L) {
            calendar.timeInMillis = selectedDateMillis
        }
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)
                selectedDateMillis = calendar.timeInMillis
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                dataEditText.setText(dateFormat.format(Date(selectedDateMillis)))
            }, hour, minute, true).show()
        }, year, month, day).show()
    }

    private fun setupDestinationAutoComplete() {
        val destinations = resources.getStringArray(R.array.options_array_destino)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, destinations)
        localEditText.setAdapter(adapter)
    }

    // Configura o AutoComplete para o campo "motivo"
    private fun setupMotivationAutoComplete() {
        motivoEditText.threshold = 0
        val motivos = resources.getStringArray(R.array.options_array_motivos)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, motivos)
        motivoEditText.setAdapter(adapter)
    }

    // Configura o AutoComplete para o campo "motorista"
    private fun setupMotoristaAutoComplete() {
        val motoristas = resources.getStringArray(R.array.options_array_motoristas)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, motoristas)
        motoristaEditText.setAdapter(adapter)
    }

    private fun fetchPlates() {
        fetchVehiclesByCategory("PLACA")
    }

    private fun fetchPlatesExtra() {
        fetchVehiclesByCategory("EXTRA")
    }

    private fun fetchVehiclesByCategory(category: String) {
        firestore.collection(COL_VEICULOS)
            .whereEqualTo(FIELD_CATEGORIA, category)
            .orderBy(FIELD_IDENTIFICADOR, Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                platesList.clear()
                for (document in result) {
                    val isActive = document.getBoolean(FIELD_ATIVO)
                    if (isActive == false) {
                        continue
                    }
                    val identificador = document.getString(FIELD_IDENTIFICADOR) ?: document.id
                    if (identificador.isNotBlank()) {
                        platesList.add(identificador)
                    }
                }
                val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, platesList)
                placaAutoCompleteTextView.setAdapter(adapter)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro recuperando placas: ${exception.message}", Toast.LENGTH_SHORT).show()            }
    }

    private fun setupEditTextsAndWatchers() {
        kmEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = kmEditText.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                kmEditText.tag = rawValue
                val formattedKm = NumberFormat.getNumberInstance(Locale("pt", "BR")).format(rawValue)
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        liEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = liEditText.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                liEditText.tag = rawValue
                liEditText.setText(formatValueWithComma(rawValue))
            } else {
                val rawValue = (liEditText.tag as? Int)?.toString() ?: ""
                liEditText.setText(rawValue)
            }
            liEditText.setSelection(liEditText.text.length)
            validateFields()
        }
        liEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (liEditText.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    liEditText.tag = input.toIntOrNull() ?: 0
                    isLfManuallyModified = false
                }
                calculateFinalLiters()
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        qaEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = qaEditText.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                qaEditText.tag = rawValue
                qaEditText.setText(formatValueWithComma(rawValue))
            } else {
                val rawValue = (qaEditText.tag as? Int)?.toString() ?: ""
                qaEditText.setText(rawValue)
            }
            qaEditText.setSelection(qaEditText.text.length)
            validateFields()
        }
        qaEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (qaEditText.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    qaEditText.tag = input.toIntOrNull() ?: 0
                    isLfManuallyModified = false
                }
                calculateFinalLiters()
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        lfAutoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = lfAutoCompleteTextView.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                lfAutoCompleteTextView.tag = rawValue
                lfAutoCompleteTextView.setText(formatValueWithComma(rawValue))
            } else {
                val rawValue = (lfAutoCompleteTextView.tag as? Int)?.toString() ?: ""
                lfAutoCompleteTextView.setText(rawValue)
            }
            lfAutoCompleteTextView.setSelection(lfAutoCompleteTextView.text.length)
            validateFields()
        }
        lfAutoCompleteTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (lfAutoCompleteTextView.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    lfAutoCompleteTextView.tag = input.toIntOrNull() ?: 0
                    isLfManuallyModified = true
                }
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })

        EditTextArla.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = EditTextArla.text.toString().replace("[^0-9]".toRegex(), "").toIntOrNull() ?: 0
                EditTextArla.tag = rawValue
                EditTextArla.setText(formatValueWithComma(rawValue))
            } else {
                val rawValue = (EditTextArla.tag as? Int)?.toString() ?: ""
                EditTextArla.setText(rawValue)
            }
            EditTextArla.setSelection(EditTextArla.text.length)
        }
        EditTextArla.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (EditTextArla.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    EditTextArla.tag = input.toIntOrNull() ?: 0
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
        })
    }

    // Formata um valor inteiro para o padrão "xxx,x"
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

    // Calcula o montante final: LF = LI + QA (exceto se o usuário editou manualmente o campo LF)
    private fun calculateFinalLiters() {
        val liValue = liEditText.tag as? Int ?: 0
        val qaValue = qaEditText.tag as? Int ?: 0
        val lfinal = liValue + qaValue
        if (!isLfManuallyModified) {
            lfAutoCompleteTextView.tag = lfinal
            lfAutoCompleteTextView.setText(formatValueWithComma(lfinal))
        }
    }

    private fun validateFields() {
        val isKmRequired = checkBoxkm.isChecked
        val kmValue = (kmEditText.tag as? Int) ?: 0
        val liValue = (liEditText.tag as? Int) ?: 0
        val lfValue = (lfAutoCompleteTextView.tag as? Int) ?: 0
        val qaValue = (qaEditText.tag as? Int) ?: 0

        val isKmValid = if (isKmRequired) kmValue > 0 else true
        if (!isKmValid) {
            kmEditText.error = "Preencha o KM!"
        } else {
            kmEditText.error = null
        }

        if (liValue < 0) {
            liEditText.error = "Montante Inicial inválido"
        } else {
            liEditText.error = null
        }

        val isQaValid = qaValue >= 0
        if (!isQaValid) {
            qaEditText.error = "Informe um valor de Abastecimento válido"
        } else {
            qaEditText.error = null
        }

        val expectedLf = liValue + qaValue
        val isLfValid = lfValue == expectedLf
        if (!isLfValid) {
            val formattedLf = formatValueWithComma(expectedLf)
            lfAutoCompleteTextView.error = "O Montante Final deve ser $formattedLf"
        } else {
            lfAutoCompleteTextView.error = null
        }

        val isParaQuemValid = paraQuemEditText.text.toString().isNotEmpty()
        val isMotivoValid = motivoEditText.text.toString().isNotEmpty()
        val isLocalValid = localEditText.text.toString().isNotEmpty()

        if (!isParaQuemValid) {
            paraQuemEditText.error = "Preencha este campo."
        } else {
            paraQuemEditText.error = null
        }
        if (!isMotivoValid) {
            motivoEditText.error = "Preencha este campo."
        } else {
            motivoEditText.error = null
        }
        if (!isLocalValid) {
            paraQuemEditText.error = "Preencha este campo."
        } else {
            paraQuemEditText.error = null
        }

        val plateInput = placaAutoCompleteTextView.text.toString()
        val isPlacaValid = plateInput.isNotEmpty() && platesList.contains(plateInput)
        if (!isPlacaValid) {
            placaAutoCompleteTextView.error = "Placa inválida ou não existente no BD."
        } else {
            placaAutoCompleteTextView.error = null
        }

        // Garante que ao menos uma das checkboxes de KM esteja selecionada.
        val isCheckboxSelected = checkBoxkm.isChecked || checkBoxGalao.isChecked || checkBoxHNF.isChecked

        val allFieldsValid = isKmValid && (liValue >= 0) && isQaValid && isLfValid &&
                isParaQuemValid && isMotivoValid && isPlacaValid && isCheckboxSelected && isLocalValid

        saveButton.isEnabled = allFieldsValid
        val color = if (allFieldsValid) {
            ContextCompat.getColor(this, R.color.azul_escuro)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        saveButton.setBackgroundColor(color)
    }

    private fun updateRecord() {
        if (documentId == null) {
            Toast.makeText(this, "ID do documento não encontrado!", Toast.LENGTH_SHORT).show()
            return
        }

        // 1) Referência ao documento original
        val docRef = firestore.collection("03-combustivel").document(documentId!!)

        // Buscar o documento original para fazer backup
        docRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (!documentSnapshot.exists()) {
                    Toast.makeText(this, "Documento não encontrado para backup!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val originalData = documentSnapshot.data ?: emptyMap<String, Any>()

                val oldQa = originalData["qa"] as? Long ?: 0L
                val oldDiesel = originalData["diesel"] as? Long ?: 0L

                // Verifica se o QA realmente mudou
                val newQa = (qaEditText.tag as? Int)?.toLong() ?: 0L
                val qaAlterado = (newQa != oldQa)

                val dateFormat = SimpleDateFormat("dd-MM-yyyy_HH-mm", Locale.getDefault())
                val nowStr = dateFormat.format(Date())
                val backupDocId = "$docDateStr ED_COMBUSTIVEL $nowStr"

                // 2) Salva a versão antiga em "04-backup"
                firestore.collection("04-backup")
                    .document(backupDocId)
                    .set(originalData)
                    .addOnSuccessListener {
                        // Sucesso ao salvar backup; agora monta e faz o update
                        val updatedData = hashMapOf<String, Any>()

                        // Se a checkbox "km" estiver selecionada, salva o valor numérico no campo "km".
                        // Caso contrário, salva o valor da opção (ex.: "Galão" ou "Sem Odômetro") no campo "semKm".
                        if (checkBoxkm.isChecked) {
                            val kmInt = kmEditText.tag as? Int ?: 0
                            updatedData["km"] = kmInt
                        } else {
                            val semKmValue = when {
                                checkBoxGalao.isChecked -> "Galão"
                                checkBoxHNF.isChecked -> "Sem Odômetro"
                                else -> ""
                            }
                            updatedData["semKm"] = semKmValue
                        }

                        val liValue = liEditText.tag as? Int ?: 0
                        val lfValue = lfAutoCompleteTextView.tag as? Int ?: 0
                        val arla = EditTextArla.tag as? Int ?: 0

                        val paraQuem = paraQuemEditText.text.toString()
                        val motivo = motivoEditText.text.toString()
                        val placa = placaAutoCompleteTextView.text.toString()
                        val local = localEditText.text.toString()
                        val selectedDate = Date(selectedDateMillis)

                        updatedData["li"] = liValue
                        updatedData["lf"] = lfValue
                        updatedData["qa"] = newQa
                        updatedData["para_quem"] = paraQuem
                        updatedData["motivo"] = motivo
                        updatedData["placa"] = placa
                        updatedData["local"] = local
                        updatedData["arla"] = arla
                        updatedData["data"] = selectedDate

                        // Campos motorista e observacao
                        updatedData["motorista"] = motoristaEditText.text.toString()
                        updatedData["observacao"] = observacaoEditText.text.toString()

                        // 3) Agora atualiza o documento original na coleção "03-combustivel"
                        // 3) Se QA mudou, busca o último doc (ignora este) para pegar o diesel e recalcular
                        if (qaAlterado) {
                            fun applyDiesel(lastDiesel: Long) {
                                val newDiesel = lastDiesel - newQa
                                updatedData["diesel"] = newDiesel

                                docRef.update(updatedData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Documento atualizado!", Toast.LENGTH_SHORT).show()
                                        showSuccessOverlay()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            fun runFallbackQuery() {
                                firestore.collection("03-combustivel")
                                    .orderBy("data", Query.Direction.DESCENDING)
                                    .whereNotEqualTo("__name__", documentId!!)  // ignora o doc atual
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { snapshot ->
                                        val lastDiesel = if (!snapshot.isEmpty) {
                                            snapshot.documents[0].getLong("diesel") ?: 0L
                                        } else {
                                            0L
                                        }
                                        applyDiesel(lastDiesel)
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Erro ao buscar último diesel: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }

                            BombasRepository.fetchEstoqueAtual(
                                firestore = firestore,
                                onSuccess = { estoqueAtual ->
                                    applyDiesel(estoqueAtual)
                                },
                                onFailure = { exception ->
                                    Log.w(
                                        "EditCombustivelActivity",
                                        "Falha ao ler bombas/diesel_patio, usando fallback.",
                                        exception
                                    )
                                    runFallbackQuery()
                                }
                            )
                        } else {
                            // Se QA não mudou, simplesmente mantém o diesel original e atualiza os demais campos
                            updatedData["diesel"] = oldDiesel
                            docRef.update(updatedData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Documento atualizado (diesel inalterado)!", Toast.LENGTH_SHORT).show()
                                    showSuccessOverlay()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Erro ao atualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao fazer backup: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar documento para backup: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showSuccessOverlay() {
        val overlay = findViewById<FrameLayout>(R.id.successOverlay)
        val gifView = findViewById<ImageView>(R.id.successGif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.thumbs_up)
            .into(gifView)
        overlay.visibility = View.VISIBLE
        overlay.postDelayed({
            overlay.visibility = View.GONE
            finish()
        }, 3000)
    }

    private fun disableAllButtons() {
        backButton.isEnabled = false
        saveButton.isEnabled = false
    }

    private fun enableAllButtons() {
        backButton.isEnabled = true
        saveButton.isEnabled = true
    }
}
