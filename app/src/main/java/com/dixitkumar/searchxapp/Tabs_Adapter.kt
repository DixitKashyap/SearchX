package com.dixitkumar.searchxapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.dixitkumar.searchxapp.databinding.TabsBinding
import com.google.android.material.snackbar.Snackbar

class Tabs_Adapter(var context: Context, private val dialog: AlertDialog) :
    RecyclerView.Adapter<Tabs_Adapter.ViewHolder>()
{

    inner class ViewHolder(tabsBinding: TabsBinding) : RecyclerView.ViewHolder(tabsBinding.root){
        val cancel = tabsBinding.tabCancelButton
        val name = tabsBinding.tabName
        val root = tabsBinding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
      return ViewHolder(TabsBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
      return MainActivity.tabList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
       holder.name.text = MainActivity.tabList[position].name
        holder.root.setOnClickListener{
            MainActivity.viewPager.currentItem = position
            dialog.dismiss()
        }

        holder.cancel.setOnClickListener{
            if(MainActivity.tabList.size == 1 || position == MainActivity.viewPager.currentItem){
                Snackbar.make(MainActivity.viewPager,"Can't Remove the Current Tab",3000).show()
            }else{
                MainActivity.tabList.removeAt(position)
                notifyDataSetChanged()
                MainActivity.viewPager.adapter?.notifyItemRemoved(position)
            }
        }
    }
}