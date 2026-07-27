package cloud.cyberverse.asterion.ui.components

import android.annotation.SuppressLint
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * A WebView tuned for third-party streaming embeds (football match streams, movie "web" sources
 * with no direct playable URL) - shared so every embed player gets the same fixes instead of
 * reinventing this per screen. Three real, previously-diagnosed defensive checks these ad-driven
 * embeds run, all handled here:
 * - `domStorageEnabled`: WebView disables localStorage/sessionStorage by default, unlike a real
 *   browser; pages that assume it exists crash without this.
 * - Stripping WebView's `"; wv)"` UA marker: some sites use it to detect and refuse in-app WebViews.
 * - Swallowing `window.open()` popups: sandboxed iframes without `allow-popups` can't open a
 *   window, so these players treat a blocked `window.open()` as proof they're sandboxed and
 *   refuse to play (surfacing a generic "remove the sandbox attribute" message) - even though
 *   nothing here actually nests them in a real `<iframe sandbox>`. WebView blocks `window.open()`
 *   by default unless multi-window support and an `onCreateWindow` handler are wired up; handing
 *   the popup a disposable throwaway WebView here lets the check "succeed" without ever showing
 *   the popup (almost always an ad) to the user.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedWebView(
    url: String,
    modifier: Modifier = Modifier,
    reloadKey: Any? = url,
    onPageFinished: () -> Unit = {},
    onMainFrameError: () -> Unit = {},
) {
    // Keying forces a fresh WebView instance when reloadKey changes - reusing the same instance
    // after a failed load can keep serving the cached error page.
    key(reloadKey) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString.replace("; wv", "")
                    settings.javaScriptCanOpenWindowsAutomatically = true
                    settings.setSupportMultipleWindows(true)
                    webChromeClient = object : WebChromeClient() {
                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: Message?,
                        ): Boolean {
                            val popup = WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                    ): Boolean = true
                                }
                            }
                            (resultMsg?.obj as? WebView.WebViewTransport)?.webView = popup
                            resultMsg?.sendToTarget()
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            onPageFinished()
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            // Only the top-level frame failing counts as this stream being dead -
                            // embeds routinely have sub-resources (ads, trackers) that 404/timeout
                            // without the stream itself being broken.
                            if (request?.isForMainFrame == true) {
                                onMainFrameError()
                            }
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = modifier.fillMaxSize(),
        )
    }
}
