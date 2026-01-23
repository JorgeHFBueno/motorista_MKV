package com.example.motorista_mkv.motorista

import com.google.firebase.Timestamp

data class HistoryModel(
    var id: String = "",
    var collectionType: String = "", // "Combustível", "Saída" ou "Chegada"
    var data: Timestamp? = null,
    var placa: String = ""
)