package com.dixitkumar.searchxapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
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
import android.view.WindowManager
import android.webkit.WebView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.dixitkumar.searchxapp.databinding.ActivityMainBinding
import com.dixitkumar.searchxapp.databinding.AddBookmarkDialogBinding
import com.dixitkumar.searchxapp.databinding.MoreOptionMenuBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


var dbHelper : DbHelper? = null

class MainActivity : AppCompatActivity() {

    companion object{
        var tabList : ArrayList<Fragment> = ArrayList()
        lateinit var viewPager : ViewPager2
        var isDesktopMode = true
        var isFullScreen = false
        var bookmarkList  :ArrayList<Bookmark> = ArrayList()
        var bookmarkIndex = -1;
        val bookmarkId = "BOOKMARKS"
        val bookmarkKey = "bookmarkList"
        var adapterPostion = -1
    }
     lateinit var mainBinding : ActivityMainBinding
     private var printJob : PrintJob? = null
     var browserRef : BrowserFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        //setting Up The Screen Orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER

        //getting All Bookmarks
        getAllBookmark()

        dbHelper = DbHelper.getDb(this)
        //Initializing The Home Fragment


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

        if(isFullScreen){
            moreOptionMenuBinding.fullscreenButton.apply {
                setIconResource(R.drawable.fullscreen_exit_icon)
            }
        }

        browserRef?.let{
            if(bookmarkIndex != -1){
                bookmarkIndex = isBookmark(it.browserBinding.webView.url!!)
                moreOptionMenuBinding.bookmarkButton.apply {
                    setIconTintResource(R.color.light_blue)
                    setTextColor(ContextCompat.getColor(this@MainActivity,R.color.light_blue))
                }
            }
        }
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

        //Setting Up Listener on FullScreen Mode Button
        moreOptionMenuBinding.fullscreenButton.setOnClickListener {
            it as MaterialButton
            if(!isFullScreen){
                changeFullScreen(enable = true)
                it.setIconResource(R.drawable.fullscreen_exit_icon)
                isFullScreen = true
            }else{
                changeFullScreen(enable = false)
                it.setIconResource(R.drawable.fullscreen_icon)
                isFullScreen = false
            }
        }
        //Setting Up Listener on History Button
        moreOptionMenuBinding.HistoryButton.setOnClickListener {
            startActivity(Intent(this@MainActivity,HistoryActivity::class.java))
        }

        //Setting Up Listener of BookMark Button
        moreOptionMenuBinding.bookmarkButton.setOnClickListener {
            browserRef?.let{
                if(bookmarkIndex == -1){
                    val view = layoutInflater.inflate(R.layout.add_bookmark_dialog,mainBinding.root,false)
                    val bBinding = AddBookmarkDialogBinding.bind(view)

                    val BookmarkDialog  = MaterialAlertDialogBuilder(this).setView(view)
                        .setTitle("Add Bookmark")
                        .setMessage("Url : ${it.browserBinding.webView.url!!}")
                        .setPositiveButton("Add"){self, _->
                            try{
                                val array = ByteArrayOutputStream()
                                it.favicon!!.compress(Bitmap.CompressFormat.PNG,100,array)
                                bookmarkList.add(Bookmark(name = bBinding.bookmarkTitle.text.toString(), url = it.browserBinding.webView.url!!,array.toByteArray()))
                            }catch (e : Exception){
                                bookmarkList.add(
                                    Bookmark(name = bBinding.bookmarkTitle.text.toString(),
                                             url = it.browserBinding.webView.url!!))
                            }
                            self.dismiss()
                        }
                        .setNegativeButton("Cancel"){self , _ ->
                            self.dismiss()
                        }.create()

                    BookmarkDialog.setOnShowListener{
                        val positiveButton = BookmarkDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        val negativeButton = BookmarkDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

                        positiveButton.setTextColor(R.color.black)
                        negativeButton.setTextColor(R.color.black)
                    }

                    BookmarkDialog.show()
                    bBinding.bookmarkTitle.setText(it.browserBinding.webView.title)
                }else{
                    val dialogB = MaterialAlertDialogBuilder(this)
                        .setTitle("Remove Bookmark")
                        .setMessage("Url : ${it.browserBinding.webView.url}")
                        .setPositiveButton("Remove"){self, _->
                            if(adapterPostion != -1){
                                bookmarkList.removeAt(adapterPostion)
                                val editor = getSharedPreferences(bookmarkId, MODE_PRIVATE).edit()
                                val data = GsonBuilder().create().toJson(bookmarkList)
                                editor.putString(bookmarkKey,data)
                                editor.apply()
                                self.dismiss()
                            }
                        }
                        .setNegativeButton("Cancel"){self,_->
                            self.dismiss()
                        }
                        .create()

                    dialogB.setOnShowListener {
                        val positiveButton = dialogB.getButton(AlertDialog.BUTTON_POSITIVE)
                        val negativeButton = dialogB.getButton(AlertDialog.BUTTON_NEGATIVE)

                        positiveButton.setTextColor(Color.BLACK)
                        negativeButton.setTextColor(Color.BLACK)
                    }

                    dialogB.show()
                }
            }
            dialog.dismiss()
        }
    }

    private fun changeFullScreen(enable : Boolean){
        if(enable) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowInsetsControllerCompat(window, mainBinding.root).let {
                it.hide(WindowInsetsCompat.Type.systemBars())
                it.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }else{
            WindowCompat.setDecorFitsSystemWindows(window, true)
            WindowInsetsControllerCompat(window, mainBinding.root).show(WindowInsetsCompat.Type.systemBars())
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
    fun isBookmark(url : String) : Int{
        bookmarkList.forEachIndexed{index,bookmark->
            if(bookmark.url == url)return index
        }
        return -1
    }

    fun saveBookmark(){
        val editor = getSharedPreferences(bookmarkId, MODE_PRIVATE).edit()
        val data = GsonBuilder().create().toJson(bookmarkList)
        editor.putString(bookmarkKey,data)
        editor.apply()
    }

    fun getAllBookmark(){
        bookmarkList = ArrayList()
        val editor = getSharedPreferences(bookmarkId, MODE_PRIVATE)
        val data = editor.getString(bookmarkKey,null)

        if(data!=null){
            val list : ArrayList<Bookmark> = GsonBuilder().create().fromJson(data,object :
                TypeToken<ArrayList<Bookmark>>(){}.type)
            bookmarkList.addAll(list)
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
fun changeTab(url: String , fragment: Fragment, isPrivate : Boolean = false){
            MainActivity.tabList.add(BrowserFragment(url))
            MainActivity.viewPager.adapter?.notifyDataSetChanged()
            MainActivity.viewPager.currentItem = MainActivity.tabList.size-1

}
