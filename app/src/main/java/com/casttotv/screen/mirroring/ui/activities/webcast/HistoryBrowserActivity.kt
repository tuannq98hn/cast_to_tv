package com.casttotv.screen.mirroring.ui.activities.webcast

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.casttotv.screen.mirroring.base.BaseActivity
import com.casttotv.screen.mirroring.data.BrowserHistory
import com.casttotv.screen.mirroring.data.HistoryBrowser
import com.casttotv.screen.mirroring.databinding.ActivityHistoryBrowserBinding
import com.casttotv.screen.mirroring.databinding.ItemHistoryBinding
import kotlinx.coroutines.launch

/** Port of `HistoryBrowserActivity`: full browsing history, newest first; a tap returns the URL to the browser. */
class HistoryBrowserActivity : BaseActivity() {

    private lateinit var binding: ActivityHistoryBrowserBinding
    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.mainAdsNative.isVisible = false
        binding.rcvHistory.layoutManager = LinearLayoutManager(this)
        binding.rcvHistory.adapter = adapter
        binding.llBack.setOnClickListener { finish() }
        binding.llDeleteAll.setOnClickListener { BrowserHistory.clear() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                BrowserHistory.items.collect {
                    binding.imvNoFile.isVisible = it.isEmpty()
                    adapter.submit(it.reversed())
                }
            }
        }
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.Holder>() {
        inner class Holder(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

        private var items: List<HistoryBrowser> = emptyList()

        @SuppressLint("NotifyDataSetChanged")
        fun submit(list: List<HistoryBrowser>) {
            items = list
            notifyDataSetChanged()
        }

        override fun getItemCount() = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            Holder(ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

        override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
            val item = items[position]
            tvTitle.text = item.title
            tvLink.text = item.linkUrl
            imvThumb.setImageResource(BrowserHistory.iconOf(item.linkUrl))
            imvRemove.setOnClickListener { BrowserHistory.remove(item) }
            root.setOnClickListener {
                setResult(RESULT_OK, Intent().putExtra(EXTRA_RESULT, item.linkUrl))
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_RESULT = "result"
    }
}
