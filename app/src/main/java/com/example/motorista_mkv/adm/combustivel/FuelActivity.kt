package com.example.motorista_mkv.adm.combustivel

import android.app.DatePickerDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.motorista_mkv.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FuelActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var plateAutoComplete: AutoCompleteTextView
    private lateinit var kmEditText: EditText
    private lateinit var paraQuemEditText: AutoCompleteTextView
    private lateinit var motivoEditText: AutoCompleteTextView
    private lateinit var localEditText: AutoCompleteTextView

    private lateinit var lfAutoCompleteTextView: AutoCompleteTextView
    private lateinit var liEditText: EditText
    private lateinit var qaEditText: EditText

    private lateinit var motoristaEditText: AutoCompleteTextView
    private lateinit var observacaoEditText: EditText
    private lateinit var motoristaText: TextView
    private lateinit var observacaoText: TextView

    private lateinit var checkBoxExtra: CheckBox
    private lateinit var EditTextArla: EditText

    private lateinit var checkBoxDiesel: CheckBox
    private lateinit var textDiesel: TextView
    private lateinit var dieselEditText: EditText
    private lateinit var scrollView1: ScrollView
    private lateinit var scrollView2: ScrollView

    private lateinit var checkBoxkm: CheckBox
    private lateinit var checkBoxGalao: CheckBox
    private lateinit var checkBoxHNF: CheckBox

    private lateinit var saveButton: Button
    private lateinit var backButton: Button

    private var previousKm: Int = 0 // Variável para armazenar a quilometragem anterior
    private var currentKm: Int = 0

    private var ultimaLitragemFirestore: Int = 0
    private var dfrncl: Int? = null // Variável para armazenar o valor

    // Flag para indicar se o usuário alterou manualmente o campo LF
    private var isLfManuallyModified: Boolean = false
    private val PREFS_NAME = "LoginPrefs"
    private val platesList = mutableListOf<String>()

    private var isDebugMode  = false
    private var isEdicaoMode = false
    private var docIdToEdit: String? = null
    private lateinit var dateEditText: EditText


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuel)

        // Inicializar os campos
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        plateAutoComplete = findViewById(R.id.plateAutoCompleteTextView) // Substitua o ID no layout!
        plateAutoComplete.imeOptions = EditorInfo.IME_ACTION_NEXT
        plateAutoComplete.setSingleLine(true)

        kmEditText = findViewById(R.id.kmEditText)
        paraQuemEditText = findViewById(R.id.paraQuemEditText)
        motivoEditText = findViewById(R.id.motivoEditText)
        localEditText = findViewById(R.id.localEditText)

        liEditText = findViewById(R.id.liEditText)
        qaEditText = findViewById(R.id.qaEditText)
        lfAutoCompleteTextView = findViewById(R.id.lfAutoCompleteTextView)

        motoristaEditText = findViewById(R.id.motoristaEditText)
        observacaoEditText = findViewById(R.id.observacaoEditText)
        motoristaText = findViewById(R.id.motoristaText)
        observacaoText = findViewById(R.id.observacaoText)

        checkBoxExtra = findViewById(R.id.checkBoxExtra)
        EditTextArla = findViewById(R.id.EditTextArla)

        checkBoxDiesel = findViewById(R.id.checkBoxDiesel)
        textDiesel = findViewById(R.id.textDiesel)
        dieselEditText = findViewById(R.id.dieselEditText)
        scrollView1 = findViewById(R.id.scrollView1)
        scrollView2 = findViewById(R.id.scrollView2)

        checkBoxkm = findViewById(R.id.checkBoxkm)
        checkBoxGalao = findViewById(R.id.checkBoxGalao)
        checkBoxHNF = findViewById(R.id.checkBoxHNF)

        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backFuelButton)

        dateEditText = findViewById(R.id.dateEditText)
        dateEditText.isEnabled = isDebugMode || isEdicaoMode
        isDebugMode  = intent.getBooleanExtra("debug",  false)
        isEdicaoMode = intent.getBooleanExtra("edicao", false)
        docIdToEdit  = intent.getStringExtra("documentId")

        if (isEdicaoMode) {
            // Data
            val dataMillis = intent.getLongExtra("data", -1L)
            if (dataMillis != -1L) {
                val date = Date(dataMillis)
                val sdf  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                dateEditText.setText(sdf.format(date))
                dateEditText.tag = date
            }

            // Campos numéricos ► lembre-se de colocar também em tag
            liEditText.setText(formatValueWithComma(intent.getLongExtra("li", 0L).toInt()))
            liEditText.tag = intent.getLongExtra("li", 0L).toInt()

            qaEditText.setText(formatValueWithComma(intent.getLongExtra("qa", 0L).toInt()))
            qaEditText.tag = intent.getLongExtra("qa", 0L).toInt()

            lfAutoCompleteTextView.setText(formatValueWithComma(intent.getLongExtra("lf", 0L).toInt()))
            lfAutoCompleteTextView.tag = intent.getLongExtra("lf", 0L).toInt()

            kmEditText.setText(formatValueWithComma(intent.getLongExtra("km", 0L).toInt()))
            kmEditText.tag = intent.getLongExtra("km", 0L).toInt()

            // Strings
            plateAutoComplete.setText(intent.getStringExtra("placa").orEmpty())
            paraQuemEditText.setText(intent.getStringExtra("para_quem").orEmpty())
            motivoEditText.setText(intent.getStringExtra("motivo").orEmpty())
            localEditText.setText(intent.getStringExtra("local").orEmpty())
            motoristaEditText.setText(intent.getStringExtra("motorista").orEmpty())
            observacaoEditText.setText(intent.getStringExtra("observacao").orEmpty())

            // Placa extra ou não
            val tipoPlaca = intent.getBooleanExtra("tipoPlaca", true)
            checkBoxExtra.isChecked = !tipoPlaca
        }else{fetchLastLfFromFirestore()}

        checkAuthorization()

        // Chama o cálculo de LF para atualizar o campo inicialmente
        calculateFinalLiters()

        // Verifica o possivel recebimento deQA da notificação
        if (intent.hasExtra("DFRNCL_VALUE")) {
            dfrncl = intent.getIntExtra("DFRNCL_VALUE", 0)
            Log.d("FuelActivity", "Valor recebido de dfrncl: $dfrncl")

            // Insere o valor no EditText
            qaEditText.setText(dfrncl.toString())
            qaEditText.tag = dfrncl
            qaEditText.setText(formatValueWithComma(dfrncl!!))
        } else {
            Log.d("FuelActivity", "Nenhum valor de dfrncl recebido.")
        }
        if (isDebugMode || isEdicaoMode) {
            dateEditText.setOnClickListener {
                val cal = Calendar.getInstance()
                DatePickerDialog(
                    this,
                    { _, y, m, d ->
                        cal.set(Calendar.YEAR,  y)
                        cal.set(Calendar.MONTH, m)
                        cal.set(Calendar.DAY_OF_MONTH, d)
                        // formate como preferir:
                        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        dateEditText.setText(sdf.format(cal.time))
                        dateEditText.tag = cal.time       // guarda o Date em tag
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
        } else {
            dateEditText.isEnabled = false          // modo normal não escolhe data
        }

        // Desabilita o botão de Salvar inicialmente
        saveButton.isEnabled = false
        saveButton.setBackgroundColor(
            ContextCompat.getColor(this, android.R.color.darker_gray)
        )
        // Listener para botão "Voltar"
        backButton.setOnClickListener {
            disableAllButtons()
            backButton.postDelayed({
                enableAllButtons()
            }, 1000)
            finish()
        }

        setupEditTextsAndWatchers()
        fetchPlates()

        plateAutoComplete.setOnItemClickListener { parent, _, position, _ ->
            val selectedPlate = parent.getItemAtPosition(position).toString()
            plateAutoComplete.error = null
            if (!checkBoxExtra.isChecked) {
                fetchLatestKmForPlate(selectedPlate)
            }
            validateFields()
        }

        // Ao clicar em uma opção do autocomplete de "local"
        localEditText.setOnItemClickListener { _, _, _, _ ->
            localEditText.error = null
            localEditText.clearFocus()
            validateFields()
        }
        localEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Aplica o mesmo tratamento para "motivo" e "paraQuem"
        motivoEditText.setOnItemClickListener { _, _, _, _ ->
            motivoEditText.error = null
            motivoEditText.clearFocus()
            validateFields()
        }
        motivoEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        paraQuemEditText.setOnItemClickListener { _, _, _, _ ->
            paraQuemEditText.error = null
            paraQuemEditText.clearFocus()
            validateFields()
        }
        paraQuemEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateFields()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        checkBoxExtra.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Buscar campo "extra"
                fetchPlatesExtra()
            } else {
                fetchPlates()
            }
        }

        checkBoxkm.isChecked = true
        kmEditText.isEnabled = checkBoxkm.isChecked

        checkBoxkm.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !checkBoxGalao.isChecked && !checkBoxHNF.isChecked) {
                checkBoxkm.isChecked = true
            } else if (isChecked) {
                checkBoxGalao.isChecked = false
                checkBoxHNF.isChecked = false
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
                checkBoxkm.isChecked = false
                checkBoxHNF.isChecked = false
                kmEditText.isEnabled = false
            }
            validateFields()
        }

        checkBoxHNF.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked && !checkBoxkm.isChecked && !checkBoxGalao.isChecked) {
                checkBoxHNF.isChecked = true
            } else if (isChecked) {
                checkBoxkm.isChecked = false
                checkBoxGalao.isChecked = false
                kmEditText.isEnabled = false
            }
            validateFields()
        }

        checkBoxDiesel.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                scrollView1.visibility = View.GONE
                scrollView2.visibility = View.GONE
                textDiesel.visibility = View.VISIBLE
                dieselEditText.visibility = View.VISIBLE
            } else {
                scrollView1.visibility = View.VISIBLE
                scrollView2.visibility = View.VISIBLE
                textDiesel.visibility = View.GONE
                dieselEditText.visibility = View.GONE
            }
            validateFields()
        }

        setupMotivationAutoComplete()
        setupDestinationAutoComplete()
        setupMotoristaAutoComplete()

        // Listener do EditText de motivo para fechar o teclado ao pressionar "Done"
        motivoEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(motivoEditText.windowToken, 0)
                localEditText.requestFocus()
                return@setOnEditorActionListener true
            }
            false
        }

        dieselEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                dieselEditText.clearFocus()

                // Opcional: fechar o teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(dieselEditText.windowToken, 0)

                // Se quiser chamar a validação ou algo após remover foco
                validateFields()

                // Indica que tratamos a ação de "Done/Next"
                return@setOnEditorActionListener true
            }
            // Se não foi a ação “Done/Next”
            false
        }

        // Clique para salvar
        saveButton.setOnClickListener {
            disableAllButtons()

            if (isEdicaoMode) {
                // atualização (independe de diesel ou não)
                updateFuel(docIdToEdit ?: return@setOnClickListener)
            } else {
                if (checkBoxDiesel.isChecked) {
                    saveFuel2()
                } else {
                    saveFuel()
                }
            }

            saveButton.postDelayed({
                enableAllButtons()
            }, 5000)
        }


        lfAutoCompleteTextView.setOnEditorActionListener { textView, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // 1) Perder o foco
                lfAutoCompleteTextView.clearFocus()

                // 2) Opcional: esconder teclado
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(lfAutoCompleteTextView.windowToken, 0)

                true // Indica que tratamos a ação
            } else {
                false // Não tratamos outras ações
            }
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
    }

    private fun fetchLastLfFromFirestore() {
        val query = firestore.collection("03-combustivel")
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(1)

        query.get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    // Aqui definimos a variável global 'ultimaLitragemFirestore'
                    ultimaLitragemFirestore = document.getLong("lf")?.toInt() ?: 0

                    // Preenche 'liEditText' com esse valor
                    liEditText.tag = ultimaLitragemFirestore
                    liEditText.setText(formatValueWithComma(ultimaLitragemFirestore))

                    // Recalcula LF, pois LI mudou
                    calculateFinalLiters()
                    // Força a validação após atualizar o campo
                    validateFields()
                } else {
                    Toast.makeText(this, "Nenhum registro encontrado no combustivel!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar dados: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkAuthorization() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2") {
            motoristaText.visibility = View.VISIBLE
            motoristaEditText.visibility = View.VISIBLE
            observacaoText.visibility = View.VISIBLE
            observacaoEditText.visibility = View.VISIBLE
            checkBoxDiesel.visibility = View.VISIBLE

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        } else {
            motoristaText.visibility = View.GONE
            motoristaEditText.visibility = View.GONE
            observacaoText.visibility = View.GONE
            observacaoEditText.visibility = View.GONE
            checkBoxDiesel.visibility = View.GONE

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private fun setupDestinationAutoComplete() {
        val destinations = resources.getStringArray(R.array.options_array_destino)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, destinations)
        localEditText.setAdapter(adapter)
    }

    private fun setupMotoristaAutoComplete() {
        val motorista = resources.getStringArray(R.array.options_array_motoristas)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, motorista)
        paraQuemEditText.setAdapter(adapter)
        motoristaEditText.setAdapter(adapter)
    }

    private fun setupEditTextsAndWatchers() {
        // KM EDITTEXT: Ao perder foco, formata com pontuação. Ao ganhar foco, mostra valor cru.
        kmEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = kmEditText.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                kmEditText.tag = rawValue
                val formattedKm = formatValueWithComma(rawValue)
                kmEditText.setText(formattedKm)
                //val formattedKm = NumberFormat.getNumberInstance(Locale("pt", "BR")).format(rawValue)
                //kmEditText.setText(formattedKm)
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

        // LI EDITTEXT
        liEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = liEditText.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                liEditText.tag = rawValue
                liEditText.setText(formatValueWithComma(rawValue))
            } else {
                // Ao ganhar o foco, mostra o valor armazenado no tag (para preservar o valor correto)
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
                    // Se o frentista modificar li, reseta a modificação manual de lf
                    isLfManuallyModified = false
                }
                calculateFinalLiters()
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // QA EDITTEXT
        qaEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = qaEditText.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
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
                    // Se o frentista modificar qa, reseta a modificação manual de lf
                    isLfManuallyModified = false
                }
                calculateFinalLiters()
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // LF AUTO COMPLETE TEXTVIEW - Agora editável manualmente
        lfAutoCompleteTextView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = lfAutoCompleteTextView.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                lfAutoCompleteTextView.tag = rawValue
                lfAutoCompleteTextView.setText(formatValueWithComma(rawValue))
            } else {
                // Ao ganhar o foco, mostra o valor sem formatação para facilitar a edição
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
                    // Se o frentista modificar lf manualmente, ativa a flag
                    isLfManuallyModified = true
                }
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        EditTextArla.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = EditTextArla.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                EditTextArla.tag = rawValue

                // Se quiser formatar com vírgula, chame a mesma função de formatação
                EditTextArla.setText(formatValueWithComma(rawValue))

            } else {
                val rawValue = (EditTextArla.tag as? Int)?.toString() ?: ""
                EditTextArla.setText(rawValue)
            }
            EditTextArla.setSelection(EditTextArla.text.length)
            // Se desejar, chame validateFields() ou algo semelhante
        }
        EditTextArla.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (EditTextArla.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    EditTextArla.tag = input.toIntOrNull() ?: 0
                }
                // Se quiser disparar validações ou recalcular algo, pode chamar aqui
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        motivoEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                motivoEditText.showDropDown()
            }
        }

        dieselEditText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val rawValue = dieselEditText.text.toString()
                    .replace("[^0-9]".toRegex(), "")
                    .toIntOrNull() ?: 0
                dieselEditText.tag = rawValue

                // Se quiser formatar o texto, chame algo como:
                dieselEditText.setText(formatValueWithComma(rawValue))
            } else {
                // Ao ganhar foco, exibe o valor cru
                val rawValue = (dieselEditText.tag as? Int)?.toString() ?: ""
                dieselEditText.setText(rawValue)
            }
            dieselEditText.setSelection(dieselEditText.text.length)
            validateFields()
        }
        dieselEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (dieselEditText.hasFocus()) {
                    val input = s.toString().replace("[^0-9]".toRegex(), "")
                    dieselEditText.tag = input.toIntOrNull() ?: 0
                }
                validateFields()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Função para formatar o valor com vírgula e ponto
    private fun formatValueWithComma(rawValue: Int): String {
        val decimalValue = rawValue / 10.0

        // Usa DecimalFormat em Locale PT-BR com 1 casa decimal
        val numberFormat = NumberFormat.getNumberInstance(Locale("pt", "BR")) as DecimalFormat
        numberFormat.applyPattern("#,###.0")  // 1 casa decimal

        return numberFormat.format(decimalValue)
    }

    /* Função para formatar o valor com vírgula VELHO
    private fun formatValueWithComma(value: Int): String {
        val rawString = value.toString()
        return if (rawString.length > 1) {
            val beforeComma = rawString.substring(0, rawString.length - 1)
            val afterComma = rawString.substring(rawString.length - 1)
            "$beforeComma,$afterComma"
        } else {
            "0,$rawString"
        }
    } */

    // Função para calcular LF
    private fun calculateFinalLiters() {
        val liValue = liEditText.tag as? Int ?: 0 // Valor bruto de LI
        val qaValue = qaEditText.tag as? Int ?: 0 // Valor bruto de QA
        val lfinal = liValue + qaValue // Soma dos valores

        // Atualiza LF somente se o usuário NÃO tiver modificado manualmente
        if (!isLfManuallyModified) {
            lfAutoCompleteTextView.tag = lfinal
            lfAutoCompleteTextView.setText(formatValueWithComma(lfinal))
        }
    }

    private fun setupMotivationAutoComplete() {
        motivoEditText.threshold = 0
        val reason = resources.getStringArray(R.array.options_array_motivos)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reason)
        motivoEditText.setAdapter(adapter)
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

    private fun fetchPlatesExtra() {
        firestore.collection("01-placas")
            .orderBy("extra", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                platesList.clear()
                for (document in result) {
                    val plateNumber = document.getString("extra")
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
        firestore.collection("caminhao")
            .document(plate)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val kmValue = document.getLong("km")
                    if (kmValue != null) {
                        val km = kmValue.toInt()
                        previousKm = km
                        kmEditText.tag = km  // Atualiza o tag para uso na validação
                        val formattedKm = formatValueWithComma(km)
                        kmEditText.setText(formattedKm)
                    } else {
                        Toast.makeText(this, "KM não encontrado para esta placa; insira manualmente.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Nenhum caminhão encontrado para esta placa.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao buscar caminhão: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Erro ao buscar quilometragem: ${exception.message}")
            }
    }

    private fun validateFields() {
        val isDieselChecked = checkBoxDiesel.isChecked

        if (isDieselChecked) {
            // Se o Diesel estiver marcado, apenas dieselEditText deve ser obrigatório
            val isDieselValid = dieselEditText.text.toString().isNotEmpty()
            if (!isDieselValid) {
                dieselEditText.error = "Preencha este campo."
            } else {
                dieselEditText.error = null
            }

            // Se Diesel é válido, podemos habilitar o botão
            val allFieldsValid = isDieselValid
            saveButton.isEnabled = allFieldsValid
            val color = if (allFieldsValid) {
                ContextCompat.getColor(this, R.color.azul_escuro)
            } else {
                ContextCompat.getColor(this, android.R.color.darker_gray)
            }
            saveButton.setBackgroundColor(color)

            // Como, nesse caso, nenhuma outra validação deve ocorrer,
            // finalizamos a função retornando
            return
        }

        // Valores brutos armazenados no tag
        val isKmRequired = checkBoxkm.isChecked

        val kmValue = (kmEditText.tag as? Int) ?: 0
        val liValue = (liEditText.tag as? Int) ?: 0
        val lfValue = (lfAutoCompleteTextView.tag as? Int) ?: 0
        val qaValue = (qaEditText.tag as? Int) ?: 0

        val isKmValid = if (isKmRequired) kmValue >= previousKm && kmValue > 0 else true
        if (!isKmValid) {
            kmEditText.error = "A quilometragem deve ser maior ou igual a ${formatValueWithComma(previousKm)}"
        } else {
            kmEditText.error = null
        }

        // LI deve ser igual a novaLitragem
        val differenceLi = kotlin.math.abs(liValue - ultimaLitragemFirestore)
        val isLiValid = differenceLi <= 9
        if (!isLiValid) {
            val formattedUL = formatValueWithComma(ultimaLitragemFirestore)
            liEditText.error = "O Montante Inicial deve ser proximo a $formattedUL"
        } else {
            liEditText.error = null
        }

        // QA >= 0
        val isQaValid = qaValue >= 0
        if (!isQaValid) {
            qaEditText.error = "Informe um valor de Abastecimento válido"
        } else {
            qaEditText.error = null
        }

        // LF deve ser proximo a (LI + QA)
        val expectedLf = liValue + qaValue
        val differenceLf = kotlin.math.abs(lfValue - expectedLf)
        val isLfValid = differenceLf <= 9
        if (!isLfValid) {
            val formattedLf = formatValueWithComma(expectedLf)
            lfAutoCompleteTextView.error = "O Montante Final deve ser próximo a $formattedLf"
        } else {
            lfAutoCompleteTextView.error = null
        }

        // Campos "paraQuem" e "motivo" não podem estar vazios
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
            localEditText.error = "Preencha este campo."
        } else {
            localEditText.error = null
        }

        // Verifica se a placa foi selecionada
        val plateInput = plateAutoComplete.text.toString()
        val isPlateValid = plateInput.isNotEmpty() && platesList.contains(plateInput)
        if (!isPlateValid) {
            plateAutoComplete.error = "Placa inválida ou não existente no BD."
        } else {
            plateAutoComplete.error = null
        }

        // Precisa ter ao menos um checkbox (km, galão, semHod) selecionado
        val isCheckboxSelected = checkBoxkm.isChecked || checkBoxGalao.isChecked || checkBoxHNF.isChecked

        // Consolida a validação
        val allFieldsValid = isKmValid && isLiValid && isQaValid && isLfValid &&
                isParaQuemValid && isMotivoValid && isLocalValid && isPlateValid && isCheckboxSelected

        // Habilita/Desabilita o botão e altera a cor de fundo
        saveButton.isEnabled = allFieldsValid
        val color = if (allFieldsValid) {
            ContextCompat.getColor(this, R.color.azul_escuro)
        } else {
            ContextCompat.getColor(this, android.R.color.darker_gray)
        }
        saveButton.setBackgroundColor(color)
    }

    private fun saveFuel() {
        // Montamos o objeto a ser salvo
        // Se "km" estiver marcado => salva "km" como int
        // Senão => salva "semKm" = "Galão" ou "Sem Odômetro"
        val dataToSave = hashMapOf<String, Any>()

        if (checkBoxkm.isChecked) {
            val kmInt = kmEditText.tag as? Int ?: 0
            dataToSave["km"] = kmInt
        } else {
            val semKmValue = when {
                checkBoxGalao.isChecked -> "Galão"
                checkBoxHNF.isChecked -> "Sem Odômetro"
                else -> ""
            }
            dataToSave["semKm"] = semKmValue
        }

        // Indicar se é placa (true) ou extra (false)
        val tipoPlaca = !checkBoxExtra.isChecked  // Se "extra" estiver marcado => false
        dataToSave["tipoPlaca"] = tipoPlaca

        val li = liEditText.tag as? Int ?: 0
        val lf = lfAutoCompleteTextView.tag as? Int ?: 0
        val qa = qaEditText.tag as? Int ?: 0
        val arla = EditTextArla.tag as? Int ?: 0
        val paraQuem = paraQuemEditText.text.toString()
        val motivo = motivoEditText.text.toString()
        val local = localEditText.text.toString()
        val plate = plateAutoComplete.text.toString()

        val currentUser = auth.currentUser
        //val currentDate = Date()
        val chosenDate = dateEditText.tag as? Date ?: Date()   // se não escolher, assume “agora”
        val finalEmail = currentUser?.uid

        dataToSave["li"] = li
        dataToSave["lf"] = lf
        dataToSave["qa"] = qa
        //dataToSave["data"] = currentDate
        dataToSave["data"] = chosenDate
        dataToSave["para_quem"] = paraQuem
        dataToSave["motivo"] = motivo
        dataToSave["local"] = local
        dataToSave["placa"] = plate
        dataToSave["arla"] = arla

        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")

        if (adminStatus == "adm2") {
            dataToSave["motorista"] = motoristaEditText.text.toString()
            dataToSave["observacao"] = observacaoEditText.text.toString()
        } else {
            dataToSave["motorista"] = finalEmail ?: "ERROR"
            dataToSave["observacao"] = "Sem observação"
        }

        // Formata a data para usar como parte do ID
        val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm-ss", Locale.getDefault())
        //val dateStr = dateFormat.format(currentDate)
        val dateStr = dateFormat.format(chosenDate)

        // Monta o ID do documento (ex.: "2023-09-18_12-30-45 usuarioUid")
        val docId = "$dateStr $finalEmail"

        val ref = firestore.collection("03-combustivel")
        // 1) Buscar o último documento (ordem decrescente, limitando a 1)
        ref.orderBy("data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                // 2) Obter o último diesel; se não existir documento algum, considere 0
                val lastDiesel = if (!snapshot.isEmpty) {
                    snapshot.documents[0].getLong("diesel")?.toInt() ?: 0
                } else {
                    0
                }

                // 3) Subtrair o "qa" do último diesel para gerar o novo diesel
                val newDieselValue = lastDiesel - qa

                // 4) Adicionar ao mapa dataToSave
                dataToSave["diesel"] = newDieselValue

                // 5) Salvar o novo documento
                ref.document(docId)
                    .set(dataToSave)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                        showSuccessOverlay()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao salvar dados; Tente novamente!", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar último diesel; Tente novamente!", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveFuel2() {
        val dataToSave = hashMapOf<String, Any>()

        val currentUser = auth.currentUser
        //val currentDate = Date()
        val chosenDate = dateEditText.tag as? Date ?: Date()   // se não escolher, assume “agora”
        val finalEmail = currentUser?.uid
        val lf = liEditText.tag as? Int ?: 0

        //dataToSave["data"] = currentDate
        dataToSave["data"] = chosenDate
        dataToSave["motorista"] = finalEmail ?: "ERROR"
        dataToSave["motivo"] = "Abastecimento de Diesel"
        dataToSave["lf"] = lf

        // Formata a data para usar como parte do ID
        val dateFormat = SimpleDateFormat("dd_MM_yy - HHmm-ss", Locale.getDefault())
        //val dateStr = dateFormat.format(currentDate)
        val dateStr = dateFormat.format(chosenDate)

        // Monta o ID do documento (ex.: "2023-09-18_12-30-45 usuarioUid")
        val docId = "$dateStr $finalEmail"

        val ref = firestore.collection("03-combustivel")
        // 1) Buscar o último documento (ordem decrescente, limitando a 1)
        ref.orderBy("data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                // 2) Obter o último diesel; se não existir documento algum, considere 0
                val lastDiesel = if (!snapshot.isEmpty) {
                    snapshot.documents[0].getLong("diesel")?.toInt() ?: 0
                } else {
                    0
                }
                val dieselABS = dieselEditText.tag as? Int ?: 0
                // 3) Subtrair o "qa" do último diesel para gerar o novo diesel
                val newDieselValue = lastDiesel + dieselABS

                // 4) Adicionar ao mapa dataToSave
                dataToSave["qa"] = dieselABS
                dataToSave["diesel"] = newDieselValue

                // 5) Salvar o novo documento
                ref.document(docId)
                    .set(dataToSave)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dados salvos com sucesso!", Toast.LENGTH_SHORT).show()
                        showSuccessOverlay()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erro ao salvar dados; Tente novamente!", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao buscar último diesel; Tente novamente!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateFuel(documentId: String) {
        val dataToUpdate = hashMapOf<String, Any>()

        // km ou semKm
        if (checkBoxkm.isChecked) {
            dataToUpdate["km"] = (kmEditText.tag as? Int ?: 0)
            dataToUpdate["semKm"] = FieldValue.delete()
        } else {
            val semKmValue = when {
                checkBoxGalao.isChecked -> "Galão"
                checkBoxHNF.isChecked   -> "Sem Odômetro"
                else                    -> ""
            }
            dataToUpdate["semKm"] = semKmValue
            dataToUpdate["km"]    = FieldValue.delete()
        }

        // demais campos
        dataToUpdate["li"]        = (liEditText.tag as? Int ?: 0)
        dataToUpdate["lf"]        = (lfAutoCompleteTextView.tag as? Int ?: 0)
        dataToUpdate["qa"]        = (qaEditText.tag as? Int ?: 0)
        dataToUpdate["para_quem"] = paraQuemEditText.text.toString()
        dataToUpdate["motivo"]    = motivoEditText.text.toString()
        dataToUpdate["local"]     = localEditText.text.toString()
        dataToUpdate["placa"]     = plateAutoComplete.text.toString()
        dataToUpdate["arla"]      = (EditTextArla.tag as? Int ?: 0)

        // data escolhida ou agora
        val chosenDate = dateEditText.tag as? Date ?: Date()
        dataToUpdate["data"] = chosenDate

        // motorista / observação (se ADM2)
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val adminStatus = sharedPreferences.getString("adminStatus", "user")
        if (adminStatus == "adm2") {
            dataToUpdate["motorista"]  = motoristaEditText.text.toString()
            dataToUpdate["observacao"] = observacaoEditText.text.toString()
        }

        firestore.collection("03-combustivel")
            .document(documentId)
            .update(dataToUpdate)
            .addOnSuccessListener {
                Toast.makeText(this, "Documento atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                showSuccessOverlay()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar documento!", Toast.LENGTH_SHORT).show()
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