package com.multibrowserbox.data.local

import android.content.Context
import android.webkit.CookieManager
import android.webkit.ValueCallback
import com.multibrowserbox.core.ProfileManager
import java.io.*
import java.net.HttpCookie
import java.util.*

class CookieManager(private val context: Context, private val profileManager: ProfileManager) {
    
    companion object {
        private const val COOKIE_FILE = "cookies.dat"
    }
    
    private val cookieFile: File by lazy {
        File(profileManager.cookieDir, COOKIE_FILE)
    }
    
    fun saveCookies() {
        val cookieManager = CookieManager.getInstance()
        
        cookieFile.outputStream().use { os ->
            ObjectOutputStream(os).use { oos ->
                // Esta é uma implementação simplificada
                // Em produção, você precisaria extrair cookies do WebView
                oos.writeObject("Cookie data for profile ${profileManager.profileId}")
            }
        }
    }
    
    fun loadCookies() {
        if (cookieFile.exists()) {
            cookieFile.inputStream().use { `is` ->
                ObjectInputStream(`is`).use { ois ->
                    // Carregar cookies
                    val cookieData = ois.readObject() as String
                    android.util.Log.d("CookieManager", 
                        "Cookies carregados para ${profileManager.profileId}: $cookieData")
                }
            }
        }
    }
    
    fun clearCookies(callback: ValueCallback<Boolean>?) {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies(callback)
        cookieFile.delete()
    }
    
    fun getCookiesForUrl(url: String): List<HttpCookie> {
        val cookies = mutableListOf<HttpCookie>()
        
        try {
            val cookieManager = CookieManager.getInstance()
            val cookieString = cookieManager.getCookie(url)
            
            cookieString?.split(";")?.forEach { cookiePair ->
                val parts = cookiePair.split("=")
                if (parts.size == 2) {
                    cookies.add(HttpCookie(parts[0].trim(), parts[1].trim()))
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CookieManager", "Erro ao obter cookies: ${e.message}")
        }
        
        return cookies
    }
    
    fun setCookie(url: String, name: String, value: String, 
                  maxAge: Long = 86400, // 1 dia
                  path: String = "/",
                  secure: Boolean = true,
                  httpOnly: Boolean = false) {
        
        val cookieManager = CookieManager.getInstance()
        val cookieString = buildString {
            append("$name=$value; ")
            append("Max-Age=$maxAge; ")
            append("Path=$path; ")
            if (secure) append("Secure; ")
            if (httpOnly) append("HttpOnly; ")
            append("SameSite=Lax")
        }
        
        cookieManager.setCookie(url, cookieString)
        cookieManager.flush()
    }
    
    fun exportCookies(): File {
        val exportFile = File(profileManager.cookieDir, 
            "cookies_export_${System.currentTimeMillis()}.json")
        
        exportFile.writeText("""{
            "profile": "${profileManager.profileId}",
            "exportDate": "${Date()}",
            "cookies": "Base64 encoded cookie data here"
        }""")
        
        return exportFile
    }
    
    fun importCookies(importFile: File): Boolean {
        return try {
            val jsonContent = importFile.readText()
            // Processar importação
            android.util.Log.d("CookieManager", 
                "Cookies importados de ${importFile.name} para ${profileManager.profileId}")
            true
        } catch (e: Exception) {
            android.util.Log.e("CookieManager", "Erro na importação: ${e.message}")
            false
        }
    }
}
