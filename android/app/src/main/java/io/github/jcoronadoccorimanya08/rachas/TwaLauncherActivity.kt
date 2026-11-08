package io.github.jcoronadoccorimanya08.rachas

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

/**
 * Abre la app web a pantalla completa en Chrome (Trusted Web Activity) y abre un canal postMessage
 * con ella. La app web manda por ese canal un resumen de SOLO números, que se guarda para el widget.
 * Los datos de la app siguen en Chrome: con tu patrón, tu huella y tus respaldos de siempre.
 */
abstract class TwaLauncherActivity : Activity() {

    /** Ruta de la app dentro del sitio, por ejemplo "/val-quirurgico/". */
    protected abstract val path: String

    /** "valq" o "bitacora": de qué app se acepta el resumen. */
    protected abstract val appKey: String

    private var connection: CustomTabsServiceConnection? = null
    private var session: CustomTabsSession? = null
    private var launched = false

    private val callback = object : CustomTabsCallback() {
        override fun onRelationshipValidationResult(relation: Int, requestedOrigin: Uri, result: Boolean, extras: Bundle?) {
            // Solo si el sitio confirma (assetlinks.json) que esta app puede hablar en su nombre.
            if (result) session?.requestPostMessageChannel(ORIGIN, ORIGIN, Bundle())
        }

        override fun onMessageChannelReady(extras: Bundle?) {
            // El primer mensaje le entrega a la página el puerto para responder.
            session?.postMessage("rachas:hola", null)
        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            WidgetStore.save(applicationContext, appKey, message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = CustomTabsClient.getPackageName(this, CHROME_PACKAGES, true)
        if (provider == null) {
            openInBrowser()
            return
        }
        val conn = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
                client.warmup(0L)
                val s = client.newSession(callback)
                if (s == null) {
                    openInBrowser()
                    return
                }
                session = s
                s.validateRelationship(CustomTabsService.RELATION_USE_AS_ORIGIN, ORIGIN, null)
                if (!launched) {
                    launched = true
                    TrustedWebActivityIntentBuilder(Uri.parse(ORIGIN.toString() + path))
                        .build(s)
                        .launchTrustedWebActivity(this@TwaLauncherActivity)
                }
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
            }
        }
        connection = conn
        if (!CustomTabsClient.bindCustomTabsService(this, provider, conn)) openInBrowser()
    }

    /** Al volver desde la app web, esta pantalla invisible se cierra. */
    override fun onRestart() {
        super.onRestart()
        finish()
    }

    override fun onDestroy() {
        connection?.let {
            try {
                unbindService(it)
            } catch (_: IllegalArgumentException) {
                // ya estaba desconectado
            }
        }
        connection = null
        super.onDestroy()
    }

    private fun openInBrowser() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ORIGIN.toString() + path)))
        finish()
    }

    companion object {
        val ORIGIN: Uri = Uri.parse("https://jcoronadoccorimanya08-jpg.github.io")
        private val CHROME_PACKAGES = listOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary")
    }
}

class ValQActivity : TwaLauncherActivity() {
    override val path = "/val-quirurgico/"
    override val appKey = "valq"
}

class BitacoraActivity : TwaLauncherActivity() {
    override val path = "/Bitacora/"
    override val appKey = "bitacora"
}
