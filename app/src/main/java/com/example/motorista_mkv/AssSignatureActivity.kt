package com.example.motorista_mkv

import android.content.ContentValues
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class AssSignatureActivity : AppCompatActivity() {

    private lateinit var signaturePad : SignaturePad
    //private lateinit var tvStatement  : TextView
    private lateinit var btnClear     : Button
    private lateinit var btnConfirm   : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ass_signature)

        signaturePad = findViewById(R.id.signaturePad)
        //tvStatement  = findViewById(R.id.tvStatementSig)
        btnClear     = findViewById(R.id.btnClear)
        btnConfirm   = findViewById(R.id.btnConfirm)

        /* ───── texto recebido da tela anterior ───── */
        val statement = intent.getStringExtra("statement") ?: ""
        //tvStatement.text = statement

        /* limpar assinatura */
        btnClear.setOnClickListener { signaturePad.clear() }

        /* confirmar e salvar em PDF */
        btnConfirm.setOnClickListener {
            if (signaturePad.isEmpty) {
                Toast.makeText(this, "Faça a assinatura primeiro.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            salvarPdf(signaturePad.signatureBitmap, statement)
        }
    }

    /* ------------------------------------------------------------------ */
    private fun salvarPdf(assinatura: Bitmap, texto: String) {
        val pageWidth  = 595   // A4 @72 dpi ≃ 210 mm
        val pageHeight = 842

        val doc      = PdfDocument()
        val pageInfo = PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, 1).create()

        val page   = doc.startPage(pageInfo)
        val canvas = page.canvas

        /* ---- imprime o texto (com quebra simples) ---- */
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            isAntiAlias = true
        }

        val margin   = 40f
        val maxWidth = pageWidth - margin * 2    // largura útil
        var yPos     = 60f

        texto.split(" ").let { words ->
            val sb = StringBuilder()
            for (word in words) {
                if (paint.measureText("$sb $word") > maxWidth) {
                    canvas.drawText(sb.toString(), margin, yPos, paint)
                    sb.clear()
                    yPos += 22f
                }
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(word)
            }
            if (sb.isNotEmpty()) {
                canvas.drawText(sb.toString(), margin, yPos, paint)
                yPos += 22f
            }
        }

        /* ---- desenha a assinatura centralizada ---- */
        val scale = 0.6f
        val sigW  = (assinatura.width  * scale).toInt()
        val sigH  = (assinatura.height * scale).toInt()
        val resized = Bitmap.createScaledBitmap(assinatura, sigW, sigH, true)

        val left = (pageWidth - sigW) / 2f
        val top  = yPos + 40f
        canvas.drawBitmap(resized, left, top, null)

        doc.finishPage(page)

        /* ---- salva no diretório Downloads ---- */
        try {
            val fileName = "Assinatura_${System.currentTimeMillis()}.pdf"
            val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                salvarEmDownloadsScoped(doc, fileName)
            } else {
                salvarEmDownloadsLegacy(doc, fileName)
            }
            doc.close()

            if (saved) {
                Toast.makeText(
                    this,
                    "PDF salvo em Downloads/$fileName",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } catch (e: Exception) {
            doc.close()
            Toast.makeText(this, "Erro ao salvar PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /* ---- Android 10 + (Scoped Storage) ---- */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun salvarEmDownloadsScoped(doc: PdfDocument, fileName: String): Boolean {
        val resolver = contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false

        resolver.openOutputStream(uri).use { out -> doc.writeTo(out) }

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    /* ---- Android 9 ou anterior ---- */
    private fun salvarEmDownloadsLegacy(doc: PdfDocument, fileName: String): Boolean {
        val downloads = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloads.exists()) downloads.mkdirs()

        val file = File(downloads, fileName)
        val out: OutputStream = FileOutputStream(file)
        doc.writeTo(out)
        out.close()
        return true
    }
}

