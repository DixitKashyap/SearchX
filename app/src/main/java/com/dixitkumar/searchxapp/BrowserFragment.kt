package com.dixitkumar.searchxapp

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.core.view.WindowInsetsControllerCompat
import com.dixitkumar.searchxapp.databinding.FragmentBrowserBinding
import com.google.android.material.snackbar.Snackbar
import java.io.ByteArrayOutputStream
import java.lang.Exception

@Suppress("DEPRECATION")
class BrowserFragment(private val link : String) : Fragment() {

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

        browserBinding.webView?.apply {
            settings.safeBrowsingEnabled = true
            settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
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
                    if(title.isNullOrEmpty()){
                        dbHelper?.historyDao?.addHistoryItem(History(title = "No Title Found", url = url.toString()))
                    }else{
                        dbHelper?.historyDao?.addHistoryItem(History(title = title, url = url.toString()))
                    }
                    mainActivityRef.mainBinding.progressBar.visibility = View.GONE
                    view?.zoomOut()
                }
            }

            //Setting Up The Custom Chrome Client
            webChromeClient = CustomChromeClient(context)

            //Setting Up Home Button Listener
            mainActivityRef.mainBinding.homeButton.setOnClickListener {
                MainActivity.tabList.add(HomeFragment())
                MainActivity.viewPager.adapter?.notifyDataSetChanged()
                MainActivity.viewPager.currentItem = MainActivity.tabList.size-1
            }

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
            Log.d("TAG","Full Screen Mode Entered")
         }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            mainActivityRef.mainBinding.progressBar.progress = newProgress
        }
    }

    override fun onPause() {
        browserBinding.webView.reload()
        super.onPause()
        mainActivityRef.saveBookmark()

    }
}