package com.dixitkumar.searchxapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.dixitkumar.searchxapp.databinding.ActivityBookmarkBinding

class Bookmark_activity : AppCompatActivity() {

    private lateinit var  bookmarkBinding: ActivityBookmarkBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bookmarkBinding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(bookmarkBinding.root)

        bookmarkBinding.rvBookmark.setItemViewCacheSize(10)
        bookmarkBinding.rvBookmark.hasFixedSize()
        bookmarkBinding.rvBookmark.layoutManager = LinearLayoutManager(this)
        bookmarkBinding.rvBookmark.adapter = BookmarkAdapter(this,isActivity = true)

        bookmarkBinding.backButton.setOnClickListener {
            finish()
        }
    }
}