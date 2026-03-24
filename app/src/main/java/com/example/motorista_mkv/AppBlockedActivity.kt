package com.example.motorista_mkv

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class AppBlockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_blocked)

        val message = intent.getStringExtra(EXTRA_BLOCK_MESSAGE)
            ?: "Esta versão do aplicativo não é mais suportada. Entre em contato com a administração."

        findViewById<TextView>(R.id.blockedMessageTextView).text = message
        findViewById<Button>(R.id.closeAppButton).setOnClickListener {
            finishAffinity()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Bloqueia retorno para telas anteriores
            }
        })
    }

    override fun attachBaseContext(newBase: Context) {
        val overrideConfig: Configuration = Configuration(newBase.resources.configuration)
        overrideConfig.fontScale = 1.0f
        val context = newBase.createConfigurationContext(overrideConfig)
        super.attachBaseContext(context)
    }

    companion object {
        const val EXTRA_BLOCK_MESSAGE = "extra_block_message"
    }
}