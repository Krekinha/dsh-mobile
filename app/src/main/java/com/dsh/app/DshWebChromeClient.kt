package com.dsh.app

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView

class DshWebChromeClient(
    private val onLaunchFileChooser: (Intent) -> Unit
) : WebChromeClient() {

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        // Cancel any pending callback
        this.filePathCallback?.onReceiveValue(null)
        this.filePathCallback = filePathCallback

        val intent = try {
            fileChooserParams?.createIntent() ?: createFallbackIntent()
        } catch (_: Exception) {
            createFallbackIntent()
        }

        return try {
            onLaunchFileChooser(intent)
            true
        } catch (_: Exception) {
            this.filePathCallback?.onReceiveValue(null)
            this.filePathCallback = null
            false
        }
    }

    fun handleFileChooserResult(resultUris: Array<Uri>?) {
        filePathCallback?.onReceiveValue(resultUris)
        filePathCallback = null
    }

    private fun createFallbackIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
    }
}
