package com.example.motorista_mkv.util

import android.content.Context
import com.example.motorista_mkv.R

object FontUtils {
    /**
     * Retorna o tamanho dinâmico da fonte em sp, baseado em um valor base e na largura da tela.
     *
     * @param context Contexto para acessar os recursos.
     * @param baseSp Tamanho base da fonte (em sp) definido para um dispositivo com a largura de referência.
     * @return O tamanho da fonte ajustado dinamicamente.
     */
    fun getDynamicFontSize(context: Context, baseSp: Float): Float {
        // Recupera as métricas da tela
        val displayMetrics = context.resources.displayMetrics

        // Converte a largura da tela de pixels para dp
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        // Busca o valor de referência definido no dimens.xml (em pixels)
        // Convertendo para dp dividindo pela densidade.
        val referenceWidthDp = context.resources.getDimension(R.dimen.reference_width_dp) / displayMetrics.density

        // Calcula o tamanho dinâmico proporcional
        return baseSp * (screenWidthDp / referenceWidthDp)
    }
}