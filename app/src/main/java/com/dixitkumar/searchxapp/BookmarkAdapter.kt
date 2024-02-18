package com.dixitkumar.searchxapp

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.RecyclerView
import com.dixitkumar.searchxapp.databinding.BookmarkItemBinding
import com.dixitkumar.searchxapp.databinding.LongBookmarkBinding
import com.google.android.material.snackbar.Snackbar

class BookmarkAdapter(val context : Context,var isActivity : Boolean = false) :
    RecyclerView.Adapter<BookmarkAdapter.ViewHolder>() {

    //For Getting Random Background color for bookmark background
    private val colors = context.resources.getIntArray(R.array.randomColor)

    inner class ViewHolder(itemView : BookmarkItemBinding?= null,bindingLong: LongBookmarkBinding ?= null)
        : RecyclerView.ViewHolder((itemView?.root?: bindingLong?.root)!!){
        val bookmarkName : TextView = (itemView?.bookmarkName ?: bindingLong?.bookmarkName)!!
        val bookmarkIcon : TextView = (itemView?.bookmarkIcon?: bindingLong?.bookmarkIcon)!!
        val root  = (itemView?.root ?: bindingLong?.root)!!
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if(isActivity){
            return ViewHolder(bindingLong = LongBookmarkBinding.inflate(LayoutInflater.from(context),parent,false))
        }
       return ViewHolder(itemView = BookmarkItemBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    override fun getItemCount(): Int {
         return  MainActivity.bookmarkList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try{
            val icon = BitmapFactory.
            decodeByteArray(MainActivity.bookmarkList[position].image,0,MainActivity.bookmarkList[position].image!!.size)
            holder.bookmarkIcon.background = icon.toDrawable(context.resources)
        }catch (e : Exception){
            holder.bookmarkIcon.setBackgroundColor(colors[(colors.indices).random()])
            holder.bookmarkIcon.text = MainActivity.bookmarkList[position].name[0].toString()
        }
        holder.bookmarkName.text = MainActivity.bookmarkList[position].name

       holder.root.setOnClickListener{
           when{
               checkNetwork(context) ->{
                   MainActivity.adapterPostion = position

                   changeTab(url =MainActivity.bookmarkList[position].url,BrowserFragment(link = MainActivity.bookmarkList[position].url))

                   if(isActivity)(context as Activity).finish()
               }

               else -> Snackbar.make(holder.root,"Internet is not Connected",500).show()
           }

       }
       }

}