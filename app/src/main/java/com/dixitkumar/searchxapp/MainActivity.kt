package com.dixitkumar.searchxapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintJob
import android.print.PrintManager
import android.util.Log
import android.view.Gravity
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dixitkumar.searchxapp.MainActivity.Companion.tabList
import com.dixitkumar.searchxapp.databinding.ActivityMainBinding
import com.dixitkumar.searchxapp.databinding.MoreOptionMenuBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object{
        var tabList : ArrayList<Fragment> = ArrayList()
        lateinit var viewPager : ViewPager2
        var isDesktopMode = false
    }
     lateinit var mainBinding : ActivityMainBinding
     private var printJob : PrintJob? = null
     var browserRef : BrowserFragment? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        //setting Up The Screen Orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        tabList.add(HomeFragment())
        mainBinding.viewPager.adapter = FragmentAdapter(supportFragmentManager,lifecycle)
        mainBinding.viewPager.isUserInputEnabled = false
        viewPager = mainBinding.viewPager


        //Setting Up The Listener on Add Tabs Button
        mainBinding.addTabButton.setOnClickListener {

            val dialogTab = MaterialAlertDialogBuilder(this)
                .setTitle("Select Tab")
                .setMessage("Select One Tab of The Following")
                .setPositiveButton("Private Tab") { self, _ ->
                    changeTab("",PrivateTab_Fragment(), isPrivate = true)
                    self.dismiss() }
                .setNeutralButton("Google") { self, _ ->
                    changeTab("https://www.google.com",BrowserFragment("https://www.google.com"))
                    self.dismiss() }
                .create()

            dialogTab.setOnShowListener {
                val positiveButton = dialogTab.getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = dialogTab.getButton(AlertDialog.BUTTON_NEUTRAL)

                positiveButton.setTextColor(Color.BLACK)
                negativeButton.setTextColor(Color.BLACK)

                positiveButton.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources,R.drawable.private_tab_icon,theme),null,null,null)
                negativeButton.setCompoundDrawablesWithIntrinsicBounds(ResourcesCompat.getDrawable(resources,R.drawable.add_icon,theme),null,null,null)
            }

            dialogTab.show()
        }
        mainBinding.moreOptionButton.setOnClickListener {
            openMoreOptionDialog()
        }
    }

    //Opening Dialog On Click Of  A Button
    @SuppressLint("ResourceAsColor", "ResourceType")
    private fun openMoreOptionDialog() {
        try {
            browserRef = tabList[mainBinding.viewPager.currentItem] as BrowserFragment
        }catch (e : Exception){

        }

        val view = layoutInflater.inflate(R.layout.more_option_menu,mainBinding.root,false)
        val moreOptionMenuBinding = MoreOptionMenuBinding.bind(view)

        val dialog = MaterialAlertDialogBuilder(this).setView(view).create()
        dialog.window?.apply {
            attributes.gravity = Gravity.BOTTOM
            attributes.y = 50
            setBackgroundDrawable(ColorDrawable(R.color.white))
        }

        dialog.show()

        //Setting Up Browser Menu's Back Button
        moreOptionMenuBinding.backButton.setOnClickListener {
            browserRef?.let {
                if(it.browserBinding.webView.canGoBack()){
                    it.browserBinding.webView.goBack()
                }
            }
        }

        //Setting Up Browser Menu's Forward Button
        moreOptionMenuBinding.forwardButton.setOnClickListener {
            browserRef?.apply {
                if(browserBinding.webView.canGoForward()){
                    browserBinding.webView.goForward()
                }
            }
        }
        //Setting Up Browser Menu's Reload Button
        moreOptionMenuBinding.reloadButton.setOnClickListener {
            browserRef?.apply {
                browserBinding.webView.reload()
            }
        }

        //Setting Up The Save Page Button
        moreOptionMenuBinding.saveButton.setOnClickListener {
          dialog.dismiss()
            if(browserRef !=null){
                browserRef?.browserBinding?.webView?.let { it1 -> savePage(webpage = it1) }
            }else{
                Snackbar.make(mainBinding.root,"First Open A Web Page",Snackbar.LENGTH_LONG).show()
            }
        }

        //Setting Up The Share Web Page Button
        moreOptionMenuBinding.sharePageButton.setOnClickListener {
            dialog.dismiss()
            if(browserRef != null){
                var i = Intent(Intent.ACTION_SEND)
                i.setType("text/plain");
                var link = browserRef?.browserBinding?.webView?.url.toString()
                i.putExtra(Intent.EXTRA_TEXT,"Here is Your Link : \n ${link}")
                startActivity(Intent.createChooser(i,"Share Url :- "))
            }else{
                Snackbar.make(mainBinding.root,"Open A Webpage First!",1500).show()
            }
        }

        //Setting The Desktop Mode in Our WebView
        moreOptionMenuBinding.desktopModeButton.setOnClickListener {
            it as MaterialButton
            browserRef?.browserBinding?.webView?.apply {
                if(isDesktopMode){
                    settings.userAgentString = null
                    reload()
                    isDesktopMode = false
                    Log.d("TAG", isDesktopMode.toString())
                }else{
                    settings.userAgentString =  "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:99.0) Gecko/20100101 Firefox/99.0"
                    settings.useWideViewPort = true
                    evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                            " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null)

                    reload()
                    isDesktopMode = true
                    Log.d("TAG", isDesktopMode.toString())
                }
                reload()
                dialog.dismiss()
            }
        }
    }


    //For Saving Web Page
    private fun savePage(webpage : WebView){
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager

        val jobName = "${URL(webpage.url).host}_${SimpleDateFormat("HH:mm d_MM-yy", Locale.ENGLISH)
            .format(Calendar.getInstance().time)}"
        val printAdapter = webpage.createPrintDocumentAdapter(jobName)
        val printAttributes = PrintAttributes.Builder()
        printJob = printManager.print(jobName,printAdapter,printAttributes.build())
    }

    override fun onResume() {
        super.onResume()
        printJob?.let {
            when{
                it.isCompleted -> Snackbar.make(mainBinding.root,"Page Downloaded Complete -> ${it.info.label}",1500).show()
                it.isFailed -> Snackbar.make(mainBinding.root,"Page Downloading Failed ->${it.info.label} ",1500).show()
            }
        }
    }

    override fun onBackPressed() {
       var browserRef : BrowserFragment? = null
        try{
            browserRef = tabList[mainBinding.viewPager.currentItem] as BrowserFragment
        }catch (e : Exception){

        }

        when{
            browserRef?.browserBinding?.webView?.canGoBack()  == true ->{
                browserRef.browserBinding.webView.goBack()
            }

            mainBinding.viewPager.currentItem!=0->{
                tabList.removeAt(mainBinding.viewPager.currentItem)
                mainBinding.viewPager.adapter?.notifyDataSetChanged()
                mainBinding.viewPager.currentItem = tabList.size-1
            }

            else -> super.onBackPressed()
        }
    }

    class FragmentAdapter(fr : FragmentManager,lc: Lifecycle) : FragmentStateAdapter(fr,lc){
        override fun getItemCount(): Int {
            return tabList.size
        }

        override fun createFragment(position: Int): Fragment {
           return tabList[position]
        }

    }
}
fun checkNetwork(context : Context) : Boolean{
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
        val network = connectivityManager.activeNetwork?: return false
        val activityNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when{
            activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activityNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

            else -> false
        }
    }else{
        val networkInfo = connectivityManager.activeNetworkInfo ?: return false
        return networkInfo.isConnected
    }
}

fun changeTab(url: String = "", fragment: Fragment,isPrivate : Boolean = false){
    when{
        isPrivate == true ->{
            MainActivity.tabList.add(PrivateTab_Fragment())
            MainActivity.viewPager.adapter?.notifyDataSetChanged()
            MainActivity.viewPager.currentItem = MainActivity.tabList.size-1
        }

        else -> {
            MainActivity.tabList.add(BrowserFragment(url))
            MainActivity.viewPager.adapter?.notifyDataSetChanged()
            MainActivity.viewPager.currentItem = MainActivity.tabList.size-1

        }
    }

}