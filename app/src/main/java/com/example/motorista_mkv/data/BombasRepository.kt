package com.example.motorista_mkv.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Date

object BombasRepository {
    private const val TAG = "BombasRepository"
    private const val COLLECTION = "bombas"
    private const val DOC_DIESEL_PATIO = "diesel_patio"

    fun fetchMontanteFolga(
        firestore: FirebaseFirestore,
        onSuccess: (montanteAtual: Long, folgaLitros: Long) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .document(DOC_DIESEL_PATIO)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w(TAG, "Documento bombas/diesel_patio não encontrado.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                val montante = document.getLong("montanteAtual")
                val folga = document.getLong("folgaLitros")

                if (montante == null || folga == null) {
                    Log.w(TAG, "Campos montanteAtual/folgaLitros ausentes em bombas/diesel_patio.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                onSuccess(montante, folga)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao ler bombas/diesel_patio.", exception)
                onFailure(exception)
            }
    }

    fun fetchMontanteAtual(
        firestore: FirebaseFirestore,
        onSuccess: (montanteAtual: Long) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .document(DOC_DIESEL_PATIO)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w(TAG, "Documento bombas/diesel_patio não encontrado.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                val montante = document.getLong("montanteAtual")
                if (montante == null) {
                    Log.w(TAG, "Campo montanteAtual ausente em bombas/diesel_patio.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                onSuccess(montante)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao ler bombas/diesel_patio.", exception)
                onFailure(exception)
            }
    }

    fun fetchMontanteAtualComUltimoAbastecimento(
        firestore: FirebaseFirestore,
        onSuccess: (montanteAtual: Long, ultimoAbastecimento: Date?) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .document(DOC_DIESEL_PATIO)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w(TAG, "Documento bombas/diesel_patio não encontrado.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                val montante = document.getLong("montanteAtual")
                if (montante == null) {
                    Log.w(TAG, "Campo montanteAtual ausente em bombas/diesel_patio.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                val ultimoAbastecimento = document.getTimestamp("ultimoAbastecimento")?.toDate()
                onSuccess(montante, ultimoAbastecimento)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao ler bombas/diesel_patio.", exception)
                onFailure(exception)
            }
    }

    fun fetchEstoqueAtual(
        firestore: FirebaseFirestore,
        onSuccess: (estoqueAtual: Long) -> Unit,
        onFailure: (Exception?) -> Unit
    ) {
        firestore.collection(COLLECTION)
            .document(DOC_DIESEL_PATIO)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    Log.w(TAG, "Documento bombas/diesel_patio não encontrado.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                val estoque = document.getLong("estoqueAtual")
                if (estoque == null) {
                    Log.w(TAG, "Campo estoqueAtual ausente em bombas/diesel_patio.")
                    onFailure(null)
                    return@addOnSuccessListener
                }

                onSuccess(estoque)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao ler bombas/diesel_patio.", exception)
                onFailure(exception)
            }
    }

    fun updateDieselPatio(
        firestore: FirebaseFirestore,
        montanteAtual: Int,
        estoqueAtual: Int,
        ultimoFrentista: String,
        ultimoAbastecimento: Date,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val data = mapOf(
            "montanteAtual" to montanteAtual,
            "estoqueAtual" to estoqueAtual,
            "ultimoFrentista" to ultimoFrentista,
            "ultimoAbastecimento" to ultimoAbastecimento
        )

        firestore.collection(COLLECTION)
            .document(DOC_DIESEL_PATIO)
            .set(data, SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Erro ao atualizar bombas/diesel_patio.", exception)
                onFailure(exception)
            }
    }
}