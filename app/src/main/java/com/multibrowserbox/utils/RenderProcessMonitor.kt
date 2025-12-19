package com.multibrowserbox.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.webkit.WebViewClientCompat
import java.util.concurrent.atomic.AtomicInteger

class RenderProcessMonitor(private val context: Context) {
    
    data class CrashReport(
        val timestamp: Long = System.currentTimeMillis(),
        val profileId: String,
        val url: String?,
        val memoryUsage: Long,
        val crashCount: Int,
        val didCrash: Boolean,
        val description: String
    )
    
    private val crashReports = mutableListOf<CrashReport>()
    private val crashCount = AtomicInteger(0)
    private val handler = Handler(Looper.getMainLooper())
    
    fun monitorWebView(webView: WebView, profileId: String) {
        // Configurar monitoramento de memória
        val memoryMonitor = Runnable {
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
                .getMemoryInfo(memoryInfo)
            
            val threshold = memoryInfo.totalMem * 0.1 // 10% da memória total
            
            if (memoryInfo.availMem < threshold) {
                android.util.Log.w("RenderProcessMonitor", 
                    "Memória baixa para perfil $profileId: ${memoryInfo.availMem / (1024*1024)}MB")
                
                // Liberar memória se necessário
                webView.clearCache(true)
            }
            
            // Agendar próximo monitoramento
            handler.postDelayed(this, 5000) // 5 segundos
        }
        
        handler.post(memoryMonitor)
    }
    
    fun onRenderProcessGone(
        webView: WebView,
        profileId: String,
        detail: RenderProcessGoneDetail
    ): Boolean {
        val crashNumber = crashCount.incrementAndGet()
        
        val report = CrashReport(
            profileId = profileId,
            url = webView.url,
            memoryUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
            crashCount = crashNumber,
            didCrash = detail.didCrash(),
            description = if (detail.didCrash()) {
                "Processo de renderização travou"
            } else {
                "Processo de renderização foi morto (falta de memória)"
            }
        )
        
        crashReports.add(report)
        
        android.util.Log.e("RenderProcessMonitor", 
            "Crash #$crashNumber para $profileId: ${report.description}")
        
        // Tomar ação apropriada
        return if (!detail.didCrash()) {
            // Recriar WebView se foi morto por falta de memória
            true
        } else {
            // Em caso de crash real, não recriar automaticamente
            false
        }
    }
    
    fun getCrashStats(): Map<String, Any> {
        return mapOf(
            "totalCrashes" to crashCount.get(),
            "recentCrashes" to crashReports.takeLast(10).size,
            "byProfile" to crashReports.groupBy { it.profileId }
                .mapValues { it.value.size },
            "lastCrash" to crashReports.lastOrNull()?.timestamp ?: 0
        )
    }
    
    fun clearCrashHistory() {
        crashReports.clear()
        crashCount.set(0)
    }
    
    fun getMemoryStats(): String {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        
        return "Memória: ${usedMem}MB / ${maxMem}MB (${(usedMem.toDouble() / maxMem * 100).toInt()}%)"
    }
    
    fun forceGarbageCollection() {
        System.gc()
        System.runFinalization()
        android.util.Log.d("RenderProcessMonitor", "Coleta de lixo forçada")
    }
    
    fun shouldRestartWebView(): Boolean {
        val recentCrashes = crashReports.count { 
            System.currentTimeMillis() - it.timestamp < 30000 // 30 segundos
        }
        return recentCrashes < 3 // Menos de 3 crashes recentes
    }
}
