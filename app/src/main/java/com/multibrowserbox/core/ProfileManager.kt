package com.multibrowserbox.core

import android.content.Context
import java.io.File

class ProfileManager(private val context: Context, val profileId: String) {
    
    companion object {
        const val PROFILE_PREFIX = "profile_"
    }
    
    val profileDir: File by lazy {
        File(context.filesDir, "profiles/${PROFILE_PREFIX}$profileId").apply { 
            mkdirs() 
        }
    }
    
    val cacheDir: File by lazy { 
        File(profileDir, "cache").apply { mkdirs() }
    }
    
    val cookieDir: File by lazy { 
        File(profileDir, "cookies").apply { mkdirs() }
    }
    
    val extensionsDir: File by lazy { 
        File(profileDir, "extensions").apply { mkdirs() }
    }
    
    val localStorageDir: File by lazy { 
        File(profileDir, "local_storage").apply { mkdirs() }
    }
    
    val indexedDBDir: File by lazy { 
        File(profileDir, "indexeddb").apply { mkdirs() }
    }
    
    val webStorageDir: File by lazy { 
        File(profileDir, "webstorage").apply { mkdirs() }
    }
    
    val historyFile: File by lazy { 
        File(profileDir, "history.db")
    }
    
    val bookmarksFile: File by lazy { 
        File(profileDir, "bookmarks.json")
    }
    
    fun getProfileDisplayName(): String {
        return when (profileId) {
            "P1" -> "Perfil 1 (Trabalho)"
            "P2" -> "Perfil 2 (Pessoal)"
            "P3" -> "Perfil 3 (Social)"
            "P4" -> "Perfil 4 (Anônimo)"
            else -> "Perfil $profileId"
        }
    }
    
    fun getProfileColor(): Int {
        return when (profileId) {
            "P1" -> android.graphics.Color.parseColor("#4CAF50")  // Verde
            "P2" -> android.graphics.Color.parseColor("#2196F3")  // Azul
            "P3" -> android.graphics.Color.parseColor("#FF9800")  // Laranja
            "P4" -> android.graphics.Color.parseColor("#9C27B0")  // Roxo
            else -> android.graphics.Color.parseColor("#607D8B")  // Cinza
        }
    }
    
    fun clearCache() {
        cacheDir.deleteRecursively()
        cacheDir.mkdirs()
    }
    
    fun clearCookies() {
        cookieDir.deleteRecursively()
        cookieDir.mkdirs()
    }
    
    fun clearAllData() {
        arrayOf(cacheDir, cookieDir, localStorageDir, indexedDBDir, webStorageDir)
            .forEach { it.deleteRecursively() }
        
        historyFile.delete()
        bookmarksFile.delete()
        
        // Recreate directories
        arrayOf(cacheDir, cookieDir, localStorageDir, indexedDBDir, webStorageDir)
            .forEach { it.mkdirs() }
    }
    
    fun getStorageStats(): Map<String, Long> {
        return mapOf(
            "cache" to cacheDir.walk().filter { it.isFile }.sumOf { it.length() },
            "cookies" to cookieDir.walk().filter { it.isFile }.sumOf { it.length() },
            "localStorage" to localStorageDir.walk().filter { it.isFile }.sumOf { it.length() },
            "extensions" to extensionsDir.walk().filter { it.isFile }.sumOf { it.length() }
        )
    }
}
