package com.example.motorista_mkv.adm.notificacoes

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.motorista_mkv.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.motorista_mkv.R
import com.example.motorista_mkv.adm.notificacoes.DetalheChamadoActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "Mensagem recebida de: ${remoteMessage.from}")

        // Obtém os dados do payload de notificação (título e mensagem)
        val titulo = remoteMessage.notification?.title ?: "Título padrão"
        val mensagem = remoteMessage.notification?.body ?: "Mensagem padrão"

        // Extrai apenas os dados essenciais
        val tipo = remoteMessage.data["tipo"] ?: "default"
        val chamadoId = remoteMessage.data["chamadoId"] ?: ""

        // Chama a função para abrir a Activity correta
        abrirActivityEspecifica(tipo, titulo, mensagem, chamadoId)
    }


    private fun abrirActivityEspecifica(
        tipo: String,
        titulo: String,
        mensagem: String,
        chamadoId: String
    ) {
        Log.d("FCM", "Abrindo Activity para tipo: $tipo, chamadoId: $chamadoId")

        val intent = when (tipo) {
            "Conflito no Montante" -> Intent(this, ConflitoMontanteActivity::class.java)
            else -> Intent(this, MainActivity::class.java)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("CHAMADO_ID", chamadoId)
        intent.putExtra("TITULO", titulo)
        intent.putExtra("MENSAGEM", mensagem)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "notificacoes_chamados"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Notificações de Chamados",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo_vetor)
            .setContentTitle(titulo)
            .setContentText(mensagem)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

}
