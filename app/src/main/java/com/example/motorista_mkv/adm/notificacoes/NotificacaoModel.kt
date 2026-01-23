package com.example.motorista_mkv.adm.notificacoes

data class NotificacaoModel (
    val chamadoId: String = "",
    val data: String = "",
    val tipo: String = "",
    val mntntInfrmd: Int = 0,
    val dfrncl: Int = 0,
    val montanteInicial: Int = 0,
    val motorista: String = ""

)
