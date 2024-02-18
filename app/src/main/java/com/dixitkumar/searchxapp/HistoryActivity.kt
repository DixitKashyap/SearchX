package com.dixitkumar.searchxapp

import android.app.ProgressDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dixitkumar.searchxapp.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyBinding: ActivityHistoryBinding

        lateinit var historyList : ArrayList<History>
        lateinit var mainActivityRef : MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        historyBinding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(historyBinding.root)


        //Adding Clear Browsing data Listener
        historyBinding.clearHistory.setOnClickListener {
            //Clearing Data of All The Tables
                dbHelper?.clearAllTables()
                historyBinding.hiRecyclerView.adapter?.notifyDataSetChanged()
                onResume()
        }

        //Adding Listener to the Back Method
        historyBinding.backButton.setOnClickListener {
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
        //setting Up The Recycler View
        historyList = ArrayList()
        historyList = dbHelper?.historyDao?.getAllHistory() as ArrayList<History>
        historyList.reverse()
        historyBinding.hiRecyclerView.hasFixedSize()

        historyBinding.hiRecyclerView.setItemViewCacheSize(5)
        historyBinding.hiRecyclerView.layoutManager = LinearLayoutManager(this)
        historyBinding.hiRecyclerView.adapter = historyAdapter(this,historyList)


    }
}