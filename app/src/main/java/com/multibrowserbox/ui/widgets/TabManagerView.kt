package com.multibrowserbox.ui.widgets

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class TabManagerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    
    interface TabManagerListener {
        fun onTabSelected(tabId: String)
        fun onTabClosed(tabId: String)
        fun onNewTabRequested()
        fun onIncognitoTabRequested()
    }
    
    var listener: TabManagerListener? = null
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var btnNewTab: MaterialButton
    private lateinit var btnIncognito: MaterialButton
    private lateinit var btnCloseAll: MaterialButton
    
    private val tabs = mutableListOf<TabItem>()
    private lateinit var adapter: TabAdapter
    
    init {
        setupView(context)
    }
    
    private fun setupView(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.widget_tab_manager, this, true)
        
        recyclerView = findViewById(R.id.recyclerView)
        btnNewTab = findViewById(R.id.btnNewTab)
        btnIncognito = findViewById(R.id.btnIncognito)
        btnCloseAll = findViewById(R.id.btnCloseAll)
        
        setupRecyclerView()
        setupListeners()
    }
    
    private fun setupRecyclerView() {
        adapter = TabAdapter(tabs, object : TabAdapter.TabClickListener {
            override fun onTabClick(tabId: String) {
                listener?.onTabSelected(tabId)
            }
            
            override fun onTabClose(tabId: String) {
                listener?.onTabClosed(tabId)
            }
        })
        
        recyclerView.layoutManager = GridLayoutManager(context, 2)
        recyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        btnNewTab.setOnClickListener { listener?.onNewTabRequested() }
        btnIncognito.setOnClickListener { listener?.onIncognitoTabRequested() }
        btnCloseAll.setOnClickListener { clearAllTabs() }
    }
    
    fun addTab(tabItem: TabItem) {
        tabs.add(tabItem)
        adapter.notifyItemInserted(tabs.size - 1)
    }
    
    fun removeTab(tabId: String) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index != -1) {
            tabs.removeAt(index)
            adapter.notifyItemRemoved(index)
        }
    }
    
    fun updateTab(tabId: String, title: String? = null, url: String? = null, thumbnail: Bitmap? = null) {
        val index = tabs.indexOfFirst { it.id == tabId }
        if (index != -1) {
            val tab = tabs[index]
            title?.let { tab.title = it }
            url?.let { tab.url = it }
            thumbnail?.let { tab.thumbnail = it }
            adapter.notifyItemChanged(index)
        }
    }
    
    fun clearAllTabs() {
        val size = tabs.size
        tabs.clear()
        adapter.notifyItemRangeRemoved(0, size)
    }
    
    fun getTabs(): List<TabItem> = tabs.toList()
    
    data class TabItem(
        val id: String,
        var title: String,
        var url: String,
        var thumbnail: Bitmap? = null,
        val isIncognito: Boolean = false,
        val profileId: String = "default"
    )
    
    private class TabAdapter(
        private val tabs: List<TabItem>,
        private val listener: TabClickListener
    ) : RecyclerView.Adapter<TabAdapter.TabViewHolder>() {
        
        interface TabClickListener {
            fun onTabClick(tabId: String)
            fun onTabClose(tabId: String)
        }
        
        inner class TabViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.card)
            val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
            val title: TextView = view.findViewById(R.id.title)
            val url: TextView = view.findViewById(R.id.url)
            val btnClose: ImageButton = view.findViewById(R.id.btnClose)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_tab, parent, false)
            return TabViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            val tab = tabs[position]
            
            holder.title.text = tab.title
            holder.url.text = tab.url
            tab.thumbnail?.let { holder.thumbnail.setImageBitmap(it) }
            
            if (tab.isIncognito) {
                holder.card.setCardBackgroundColor(0xFF333333.toInt())
            }
            
            holder.card.setOnClickListener {
                listener.onTabClick(tab.id)
            }
            
            holder.btnClose.setOnClickListener {
                listener.onTabClose(tab.id)
            }
        }
        
        override fun getItemCount(): Int = tabs.size
    }
}
