package com.example.pythonspike

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private lateinit var historyList: ListView
    private lateinit var emptyText: TextView
    private lateinit var clearButton: Button
    private lateinit var refreshButton: Button
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyList = findViewById(R.id.historyList)
        emptyText = findViewById(R.id.historyEmptyText)
        clearButton = findViewById(R.id.historyClearButton)
        refreshButton = findViewById(R.id.historyRefreshButton)

        adapter = HistoryAdapter(layoutInflater)
        historyList.adapter = adapter
        historyList.emptyView = emptyText

        refreshButton.setOnClickListener {
            loadHistory()
        }

        clearButton.setOnClickListener {
            confirmClearHistory()
        }

        historyList.setOnItemClickListener { _, _, position, _ ->
            val item = adapter.getItem(position)
            showHistoryDetail(item)
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val items = runCatching {
            ExecutionHistoryRepository.listRecent(
                context = applicationContext,
                limit = ExecutionHistoryRepository.DEFAULT_MAX_ITEMS
            )
        }.onFailure {
            AppLog.w("HistoryActivity", "load history failed", it)
            Toast.makeText(this, getString(R.string.history_load_failed), Toast.LENGTH_SHORT).show()
        }.getOrDefault(emptyList())

        adapter.submitList(items)
    }

    private fun confirmClearHistory() {
        AlertDialog.Builder(this)
            .setTitle(R.string.history_clear_title)
            .setMessage(R.string.history_clear_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.history_clear_confirm) { _, _ ->
                ExecutionHistoryRepository.clearAll(applicationContext)
                loadHistory()
                Toast.makeText(this, getString(R.string.history_clear_done), Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showHistoryDetail(item: ExecutionHistoryItem) {
        val timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", item.createdAtEpochMs).toString()
        val detail = buildString {
            appendLine(getString(R.string.history_detail_time, timestamp))
            appendLine(getString(R.string.history_detail_status, item.status, item.errorCode ?: "-"))
            appendLine(getString(R.string.history_detail_duration, item.durationMs))
            appendLine(getString(R.string.history_detail_source, item.source))
            appendLine("")
            appendLine(getString(R.string.history_detail_input))
            appendLine(if (item.inputPreview.isBlank()) "-" else item.inputPreview)
            appendLine("")
            appendLine(getString(R.string.history_detail_message))
            append(if (item.message.isBlank()) "-" else item.message)
        }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_detail_title, item.requestId))
            .setMessage(detail)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private class HistoryAdapter(
        private val inflater: LayoutInflater
    ) : BaseAdapter() {
        private val items: MutableList<ExecutionHistoryItem> = mutableListOf()

        fun submitList(newItems: List<ExecutionHistoryItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size

        override fun getItem(position: Int): ExecutionHistoryItem = items[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.item_history_record, parent, false)
            val titleText = view.findViewById<TextView>(R.id.historyItemTitle)
            val metaText = view.findViewById<TextView>(R.id.historyItemMeta)
            val detailText = view.findViewById<TextView>(R.id.historyItemDetail)

            val item = getItem(position)
            val context = view.context
            val timestamp = DateFormat.format("yyyy-MM-dd HH:mm:ss", item.createdAtEpochMs).toString()
            val statusUpper = item.status.uppercase(Locale.US)
            titleText.text = context.getString(R.string.history_item_title_format, timestamp, statusUpper)
            metaText.text = context.getString(
                R.string.history_item_meta_format,
                item.durationMs,
                item.errorCode ?: "-"
            )
            detailText.text = if (item.inputPreview.isBlank()) {
                context.getString(R.string.history_item_no_input)
            } else {
                item.inputPreview
            }

            val statusColor = when (item.status) {
                "success" -> 0xFF0E7A0D.toInt()
                "cancelled" -> 0xFF8A6D00.toInt()
                else -> 0xFFB00020.toInt()
            }
            titleText.setTextColor(statusColor)

            return view
        }
    }
}
