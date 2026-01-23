package com.example.motorista_mkv.adm.combustivel
import com.google.firebase.Timestamp

class bombModel (
    var id: String? = null,         // Document ID do Firestore
    var data: Timestamp? = null,    // Campo "data" do tipo Timestamp
    var km: Long? = null,           // Campo "km"
    var lf: Long? = null,           // Campo "lf"
    var li: Long? = null,           // Campo "li"
    var motivo: String? = null,     // Campo "motivo"
    var para_quem: String? = null,  // Campo "para_quem"
    var placa: String? = null,      // Campo "placa"
    var qa: Long? = null,
    var arla: Long? = null,
    var diesel: Long? = null,
    var motorista: String? = null,
    var observacao: String? = null,
    var semKm: String? = null, // Novo campo para indicar "Galão" ou "Sem Odômetro"
    var local: String? = null,
    var tipoPlaca: Boolean = true // true = "placa", false = "extra"
)