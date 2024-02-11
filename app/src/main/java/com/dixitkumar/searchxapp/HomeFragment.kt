package com.dixitkumar.searchxapp

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
import com.dixitkumar.searchxapp.databinding.FragmentHomeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

class HomeFragment : Fragment() {

    //reference of home fragment
    private lateinit var homeBinding: FragmentHomeBinding

    //Getting a Reference of Main Activity
    private lateinit var mainActivityRef : MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mainActivityRef = requireActivity() as MainActivity
        homeBinding = FragmentHomeBinding.inflate(layoutInflater)

//        homeBinding.searchView.setOnQueryTextListener()
        return homeBinding.root
    }

    override fun onResume() {
        super.onResume()

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
            MainActivity.tabList.add(HomeFragment())
            MainActivity.viewPager.adapter?.notifyDataSetChanged()
            MainActivity.viewPager.currentItem = MainActivity.tabList.size-1
        }
    }
}

