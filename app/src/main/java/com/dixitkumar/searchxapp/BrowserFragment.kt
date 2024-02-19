@file:Suppress("UNREACHABLE_CODE")

package com.dixitkumar.searchxapp

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Base64
import android.util.Log
import android.view.ContextMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ShareActionProvider
import androidx.annotation.RequiresApi
import androidx.core.app.ShareCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dixitkumar.searchxapp.databinding.FragmentBrowserBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception

@Suppress("DEPRECATION")
class BrowserFragment(private val link : String,private val isIngoCognito : Boolean = false) : Fragment() {

    lateinit var browserBinding : FragmentBrowserBinding
    lateinit var mainActivityRef : MainActivity
    var favicon : Bitmap? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        browserBinding = FragmentBrowserBinding.inflate(layoutInflater)



        //Getting Reference of the Main Activity
        mainActivityRef = requireActivity() as MainActivity

        registerForContextMenu(browserBinding.webView)
        browserBinding.webView.apply {
            when{
                URLUtil.isValidUrl(link )->loadUrl(link)

                link.contains(".com",ignoreCase = true) -> loadUrl("https://"+link)
                else -> loadUrl("https://www.google.com/search?q=${link}")
            }
        }

        return browserBinding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        browserBinding.webView.onResume()

        MainActivity.tabList[MainActivity.viewPager.currentItem].name = browserBinding.webView.url.toString()
        MainActivity.tabBtn.text = MainActivity.tabList.size.toString()

        browserBinding.webView?.apply {
            settings.safeBrowsingEnabled = true
            if(isIngoCognito){
                settings.cacheMode = WebSettings.LOAD_NO_CACHE
            }else{
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            settings.javaScriptEnabled = true
            settings.setSupportZoom(true)
            settings.allowFileAccess
            settings.allowFileAccessFromFileURLs
            settings.allowUniversalAccessFromFileURLs
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            webViewClient = object : WebViewClient(){

                override fun onLoadResource(view: WebView?, url: String?) {
                    super.onLoadResource(view, url)
                    if(MainActivity.isDesktopMode){
                        view?.evaluateJavascript("document.querySelector('meta[name=\"viewport\"]').setAttribute('content'," +
                                " 'width=1024px, initial-scale=' + (document.documentElement.clientWidth / 1024));", null)

                    }
                }

                //For Loading the Ulr in the search Bar
                override fun doUpdateVisitedHistory(
                    view: WebView?,
                    url: String?,
                    isReload: Boolean
                ) {
                    super.doUpdateVisitedHistory(view, url, isReload)
                    mainActivityRef.mainBinding.searchBar.text = SpannableStringBuilder(url)
                    MainActivity.tabList[MainActivity.viewPager.currentItem].name = url.toString()
                }

                //Reseting the Loading Bar to 0
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    mainActivityRef.mainBinding.progressBar.visibility = View.VISIBLE
                    mainActivityRef.mainBinding.progressBar.progress = 0
                }



                //On Page Loading Hiding the progress bar on page on Loaded
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if(!isIngoCognito){
                        if(title.isNullOrEmpty()){
                            dbHelper?.historyDao?.addHistoryItem(History(title = "No Title Found", url = url.toString()))
                        }else{
                            dbHelper?.historyDao?.addHistoryItem(History(title = title, url = url.toString()))
                        }
                    }else{

                    }
                    mainActivityRef.mainBinding.progressBar.visibility = View.GONE
                    view?.zoomOut()
                }
            }

            //Setting Up The Custom Chrome Client
            webChromeClient = CustomChromeClient(context)

            //Setting Up Home Button Listener
            mainActivityRef.mainBinding.homeButton.setOnClickListener {
                MainActivity.tabList.add(Tabs("Home",HomeFragment()))
                MainActivity.viewPager.adapter?.notifyDataSetChanged()
                MainActivity.viewPager.currentItem = MainActivity.tabList.size-1
            }

        }

        //Setting up The Download listener
        browserBinding.webView.setDownloadListener{url,_,_,mimeType,_->
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,null,mimeType))
            val dm = mainActivityRef.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Snackbar.make(browserBinding.root,"Downloading Files",2000).show()
        }

        //Setting Up The Touch Event False
        browserBinding.webView.setOnTouchListener{ _, motionEven ->
            mainActivityRef.mainBinding.root.onTouchEvent(motionEven)
            return@setOnTouchListener false
        }
        mainActivityRef.mainBinding.searchButton.setOnClickListener {
            if(checkNetwork(requireContext())){
                changeTab(mainActivityRef.mainBinding.searchBar.text.toString(),
                BrowserFragment(mainActivityRef.mainBinding.searchBar.text.toString()))
            }else{
                Snackbar.make(browserBinding.root,"Internet is Not Connected",3000).show()
            }
         }
        browserBinding.webView.reload()
    }


    //Creating The Custom Chrome Client
    inner class CustomChromeClient(context : Context) : WebChromeClient(){

        private lateinit var context: Context
        private var mCustomView : View? = null
        private var mCustomViewCallback:  CustomViewCallback? = null
        private var mOriginalSystemUiVisibility : Int = 0

        private var orientation : Int = resources.configuration.orientation

        init {
            this.context = context
        }

        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            super.onReceivedIcon(view, icon)
            try{
                mainActivityRef.mainBinding.websiteIcon.setImageBitmap(icon)
                favicon = icon

                MainActivity.bookmarkIndex = mainActivityRef.isBookmark(view?.url!!)
                if(MainActivity.bookmarkIndex != -1) {
                    val array = ByteArrayOutputStream()
                    icon!!.compress(Bitmap.CompressFormat.PNG, 100, array)
                    MainActivity.bookmarkList[MainActivity.bookmarkIndex].image =
                        array.toByteArray()
                }
            }catch (e : Exception){

            }
        }

        public override fun getDefaultVideoPoster(): Bitmap? {
            if(mCustomView == null){
                return null
            }
            return  BitmapFactory.decodeResource(context.resources,2130837573)
        }

        override fun onHideCustomView() {
            (mainActivityRef.window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null

            mainActivityRef.window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            mainActivityRef.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            mCustomViewCallback = null
        }

        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
            super.onShowCustomView(view, callback)

            if(this.mCustomView !=null){
                onHideCustomView()
                return
            }

            this.mCustomView = view
            WindowInsetsControllerCompat(mainActivityRef.window,browserBinding.root).systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            this.mOriginalSystemUiVisibility= mainActivityRef.window.decorView.systemUiVisibility

            //Force landscape mode before adding the custom view
            mainActivityRef.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

            this.mCustomViewCallback = callback
            (mainActivityRef.window.decorView as FrameLayout).addView(this.mCustomView,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT))
            mainActivityRef.window.decorView.systemUiVisibility = 3846
         }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            mainActivityRef.mainBinding.progressBar.progress = newProgress
        }
    }

    //Showing Up Menu on Click Of A Link
    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)

        val result = browserBinding.webView.hitTestResult

        when(result.type){

            WebView.HitTestResult.IMAGE_TYPE ->{
                menu.add("View Image")
                menu.add("Save Image")
                menu.add("Share Link")
                menu.add("Close")
            }


            WebView.HitTestResult.SRC_ANCHOR_TYPE,WebView.HitTestResult.ANCHOR_TYPE ->{
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share Link")
                menu.add("Close")
            }

            WebView.HitTestResult.EDIT_TEXT_TYPE,WebView.HitTestResult.UNKNOWN_TYPE ->{}
            else ->{
                menu.add("Open in New Tab")
                menu.add("Open Tab in Background")
                menu.add("Share Link")
                menu.add("Close")
            }
        }
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val message = Handler().obtainMessage()
        browserBinding.webView.requestFocusNodeHref(message)
        val url = message.data.getString("url")
        val imageUrl = message.data.getString("src")

        when(item.title){

            "Open in New Tab"->{
                changeTab(url.toString(),BrowserFragment(url.toString()))
            }

            "Open Tab in Background" ->{
                changeTab(url.toString(),BrowserFragment(url.toString()), isBackround = true)
            }

            "View Image" ->{
                if(imageUrl!=null){
                    if(imageUrl.contains("base64")){
                        val pureByte = imageUrl.substring(imageUrl.indexOf(",")+1)
                        val decodedBytes = Base64.decode(pureByte,Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes,0,decodedBytes.size-1)

                        val imageView = ShapeableImageView(requireContext())
                        imageView.setImageBitmap(finalImg)
                        val imgDialog = MaterialAlertDialogBuilder(requireContext()).setView(imageView).create()
                        imgDialog.show()

                        imageView.layoutParams.width = Resources.getSystem().displayMetrics.widthPixels
                        imageView.layoutParams.height = (Resources.getSystem().displayMetrics.heightPixels *0.75).toInt()

                        imageView.requestLayout()
                        imgDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    }else{
                        changeTab(imageUrl,BrowserFragment(imageUrl))
                    }
                }
            }
            "Save Image"->{
                if(imageUrl != null){
                    if(imageUrl.contains("base64")) {

                        val pureBytes = imageUrl.substring(imageUrl.indexOf(",")+1)
                        val decodedBytes = Base64.decode(pureBytes,Base64.DEFAULT)
                        val finalImg = BitmapFactory.decodeByteArray(decodedBytes,0,decodedBytes.size)

                        val path = MediaStore.Images.Media.insertImage(requireActivity().contentResolver,
                            finalImg,"Image",null)


                        Snackbar.make(browserBinding.root,"Image Saved Successfully!",3000).show()

//                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Share Url!")
//                            .setType("image/*")
//                            .setStream(Uri.parse(path))
//                            .startChooser()
                    }else{
                        saveImage(url=imageUrl)
                    }
                }
            }
            "Share Link" -> {
                val tempUlr = url ?: imageUrl
                if (tempUlr != null) {
                    if (tempUlr.contains("base64")) {

                        val pureBytes = tempUlr.substring(tempUlr.indexOf(",") + 1)
                        val decodedBytes = Base64.decode(pureBytes, Base64.DEFAULT)
                        val finalImg =
                            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                        val path = MediaStore.Images.Media.insertImage(
                            requireActivity().contentResolver,
                            finalImg, "Image", null
                        )

                        ShareCompat.IntentBuilder(requireContext()).setChooserTitle("Share Url!")
                            .setType("image/*")
                            .setStream(Uri.parse(path))
                            .startChooser()
                    } else {
                        ShareCompat.IntentBuilder(requireContext())
                            .setChooserTitle("Share Url!")
                            .setType("text/plain")
                            .setText(tempUlr)
                            .startChooser()
                    }
                } else {
                    Snackbar.make(browserBinding.root, "Not a Valid Link!", 3000).show()
                }
            }

        }
        return super.onContextItemSelected(item)
    }

    fun saveImage(url : String){
        if(url.contains(".jpg")){
            val request = DownloadManager.Request(Uri.parse(url))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,URLUtil.guessFileName(url,null,MediaStore.Images.Media.MIME_TYPE))
            val dm = mainActivityRef.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Snackbar.make(browserBinding.root,"Downloading Files",2000).show()
        }
    }

    fun getFileName(url : String) : String{
        return System.currentTimeMillis().toString()+".jpg"
    }
    override fun onPause() {
        browserBinding.webView.reload()
        super.onPause()
        mainActivityRef.saveBookmark()

    }
}