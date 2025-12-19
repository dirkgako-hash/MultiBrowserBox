package com.multibrowserbox.utils

import android.content.Context
import android.webkit.SafeBrowsingResponse
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.webkit.SafeBrowsingResponseCompat
import androidx.webkit.WebViewClientCompat
import androidx.webkit.WebViewCompat

class SafeBrowsingHelper(private val context: Context) {
    
    companion object {
        private const val SAFE_BROWSING_URL = "https://safebrowsing.google.com/safebrowsing"
    }
    
    fun initializeSafeBrowsing() {
        try {
            WebViewCompat.setSafeBrowsingEnabled(context, true)
            
            WebViewCompat.startSafeBrowsing(context) { success ->
                android.util.Log.d("SafeBrowsing", 
                    "Safe Browsing inicializado: ${if (success) "SUCESSO" else "FALHA"}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SafeBrowsing", "Erro na inicialização: ${e.message}")
        }
    }
    
    fun checkUrl(url: String, callback: (Boolean) -> Unit) {
        // Verificação local simples
        val unsafePatterns = listOf(
            "phishing", "malware", "trojan", "virus", "exploit",
            ".exe", ".bat", ".cmd", "javascript:alert"
        )
        
        val isSafe = unsafePatterns.none { url.contains(it, ignoreCase = true) }
        callback(isSafe)
    }
    
    fun getThreatTypeDescription(threatType: Int): String {
        return when (threatType) {
            WebViewClientCompat.SAFE_BROWSING_THREAT_MALWARE -> "MALWARE"
            WebViewClientCompat.SAFE_BROWSING_THREAT_PHISHING -> "PHISHING"
            WebViewClientCompat.SAFE_BROWSING_THREAT_UNWANTED_SOFTWARE -> "UNWANTED_SOFTWARE"
            else -> "UNKNOWN_THREAT"
        }
    }
    
    fun handleSafeBrowsingHit(
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponseCompat
    ) {
        // Registrar o incidente
        android.util.Log.w("SafeBrowsing", 
            "Ameaça detectada: ${getThreatTypeDescription(threatType)} em ${request.url}")
        
        // Mostrar diálogo de segurança
        showSecurityWarning(request.url.toString(), threatType) { userChoice ->
            when (userChoice) {
                SecurityChoice.BACK_TO_SAFETY -> callback.backToSafety(true)
                SecurityChoice.PROCEED -> callback.proceed(true)
                SecurityChoice.SHOW_INTERSTITIAL -> callback.showInterstitial(true)
            }
        }
    }
    
    private fun showSecurityWarning(
        url: String, 
        threatType: Int, 
        callback: (SecurityChoice) -> Unit
    ) {
        // Em produção, mostrar um diálogo real
        android.util.Log.w("SafeBrowsing", 
            "AVISO DE SEGURANÇA: $url - ${getThreatTypeDescription(threatType)}")
        
        // Por simplicidade, sempre voltar à segurança
        callback(SecurityChoice.BACK_TO_SAFETY)
    }
    
    enum class SecurityChoice {
        BACK_TO_SAFETY, PROCEED, SHOW_INTERSTITIAL
    }
    
    fun getSafeBrowsingStatus(): String {
        return try {
            val isEnabled = WebViewCompat.isSafeBrowsingEnabled(context)
            "Safe Browsing: ${if (isEnabled) "ATIVADO" else "DESATIVADO"}"
        } catch (e: Exception) {
            "Safe Browsing: INDISPONÍVEL"
        }
    }
    
    fun reportSafeBrowsingHit(url: String, threatType: String) {
        // Reportar à API do Google Safe Browsing
        android.util.Log.i("SafeBrowsing", 
            "Reportando ameaça: $threatType em $url")
        
        // Em produção, enviar para a API real
        // https://developers.google.com/safe-browsing/v4/reporting
    }
}
