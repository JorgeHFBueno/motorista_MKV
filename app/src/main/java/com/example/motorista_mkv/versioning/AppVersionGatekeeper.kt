package com.example.motorista_mkv.versioning

import android.content.Context
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

private const val TAG = "AppVersionGatekeeper"
private const val VERSION_PREFS = "VersionGatePrefs"
private const val KEY_LAST_MIN_VERSION_CODE = "lastMinVersionCode"
private const val KEY_LAST_BLOCK_MESSAGE = "lastBlockMessage"
private const val DEFAULT_BLOCK_MESSAGE = "Esta versão do aplicativo não é mais suportada. Entre em contato com a administração."

data class VersionGateConfig(
    val minVersionCode: Long,
    val blockedMessage: String
)

object AppVersionGatekeeper {

    fun getInstalledVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    fun getInstalledVersionName(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return packageInfo.versionName ?: "desconhecida"
    }

    fun fetchVersionGateConfig(
        firestore: FirebaseFirestore,
        context: Context,
        onResult: (VersionGateConfig) -> Unit
    ) {
        firestore.collection("Versionamento")
            .document("config")
            .get()
            .addOnSuccessListener { configDoc ->
                if (configDoc.exists()) {
                    val parsed = parseConfigDocument(configDoc.data ?: emptyMap())
                    if (parsed != null) {
                        cacheConfig(context, parsed)
                        onResult(parsed)
                        return@addOnSuccessListener
                    }
                    Log.w(TAG, "Documento Versionamento/config existe, mas sem minVersionCode válido. Tentando fallback.")
                }

                fetchLatestVersionamentoDocument(firestore, context, onResult)
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao ler Versionamento/config. Usando fallback. Erro: ${error.message}", error)
                fetchLatestVersionamentoDocument(firestore, context, onResult)
            }
    }

    private fun fetchLatestVersionamentoDocument(
        firestore: FirebaseFirestore,
        context: Context,
        onResult: (VersionGateConfig) -> Unit
    ) {
        firestore.collection("Versionamento")
            .orderBy("Data", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { docs ->
                val doc = docs.documents.firstOrNull()
                if (doc != null) {
                    val parsed = parseConfigDocument(doc.data ?: emptyMap())
                    if (parsed != null) {
                        cacheConfig(context, parsed)
                        onResult(parsed)
                        return@addOnSuccessListener
                    }
                    Log.w(TAG, "Documento legado de Versionamento sem minVersionCode válido. Usando cache/local.")
                } else {
                    Log.w(TAG, "Coleção Versionamento sem documentos. Usando cache/local.")
                }

                onResult(getCachedOrDefaultConfig(context))
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Falha ao ler fallback de Versionamento. Usando cache/local. Erro: ${error.message}", error)
                onResult(getCachedOrDefaultConfig(context))
            }
    }

    private fun parseConfigDocument(data: Map<String, Any>): VersionGateConfig? {
        val minVersionCode = readMinVersionCode(data) ?: return null
        val message = readMessage(data)
        return VersionGateConfig(minVersionCode = minVersionCode, blockedMessage = message)
    }

    private fun readMinVersionCode(data: Map<String, Any>): Long? {
        val candidates = listOf("minVersionCode", "MinVersionCode", "versionCodeMinimo", "versaoCodeMinima")
        for (field in candidates) {
            val value = data[field] ?: continue
            when (value) {
                is Number -> return value.toLong()
                is String -> value.toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun readMessage(data: Map<String, Any>): String {
        val candidates = listOf("mensagemBloqueio", "MensagemBloqueio", "blockedMessage")
        for (field in candidates) {
            val value = data[field]
            if (value is String && value.isNotBlank()) {
                return value
            }
        }
        return DEFAULT_BLOCK_MESSAGE
    }

    private fun cacheConfig(context: Context, config: VersionGateConfig) {
        context.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_MIN_VERSION_CODE, config.minVersionCode)
            .putString(KEY_LAST_BLOCK_MESSAGE, config.blockedMessage)
            .apply()
    }

    private fun getCachedOrDefaultConfig(context: Context): VersionGateConfig {
        val prefs = context.getSharedPreferences(VERSION_PREFS, Context.MODE_PRIVATE)
        val minVersionCode = prefs.getLong(KEY_LAST_MIN_VERSION_CODE, 0L)
        val message = prefs.getString(KEY_LAST_BLOCK_MESSAGE, DEFAULT_BLOCK_MESSAGE)
            ?: DEFAULT_BLOCK_MESSAGE
        return VersionGateConfig(minVersionCode = minVersionCode, blockedMessage = message)
    }
}