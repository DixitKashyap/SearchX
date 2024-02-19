package com.dixitkumar.searchxapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.dixitkumar.searchxapp.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

@Suppress("DEPRECATION")
class HomeFragment : Fragment() {

    //reference of home fragment
    private lateinit var homeBinding: FragmentHomeBinding

    //Getting a Reference of Main Activity
    private lateinit var mainActivityRef : MainActivity
    private var RequestCode = 111
    lateinit var searchedQuery : String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        homeBinding = FragmentHomeBinding.inflate(layoutInflater)
        mainActivityRef = requireActivity() as MainActivity



//        homeBinding.searchView.setOnQueryTextListener()
        return homeBinding.root
    }

    override fun onResume() {
        super.onResume()

        MainActivity.tabList[MainActivity.viewPager.currentItem].name = "Home"
        MainActivity.tabBtn.text = MainActivity.tabList.size.toString()
        //setting up the listener of search button
        mainActivityRef.mainBinding.searchBar.setText("")
        homeBinding.searchView.setQuery("",false)
        mainActivityRef.mainBinding.websiteIcon.setImageResource(R.drawable.search_icon)


        //Setting Up Search Query Listener on Search View
        homeBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String): Boolean {
               if(checkNetwork(requireContext())){
                   changeTab(query,BrowserFragment(query))
               }else{
                   Snackbar.make(homeBinding.root,"Internet Is Not Connected",2000).show()
               }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
              return false
            }

        })
        mainActivityRef.mainBinding.searchButton.setOnClickListener {
            if(checkNetwork(requireContext())){
                changeTab(mainActivityRef.mainBinding.searchBar.text.toString(),
                    BrowserFragment(mainActivityRef.mainBinding.searchBar.text.toString()))
            }else{
                Snackbar.make(homeBinding.root,"Internet is Not Connected",3000).show()
            }
        }

        //Setting Up Home Button Listener
        mainActivityRef.mainBinding.homeButton.setOnClickListener {
            MainActivity.tabList.add(Tabs("Home",HomeFragment()))
            MainActivity.viewPager.adapter?.notifyDataSetChanged()
            MainActivity.viewPager.currentItem = MainActivity.tabList.size-1
        }

        //Setting Up The Bookmark Recycler View
        homeBinding.rvBookmark.setHasFixedSize(true)
        homeBinding.rvBookmark.setItemViewCacheSize(5)
        homeBinding.rvBookmark.layoutManager = GridLayoutManager(requireContext(),5)
        homeBinding.rvBookmark.adapter = BookmarkAdapter(requireContext())


        if(MainActivity.bookmarkList.size<1){
            homeBinding.allbookmarks.visibility = View.GONE
        }else{
            homeBinding.allbookmarks.setOnClickListener{
                startActivity(Intent(requireContext(),Bookmark_activity::class.java))
            }
        }

        //Setting Up Speech to text
        homeBinding.voiceSearch.setOnClickListener {
            speakUp()
        }

    }

    fun speakUp(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,3000)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Start Speaking....")
        startActivityForResult(intent,RequestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RequestCode && resultCode == RESULT_OK)
            searchedQuery = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0).toString()
          homeBinding.searchView.setQuery(searchedQuery,true)
    }
}

