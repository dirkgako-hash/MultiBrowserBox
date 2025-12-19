package com.multibrowserbox.core

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.*
import android.widget.Toast
import androidx.webkit.*

class BrowserInstance(
    private val context: Context,
    val profileId: String,
    private val extensionEngine: ExtensionEngine? = null
) : WebViewClientCompat() {

    lateinit var webView: WebView
    private lateinit var profileManager: ProfileManager
    private var safeBrowsingHelper: SafeBrowsingHelper? = null

    @SuppressLint("SetJavaScriptEnabled")
    fun initialize(container: android.view.ViewGroup): WebView {
        profileManager = ProfileManager(context, profileId)
        
        // Criar WebView com configurações avançadas
        webView = WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )

            // Configurações como navegador real
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mediaPlaybackRequiresUserGesture = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportMultipleWindows(true)
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
                blockNetworkImage = false
                blockNetworkLoads = false
                defaultFontSize = 16
                defaultFixedFontSize = 13
                minimumFontSize = 8
                minimumLogicalFontSize = 8
            }

            // Isolamento de dados por perfil
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                WebView.setDataDirectorySuffix(profileId)
            }

            webViewClient = this@BrowserInstance
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest) {
                    request.grant(request.resources)
                }
                
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    android.util.Log.d("Browser-$profileId", 
                        "${consoleMessage.sourceId()}:${consoleMessage.lineNumber()} ${consoleMessage.message()}")
                    return true
                }
            }
            
            // Configurar cookie manager isolado
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }

        // Inicializar navegação segura
        safeBrowsingHelper = SafeBrowsingHelper(context)
        safeBrowsingHelper?.initializeSafeBrowsing()

        container.addView(webView)
        return webView
    }

    fun loadUrl(url: String) {
        webView.loadUrl(if (url.startsWith("http")) url else "https://$url")
    }

    fun goBack() = webView.canGoBack().let { if (it) webView.goBack() }
    fun goForward() = webView.canGoForward().let { if (it) webView.goForward() }
    fun reload() = webView.reload()
    fun stopLoading() = webView.stopLoading()

    override fun shouldInterceptRequest(
        view: WebView?,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // Interceptar requisições para extensões
        extensionEngine?.executeContentScript(request.url.toString(), webView)
        return null
    }

    override fun onRenderProcessGone(
        view: WebView?,
        detail: RenderProcessGoneDetail?
    ): Boolean {
        android.util.Log.e("BrowserInstance", "Renderer process gone for profile $profileId")
        // Recriar WebView se necessário
        return detail?.didCrash() == false
    }

    override fun onSafeBrowsingHit(
        view: WebView?,
        request: WebResourceRequest?,
        threatType: Int,
        callback: SafeBrowsingResponseCompat
    ) {
        callback.backToSafety(true)
        android.widget.Toast.makeText(
            context, 
            "Site bloqueado por segurança (${threatType})", 
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    fun destroy() {
        webView.stopLoading()
        webView.destroy()
    }
}
