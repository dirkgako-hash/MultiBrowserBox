package com.multibrowserbox.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.button.MaterialButton

class BrowserToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    interface ToolbarListener {
        fun onBackClicked()
        fun onForwardClicked()
        fun onRefreshClicked()
        fun onHomeClicked()
        fun onUrlSubmitted(url: String)
        fun onMenuClicked()
        fun onTabSwitched(tabId: String)
    }
    
    var listener: ToolbarListener? = null
    
    private lateinit var btnBack: MaterialButton
    private lateinit var btnForward: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnHome: MaterialButton
    private lateinit var urlBar: EditText
    private lateinit var btnMenu: MaterialButton
    private lateinit var tabContainer: LinearLayout
    
    init {
        setupView(context)
    }
    
    private fun setupView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.widget_browser_toolbar, this, true)
        
        btnBack = findViewById(R.id.btnBack)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnHome = findViewById(R.id.btnHome)
        urlBar = findViewById(R.id.urlBar)
        btnMenu = findViewById(R.id.btnMenu)
        tabContainer = findViewById(R.id.tabContainer)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener { listener?.onBackClicked() }
        btnForward.setOnClickListener { listener?.onForwardClicked() }
        btnRefresh.setOnClickListener { listener?.onRefreshClicked() }
        btnHome.setOnClickListener { listener?.onHomeClicked() }
        btnMenu.setOnClickListener { listener?.onMenuClicked() }
        
        urlBar.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO) {
                listener?.onUrlSubmitted(v.text.toString())
                true
            } else {
                false
            }
        }
    }
    
    fun setUrl(url: String) {
        urlBar.setText(url)
    }
    
    fun setNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        btnBack.isEnabled = canGoBack
        btnForward.isEnabled = canGoForward
        
        btnBack.alpha = if (canGoBack) 1.0f else 0.5f
        btnForward.alpha = if (canGoForward) 1.0f else 0.5f
    }
    
    fun addTab(tabId: String, title: String) {
        val tabButton = MaterialButton(context).apply {
            text = title
            setOnClickListener { listener?.onTabSwitched(tabId) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 8
            }
        }
        tabContainer.addView(tabButton)
    }
    
    fun removeTab(tabId: String) {
        for (i in 0 until tabContainer.childCount) {
            val view = tabContainer.getChildAt(i)
            if (view is MaterialButton && view.tag == tabId) {
                tabContainer.removeView(view)
                break
            }
        }
    }
    
    fun clearTabs() {
        tabContainer.removeAllViews()
    }
    
    fun showProgress(progress: Int) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = progress
        progressBar.visibility = if (progress in 1..99) {
            android.view.View.VISIBLE
        } else {
            android.view.View.GONE
        }
    }
}
