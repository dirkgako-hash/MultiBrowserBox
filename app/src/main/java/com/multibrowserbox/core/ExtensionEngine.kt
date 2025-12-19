package com.multibrowserbox.core

import android.content.Context
import android.webkit.WebView
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

data class Extension(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val permissions: List<String>,
    val contentScripts: List<ContentScript>,
    val backgroundScript: String? = null,
    val popupHtml: String? = null
)

data class ContentScript(
    val matches: List<String>,
    val js: List<String>,
    val css: List<String> = emptyList()
)

class ExtensionEngine(private val context: Context, private val profileManager: ProfileManager) {
    
    private val loadedExtensions = mutableMapOf<String, Extension>()
    private val contentScriptRegistry = mutableMapOf<String, MutableList<ContentScript>>()
    
    fun loadExtension(crxFile: File): Boolean {
        return try {
            // Descompactar arquivo CRX
            val extensionDir = File(profileManager.extensionsDir, crxFile.nameWithoutExtension)
            extensionDir.mkdirs()
            
            // Ler manifest.json
            val manifestFile = File(extensionDir, "manifest.json")
            if (!manifestFile.exists()) {
                // Tentar extrair do CRX
                extractCrx(crxFile, extensionDir)
            }
            
            val manifest = JSONObject(manifestFile.readText())
            val extension = parseManifest(manifest, extensionDir)
            
            // Registrar extensão
            loadedExtensions[extension.id] = extension
            
            // Registrar content scripts
            extension.contentScripts.forEach { script ->
                script.matches.forEach { pattern ->
                    val scripts = contentScriptRegistry.getOrPut(pattern) { mutableListOf() }
                    scripts.add(script)
                }
            }
            
            // Executar script de background se existir
            extension.backgroundScript?.let { scriptPath ->
                executeBackgroundScript(File(extensionDir, scriptPath))
            }
            
            true
        } catch (e: Exception) {
            android.util.Log.e("ExtensionEngine", "Falha ao carregar extensão: ${e.message}")
            false
        }
    }
    
    private fun extractCrx(crxFile: File, targetDir: File) {
        FileInputStream(crxFile).use { fis ->
            // Pular cabeçalho CRX (magic number + version)
            fis.skip(16)
            ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outputFile = File(targetDir, entry.name)
                        outputFile.parentFile?.mkdirs()
                        outputFile.outputStream().use { os ->
                            zis.copyTo(os)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }
    
    private fun parseManifest(manifest: JSONObject, extensionDir: File): Extension {
        return Extension(
            id = manifest.optString("key", manifest.getString("name").hashCode().toString()),
            name = manifest.getString("name"),
            version = manifest.getString("version"),
            description = manifest.optString("description", ""),
            permissions = manifest.optJSONArray("permissions")?.let { permissions ->
                (0 until permissions.length()).map { permissions.getString(it) }
            } ?: emptyList(),
            contentScripts = parseContentScripts(manifest, extensionDir),
            backgroundScript = manifest.optJSONObject("background")?.optString("scripts")?.split(",")?.firstOrNull(),
            popupHtml = manifest.optJSONObject("browser_action")?.optString("default_popup")
        )
    }
    
    private fun parseContentScripts(manifest: JSONObject, extensionDir: File): List<ContentScript> {
        val scripts = mutableListOf<ContentScript>()
        
        manifest.optJSONArray("content_scripts")?.let { contentScripts ->
            for (i in 0 until contentScripts.length()) {
                val script = contentScripts.getJSONObject(i)
                scripts.add(
                    ContentScript(
                        matches = script.getJSONArray("matches").let { matches ->
                            (0 until matches.length()).map { matches.getString(it) }
                        },
                        js = script.getJSONArray("js").let { jsFiles ->
                            (0 until jsFiles.length()).map { 
                                File(extensionDir, jsFiles.getString(it)).readText() 
                            }
                        },
                        css = script.optJSONArray("css")?.let { cssFiles ->
                            (0 until cssFiles.length()).map { 
                                File(extensionDir, cssFiles.getString(it)).readText() 
                            }
                        } ?: emptyList()
                    )
                )
            }
        }
        
        return scripts
    }
    
    fun executeContentScripts(url: String, webView: WebView) {
        contentScriptRegistry.forEach { (pattern, scripts) ->
            if (url.matches(pattern.toRegex())) {
                scripts.forEach { script ->
                    // Injeta JavaScript
                    script.js.forEach { jsCode ->
                        webView.evaluateJavascript(jsCode, null)
                    }
                    
                    // Injeta CSS
                    if (script.css.isNotEmpty()) {
                        val cssCode = script.css.joinToString("\n")
                        val injectCss = """
                            (function() {
                                var style = document.createElement('style');
                                style.textContent = `$cssCode`;
                                document.head.appendChild(style);
                            })();
                        """.trimIndent()
                        webView.evaluateJavascript(injectCss, null)
                    }
                }
            }
        }
    }
    
    private fun executeBackgroundScript(scriptFile: File) {
        // Executar em thread separada
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val jsCode = scriptFile.readText()
                // Aqui você precisaria de um motor JavaScript como Rhino ou J2V8
                // Por enquanto apenas log
                android.util.Log.d("ExtensionEngine", "Background script carregado: ${scriptFile.name}")
            } catch (e: Exception) {
                android.util.Log.e("ExtensionEngine", "Erro no background script: ${e.message}")
            }
        }
    }
    
    fun getInstalledExtensions(): List<Extension> = loadedExtensions.values.toList()
    
    fun uninstallExtension(extensionId: String) {
        loadedExtensions.remove(extensionId)
        // Remover do registro de content scripts
        contentScriptRegistry.values.forEach { scripts ->
            scripts.removeAll { script ->
                loadedExtensions.values.none { it.contentScripts.contains(script) }
            }
        }
    }
}
