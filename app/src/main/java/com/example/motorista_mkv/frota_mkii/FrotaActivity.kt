package com.example.motorista_mkv.frota_mkii

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.res.Configuration
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.example.motorista_mkv.R
import com.example.motorista_mkv.util.FontUtils
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class FrotaActivity : AppCompatActivity() {

    private lateinit var table:   TableLayout
    private lateinit var backBtn: Button
    private lateinit var db:      FirebaseFirestore
    private lateinit var toolbar: Toolbar

    companion object {
        private const val BASE_SP = 20f
        private const val PAD = 10
        private const val MOTIVO_MAX_DP = 150
        private val SDF = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    }

    private var fTipo:   String? = null     // "saida" | "chegada" | null
    private var fPlaca:  String? = null     // prefixo ou placa cheia
    private var fIni:    Date?   = null     // início do intervalo
    private var fFim:    Date?   = null     // fim do intervalo
    private val atividadesRef by lazy { db.collection("atividades") }


    override fun onCreate(saved: Bundle?) {
        super.onCreate(saved)
        setContentView(R.layout.activity_frota_new)

        table   = findViewById(R.id.tableLayoutDados)
        backBtn = findViewById(R.id.btnBack)
        toolbar = findViewById(R.id.toolbarFrota)
        db      = FirebaseFirestore.getInstance()

        configToolbar()
        carregar()

        applyDynamicTextSize(findViewById(android.R.id.content), BASE_SP)

        backBtn.setOnClickListener {
            it.isEnabled = false
            it.postDelayed({ it.isEnabled = true; finish() }, 500)
        }
    }

    private fun configToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayUseLogoEnabled(false)
            setLogo(null)
            setDisplayHomeAsUpEnabled(false)
        }
        toolbar.apply {
            title = ""; subtitle = null; logo = null; navigationIcon = null
            setContentInsetStartWithNavigation(0)
            setContentInsetsRelative(0, 0)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_frota, menu)
        // fonte dos itens
        for (i in 0 until toolbar.childCount) {
            val amv = toolbar.getChildAt(i)
            if (amv is ActionMenuView) {
                for (j in 0 until amv.childCount) {
                    val tv = amv.getChildAt(j)
                    if (tv is TextView) {
                        tv.setTextSize(
                            TypedValue.COMPLEX_UNIT_SP,
                            FontUtils.getDynamicFontSize(this, BASE_SP)
                        )
                    }
                }
                break
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_tudo      -> { fTipo = null;        carregar(); true }
        R.id.menu_saidas    -> { fTipo = "saida";     carregar(); true }
        R.id.menu_chegadas  -> { fTipo = "chegada";   carregar(); true }
        R.id.menu_placa     -> { pedirPlaca();  true }
        R.id.menu_data      -> { pedirPeriodo(); true }
        R.id.menu_limpar    -> { limpar(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun pedirPlaca() {
        val input = EditText(this).apply { hint = "ABC‑1D23 ou prefixo"; maxLines = 1 }

        AlertDialog.Builder(this)
            .setTitle("Filtrar por placa")
            .setView(input)
            .setPositiveButton("Filtrar") { _, _ ->
                val txt = input.text.toString().trim().uppercase(Locale.getDefault())
                fPlaca = txt.takeIf { it.isNotEmpty() }
                carregar()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun pedirPeriodo() {
        val hoje = Calendar.getInstance()

        DatePickerDialog(this, { _, y1, m1, d1 ->
            val ini = Calendar.getInstance().apply {
                set(y1, m1, d1, 0, 0, 0); set(Calendar.MILLISECOND, 0)
            }.time

            DatePickerDialog(this, { _, y2, m2, d2 ->
                val fim = Calendar.getInstance().apply {
                    set(y2, m2, d2, 23, 59, 59); set(Calendar.MILLISECOND, 999)
                }.time

                fIni = ini; fFim = fim; carregar()

            }, y1, m1, d1).apply { datePicker.minDate = ini.time }.show()

        }, hoje.get(Calendar.YEAR), hoje.get(Calendar.MONTH), hoje.get(Calendar.DAY_OF_MONTH))
            .show()
    }

    private fun carregar() {
        // 1. monta a query conforme filtro de placa
        var q: Query = if (fPlaca != null) {
            db.collection("atividades")
                .orderBy("placa")
                .orderBy("data", Query.Direction.DESCENDING)
                .startAt(fPlaca)
                .endAt(fPlaca + '\uf8ff')            // prefixo
        } else {
            db.collection("atividades")
                .orderBy("data", Query.Direction.DESCENDING)
        }

        // 2. filtros que ainda cabem no servidor
        fTipo?.let  { q = q.whereEqualTo("tipo", it) }
        // (não podemos fazer range em data se já fizemos range em placa)

        q.get()
            .addOnSuccessListener { snap ->
                // limpa tabela (mantém cabeçalho)
                while (table.childCount > 1) table.removeViewAt(1)

                val docs = snap.documents
                    .filter { filtroDataCliente(it) }    // aplica intervalo de datas, se houver

                docs.forEach { table.addView(row(it)) }
                applyDynamicTextSize(findViewById(android.R.id.content), BASE_SP)
            }
            .addOnFailureListener { toast("Falha ao carregar dados!") }
    }

    // Se fIni/fFim definidos, filtra localmente
    private fun filtroDataCliente(doc: DocumentSnapshot): Boolean {
        if (fIni == null || fFim == null) return true
        val d = doc.getTimestamp("data")?.toDate() ?: return false
        return !(d.before(fIni) || d.after(fFim))
    }

    private fun row(doc: DocumentSnapshot): TableRow {
        val idDoc  = doc.id

        /* ------ dados do documento ------ */
        val tsData = doc.getTimestamp("data")
        val data   = tsData?.toDate()?.let { SDF.format(it) } ?: ""
        val placa  = doc.getString("placa")     ?: ""
        val tipo   = doc.getString("tipo")      ?: ""
        val dest   = doc.getString("destino")   ?: ""
        val mot    = doc.getString("motorista") ?: ""
        val km     = doc.getLong("km")?.toInt() ?: 0
        val motv   = (doc.getString("motivo") ?: "").replace("\n", " ")

        /* ------ cálculo da largura de todas as colunas ------ */
        val scale      = resources.displayMetrics.density
        val orient     = resources.configuration.orientation
        val fator      = if (orient == Configuration.ORIENTATION_LANDSCAPE) 1.5f else 1f
        val colWidthPx = (MOTIVO_MAX_DP * fator * scale).toInt()

        /* ------ factory de célula ------ */
        fun cel(txt: String, campoFS: String? = null): TextView =
            TextView(this).apply {
                layoutParams = TableRow.LayoutParams(colWidthPx, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(PAD, PAD, PAD, PAD)
                setBackgroundColor(ContextCompat.getColor(context, R.color.azul_claroFundo))
                setTextSize(
                    TypedValue.COMPLEX_UNIT_SP,
                    FontUtils.getDynamicFontSize(context, BASE_SP)
                )
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                text = txt

                campoFS?.let { field ->
                    // destaque visual + clique para editar
                    //setTextColor(ContextCompat.getColor(context, R.color.link_color))
                    paint.isUnderlineText = true
                    setOnClickListener { abrirDialogoEdicao(idDoc, field, txt) }
                }
            }

        /* ------ construção da linha ------ */
        return TableRow(this).apply {
            // espaçamento extra entre linhas
            setPadding(0, PAD, 0, PAD)

            /* ——— colunas ——— */
            addView(
                cel(data).apply {               // Data: editável via Date+Time picker
                    //setTextColor(ContextCompat.getColor(context, R.color.link_color))
                    paint.isUnderlineText = true
                    setOnClickListener { tsData?.toDate()?.let { abrirDialogoData(idDoc, it) } }
                }
            )
            addView(cel(placa))                    // leitura
            addView(cel(tipo))                     // leitura
            addView(cel(dest, "destino"))          // editável
            addView(cel(mot,  "motorista"))        // editável
            addView(cel(km.toString(), "km"))      // editável (Int)
            addView(cel(motv, "motivo"))           // editável

            /* ——— long-press para excluir ——— */
            setOnLongClickListener {
                AlertDialog.Builder(this@FrotaActivity)
                    .setTitle("Excluir registro")
                    .setMessage("Remover $data · $placa ?")
                    .setPositiveButton("Excluir") { _, _ ->
                        atividadesRef.document(idDoc).delete()
                            .addOnSuccessListener { toast("Registro excluído"); carregar() }
                            .addOnFailureListener { toast("Falha ao excluir: ${it.message}") }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
                true
            }
        }
    }

    /** Mostra um diálogo de edição (campo string ou inteiro) */
    private fun abrirDialogoEdicao(
        docId: String,
        campo: String,
        valorAtual: String
    ) {
        val input = EditText(this).apply {
            setText(valorAtual)
            if (campo == "km") inputType = InputType.TYPE_CLASS_NUMBER
        }

        AlertDialog.Builder(this)
            .setTitle("Editar $campo")
            .setView(input)
            .setPositiveButton("Salvar") { _, _ ->
                val txt = input.text.toString().trim()
                val novoValor: Any = if (campo == "km") txt.toIntOrNull() ?: return@setPositiveButton
                else txt
                if (txt.isNotEmpty() && txt != valorAtual) atualizarCampo(docId, campo, novoValor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Mostra DatePicker + TimePicker, devolve Timestamp para o Firestore */
    private fun abrirDialogoData(
        docId: String,
        dataAtual: Date
    ) {
        val cal = Calendar.getInstance().apply { time = dataAtual }

        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)

            TimePickerDialog(this, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)

                val novaData = cal.time
                atualizarCampo(docId, "data", Timestamp(novaData))   // <-- muda só esse campo
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()

        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    /** Faz update no Firestore e recarrega a UI */
    private fun atualizarCampo(docId: String, campo: String, valor: Any) {
        atividadesRef.document(docId).update(campo, valor)
            .addOnSuccessListener { carregar() }
            .addOnFailureListener { toast("Erro ao salvar: ${it.message}") }
    }


    private fun applyDynamicTextSize(root: View, baseSp: Float) {
        // Fator: 0.5 × em retrato, 1 × em paisagem
        val orient = resources.configuration.orientation
        val factor = if (orient == Configuration.ORIENTATION_LANDSCAPE) 0.5f else 1f
        val targetSp = baseSp * factor

        when (root) {
            is TextView -> root.setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                FontUtils.getDynamicFontSize(root.context, targetSp)
            )
            is ViewGroup -> (0 until root.childCount).forEach {
                applyDynamicTextSize(root.getChildAt(it), baseSp)   // mantém baseSp para recálculo
            }
        }
    }

    private fun limpar() {
        fTipo = null; fPlaca = null; fIni = null; fFim = null
        carregar()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
