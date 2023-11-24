package br.com.course.app.tcc

import android.Manifest
import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


const val WEB_VIEW_URL = "https://tcc-app-course.vercel.app/"
const val REQUEST_CODE_PERMISSIONS = 200

class MainActivity : AppCompatActivity() {

    private val permissions = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
    )

    private lateinit var webView: WebView

    val REQUEST_SELECT_FILE = 100
    private val FILECHOOSER_RESULTCODE = 1
    var uploadMessage: ValueCallback<Array<Uri>>? = null

    private var mUploadMessage: ValueCallback<*>? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SELECT_FILE) {
            if (null == uploadMessage) {
                return
            }
            uploadMessage?.onReceiveValue(
                WebChromeClient.FileChooserParams.parseResult(
                    resultCode,
                    data
                )
            )
            uploadMessage = null

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webChromeClient = object : WebChromeClient() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }

            // For Lollipop 5.0+ Devices
            override fun onShowFileChooser(
                mWebView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (uploadMessage != null) {
                        uploadMessage?.onReceiveValue(null)
                        uploadMessage = null
                    }
                    uploadMessage = filePathCallback
                    val intent = fileChooserParams.createIntent()
                    try {
                        startActivityForResult(intent, REQUEST_SELECT_FILE)
                    } catch (e: ActivityNotFoundException) {
                        uploadMessage = null
                        Toast.makeText(
                            applicationContext,
                            "Cannot Open File Chooser",
                            Toast.LENGTH_LONG
                        ).show()
                        return false
                    }
                    return true
                } else {
                    return false
                }
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                //filePermission()
                mUploadMessage = uploadMsg
                val i = Intent(Intent.ACTION_GET_CONTENT)
                i.addCategory(Intent.CATEGORY_OPENABLE)
                i.type = "image/*"
                startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    FILECHOOSER_RESULTCODE
                )
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.mediaPlaybackRequiresUserGesture = true
        webView.settings.cacheMode = WebSettings.LOAD_NO_CACHE
        webView.settings.setGeolocationEnabled(true)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                CookieSyncManager.getInstance().sync()
                super.onPageFinished(view, url)
            }
        }
        webView.loadUrl(WEB_VIEW_URL)

        askForPermissions()
    }

    override fun onDestroy() {
        CookieManager.getInstance().flush()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (webView.copyBackForwardList().currentIndex > 0) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun askForPermissions() {
        val shouldAskPermission = permissions.firstOrNull {
            ContextCompat.checkSelfPermission(
                this,
                it
            ) != PackageManager.PERMISSION_GRANTED
        } != null

        if (shouldAskPermission) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSIONS)
        }
    }
}
