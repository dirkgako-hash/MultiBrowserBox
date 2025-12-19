package com.multibrowserbox.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton
import com.multibrowserbox.core.BrowserInstance
import com.multibrowserbox.core.ProfileManager

class MainActivity : AppCompatActivity() {
    
    private lateinit var gridContainer: GridLayout
    private lateinit var profileSelector: LinearLayout
    private lateinit var statusTextView: TextView
    
    private val selectedProfiles = mutableSetOf<String>()
    private val browserInstances = mutableMapOf<String, BrowserInstance>()
    
    // Configurações de layout automático
    private val layoutConfigs = mapOf(
        setOf("P1") to Pair(1, 1),
        setOf("P1", "P2") to Pair(1, 2),
        setOf("P1", "P2", "P3") to Pair(1, 3),
        setOf("P1", "P2", "P3", "P4") to Pair(2, 2),
        setOf("P1", "P2", "P3", "P4", "P5") to Pair(2, 3),
        setOf("P1", "P2", "P3", "P4", "P5", "P6") to Pair(2, 3)
    )
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        gridContainer = findViewById(R.id.gridContainer)
        profileSelector = findViewById(R.id.profileSelector)
        statusTextView = findViewById(R.id.statusTextView)
        
        setupProfileButtons()
        setupControlButtons()
        updateStatus()
    }
    
    private fun setupProfileButtons() {
        val profiles = listOf("P1", "P2", "P3", "P4", "P5", "P6")
        
        profileSelector.removeAllViews()
        profiles.forEach { profile ->
            val button = MaterialButton(this).apply {
                text = profile
                setOnClickListener { toggleProfile(profile) }
                setBackgroundColor(
                    if (selectedProfiles.contains(profile)) 
                        ProfileManager(this@MainActivity, profile).getProfileColor()
                    else 
                        0xFF9E9E9E.toInt()
                )
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = 16
                }
            }
            profileSelector.addView(button)
        }
    }
    
    private fun toggleProfile(profile: String) {
        if (selectedProfiles.contains(profile)) {
            selectedProfiles.remove(profile)
            browserInstances[profile]?.destroy()
            browserInstances.remove(profile)
        } else {
            selectedProfiles.add(profile)
        }
        updateLayout()
        updateStatus()
    }
    
    private fun updateLayout() {
        gridContainer.removeAllViews()
        
        val (rows, cols) = determineLayout()
        gridContainer.apply {
            columnCount = cols
            rowCount = rows
        }
        
        // Criar containers para cada perfil
        selectedProfiles.sorted().forEachIndexed { index, profile ->
            createBrowserContainer(profile, index)
        }
    }
    
    private fun determineLayout(): Pair<Int, Int> {
        return layoutConfigs[selectedProfiles] ?: when (selectedProfiles.size) {
            1 -> Pair(1, 1)
            2 -> Pair(1, 2)
            3 -> Pair(1, 3)
            4 -> Pair(2, 2)
            5 -> Pair(2, 3)
            6 -> Pair(2, 3)
            else -> Pair(1, 1)
        }
    }
    
    private fun createBrowserContainer(profileName: String, index: Int) {
        val container = FrameLayout(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            setBackgroundColor(ProfileManager(this@MainActivity, profileName).getProfileColor())
            setPadding(4)
            elevation = 8f
        }
        
        // Adicionar header com nome do perfil
        val header = TextView(this).apply {
            text = ProfileManager(this@MainActivity, profileName).getProfileDisplayName()
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0x80000000.toInt())
            gravity = android.view.Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(header)
        
        // Área para o WebView
        val webViewContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = 48
            }
            id = android.view.View.generateViewId()
        }
        container.addView(webViewContainer)
        
        // Inicializar BrowserInstance
        val browserInstance = BrowserInstance(this, profileName)
        browserInstances[profileName] = browserInstance
        browserInstance.initialize(webViewContainer)
        
        // Carregar página padrão
        browserInstance.loadUrl(getDefaultUrl(profileName))
        
        gridContainer.addView(container)
    }
    
    private fun getDefaultUrl(profile: String): String {
        return when (profile) {
            "P1" -> "https://www.google.com"
            "P2" -> "https://www.github.com"
            "P3" -> "https://www.stackoverflow.com"
            "P4" -> "https://www.youtube.com"
            "P5" -> "https://www.twitter.com"
            "P6" -> "https://www.linkedin.com"
            else -> "https://www.google.com"
        }
    }
    
    private fun setupControlButtons() {
        findViewById<MaterialButton>(R.id.btnStartAll).setOnClickListener {
            browserInstances.values.forEach { browser ->
                browser.reload()
            }
            Toast.makeText(this, "Todos os navegadores recarregados", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.btnClearAll).setOnClickListener {
            selectedProfiles.clear()
            browserInstances.values.forEach { it.destroy() }
            browserInstances.clear()
            updateLayout()
            updateStatus()
            Toast.makeText(this, "Todos os navegadores fechados", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.btnSettings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        findViewById<MaterialButton>(R.id.btnFullscreen).setOnClickListener {
            if (selectedProfiles.isNotEmpty()) {
                val profile = selectedProfiles.first()
                val intent = Intent(this, BrowserBoxActivity::class.java).apply {
                    putExtra("PROFILE_NAME", profile)
                }
                startActivity(intent)
            } else {
                Toast.makeText(this, "Selecione um perfil primeiro", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun updateStatus() {
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        (getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager)
            .getMemoryInfo(memoryInfo)
        
        val availableMem = memoryInfo.availMem / (1024 * 1024)
        val totalMem = memoryInfo.totalMem / (1024 * 1024)
        
        statusTextView.text = """
            Perfis ativos: ${selectedProfiles.size}/6
            Instâncias: ${browserInstances.size}
            Memória: ${availableMem}MB / ${totalMem}MB livre
            Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})
        """.trimIndent()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        browserInstances.values.forEach { it.destroy() }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
}
