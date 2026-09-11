package com.dsh.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class DshWebViewClient(
    private val onPageStartedCallback: () -> Unit,
    private val onPageFinishedCallback: () -> Unit,
    private val onErrorCallback: (errorCode: Int, description: String) -> Unit
) : WebViewClient() {

    private var hasError: Boolean = false

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val uri = request?.url ?: return false
        val scheme = uri.scheme?.lowercase() ?: ""

        if (scheme == "http" || scheme == "https") {
            // Keep HTTP/HTTPS browsing inside the wrapper
            return false
        }

        // Handle external protocols (mailto, tel, intent, etc.)
        return try {
            val context = view?.context ?: return false
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            true
        }
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        hasError = false
        onPageStartedCallback()
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (!hasError) {
            onPageFinishedCallback()
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            hasError = true
            val errorCode = error?.errorCode ?: -1
            val description = error?.description?.toString() ?: "Network error"
            onErrorCallback(errorCode, description)
        }
    }
}
