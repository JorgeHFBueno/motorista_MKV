package com.example.motorista_mkv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class AssActivity : AppCompatActivity() {

    private lateinit var tvStatement: TextView
    private lateinit var btnSign: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ass)

        tvStatement = findViewById(R.id.tvStatement)
        btnSign     = findViewById(R.id.btnSign)

        /* ───── monta a frase exibida ───── */
        val userName = FirebaseAuth.getInstance()
            .currentUser?.displayName ?: "usuário"

        val itemName = gerarItemAleatorio()
        val dataHora = SimpleDateFormat(
            "HH:mm dd/MM/yy",
            Locale("pt", "BR")
        ).format(Date())

        val statement =
            "Eu $userName, confirmo que estou pegando o item $itemName, na data: $dataHora"

        tvStatement.text = statement

        /* ───── botão “ASSINAR” ───── */
        btnSign.setOnClickListener {
            val i = Intent(this, AssSignatureActivity::class.java)
            i.putExtra("statement", statement)   // envia o texto
            startActivity(i)
        }
    }

    /* gera um dos itens predefinidos de forma aleatória */
    private fun gerarItemAleatorio(): String {
        val itens = listOf(
            "Chave 10 mm",
            "Furadeira",
            "Tablet de Serviço",
            "Cabo USB-C",
            "Controle RFID",
            "Bateria Reserva"
        )
        return itens.random()
    }
}
