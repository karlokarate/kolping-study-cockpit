package de.kolping.cockpit.mapping.android.web

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import de.kolping.cockpit.mapping.android.web.js.InjectionScripts

class RecorderWebViewClient(
    private val isRecordingProvider: () -> Boolean,
    private val onUrlChanged: (String) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false
        onUrlChanged(url)
        return false
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        val u = url ?: return
        onUrlChanged(u)

        if (!isRecordingProvider()) return

        // MVP: inject JS to capture fetch/xhr (in-memory only for now)
        view?.evaluateJavascript(InjectionScripts.CAPTURE_NETWORK, null)
    }
}
