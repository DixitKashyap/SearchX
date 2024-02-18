package com.dixitkumar.searchxapp

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dixitkumar.searchxapp.databinding.HistoryItemBinding
import com.google.android.material.snackbar.Snackbar

class historyAdapter(val context : Context, var historyList : ArrayList<History>) : RecyclerView.Adapter<historyAdapter.ViewHolder>() {

    private val color = context.resources.getIntArray(R.array.randomColor)
    inner class ViewHolder(history: HistoryItemBinding)
        : RecyclerView.ViewHolder(history.root){
            var icon = history.HistoryIcon
            var title = history.historyWebTitle
            var url = history.historyWebUrl
            var delete = history.deleteHistoryItem
            var root = history.parentLayout
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
       return ViewHolder(HistoryItemBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
       return historyList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(holder.title.text.isNullOrEmpty()){
            holder.icon.text = "E"
        }else{
            holder.icon.text = historyList[position]?.title!![0].toString()
        }
        holder.title.text = historyList[position].title
        holder.url.text  = historyList[position].url

        holder.root.setOnClickListener {

            when{
                checkNetwork(context) ->{
                    MainActivity.adapterPostion = position

                    changeTab(url =historyList[position].url.toString(),BrowserFragment(link = historyList[position].url.toString()))

                    (context as Activity).finish()
                }

                else -> Snackbar.make(holder.root,"Internet is not Connected",500).show()
            }

        }

        //Setting Up Listener on delete History Item Button
        holder.delete.setOnClickListener {
            dbHelper?.historyDao?.deleteHistoryItem(historyList[position])
            notifyDataSetChanged()
            historyList.removeAt(position)
        }
    }
}