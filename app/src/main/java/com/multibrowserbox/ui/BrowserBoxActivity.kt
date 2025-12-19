package com.multibrowserbox.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import com.multibrowserbox.core.BrowserInstance
import com.multibrowserbox.core.ProfileManager

class BrowserBoxActivity : AppCompatActivity() {
    
    private lateinit var browserInstance: BrowserInstance
    private lateinit var urlBar: EditText
    private lateinit var webViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser_box)
        
        val profileName = intent.getStringExtra("PROFILE_NAME") ?: "P1"
        val profileManager = ProfileManager(this, profileName)
        
        // Configurar toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = profileManager.getProfileDisplayName()
        toolbar.setBackgroundColor(profileManager.getProfileColor())
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        // Inicializar componentes
        urlBar = findViewById(R.id.urlBar)
        webViewContainer = findViewById(R.id.webViewContainer)
        progressBar = findViewById(R.id.progressBar)
        
        // Botões de navegação
        findViewById<MaterialButton>(R.id.btnBack).setOnClickListener {
            browserInstance.goBack()
        }
        
        findViewById<MaterialButton>(R.id.btnForward).setOnClickListener {
            browserInstance.goForward()
        }
        
        findViewById<MaterialButton>(R.id.btnRefresh).setOnClickListener {
            browserInstance.reload()
        }
        
        findViewById<MaterialButton>(R.id.btnHome).setOnClickListener {
            browserInstance.loadUrl("https://www.google.com")
        }
        
        // Inicializar BrowserInstance
        browserInstance = BrowserInstance(this, profileName)
        val webView = browserInstance.initialize(webViewContainer)
        
        // Configurar listeners do WebView
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = android.view.View.GONE
                } else {
                    progressBar.visibility = android.view.View.VISIBLE
                }
            }
            
            override fun onReceivedTitle(view: WebView?, title: String?) {
                supportActionBar?.subtitle = title
            }
        }
        
        webView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                urlBar.setText(url)
            }
        }
        
        // Configurar URL bar
        urlBar.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                browserInstance.loadUrl(v.text.toString())
                true
            } else {
                false
            }
        }
        
        // Carregar URL inicial
        browserInstance.loadUrl("https://www.google.com")
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browser_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_new_tab -> {
                Toast.makeText(this, "Nova aba (em desenvolvimento)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_history -> {
                Toast.makeText(this, "Histórico (em desenvolvimento)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_bookmarks -> {
                Toast.makeText(this, "Favoritos (em desenvolvimento)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_downloads -> {
                Toast.makeText(this, "Downloads (em desenvolvimento)", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_settings -> {
                val intent = android.content.Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        browserInstance.destroy()
    }
    
    override fun onBackPressed() {
        if (browserInstance.goBack()) {
            return
        }
        super.onBackPressed()
    }
}
