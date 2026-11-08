package io.github.jcoronadoccorimanya08.rachas

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.browser.trusted.TrustedWebActivityIntentBuilder

/**
 * Abre la app web a pantalla completa en Chrome (Trusted Web Activity) y abre un canal postMessage
 * con ella. La app web manda por ese canal el resumen que muestra el widget, y aquí se guarda.
 * Los datos de la app siguen en Chrome: con tu patrón, tu huella y tus respaldos de siempre.
 *
 * Cómo se abre el canal (Chrome lo exige en este orden):
 * 1. warmup() y una sesión nueva; 2. validateRelationship(USE_AS_ORIGIN) contra assetlinks.json;
 * 3. cuando la página terminó de cargar y la validación salió bien, requestPostMessageChannel();
 * 4. Chrome entrega el puerto a la página y avisa con onMessageChannelReady(). Chrome entra por
 *    PostMessageService (declarado en el manifiesto) para pasarnos los mensajes de la página.
 * Si la página se recarga (por ejemplo, al actualizar la app web), el canal se cierra y se pide otro.
 */
abstract class TwaLauncherActivity : Activity() {

    /** Ruta de la app dentro del sitio, por ejemplo "/val-quirurgico/". */
    protected abstract val path: String

    /** "valq" o "bitacora": de qué app se acepta el resumen. */
    protected abstract val appKey: String

    private var connection: CustomTabsServiceConnection? = null
    private var session: CustomTabsSession? = null
    private var launched = false
    private var pendingIntent: Intent? = null

    /** Esperando la respuesta al permiso de notificaciones (para el recordatorio de las 22:30). */
    private var asking = false

    // Estado del canal. Chrome llama a estos métodos en el hilo principal.
    private var validated = false
    private var pageLoaded = false
    private var channelRequested = false
    private var channelReady = false

    private val callback = object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
            when (navigationEvent) {
                NAVIGATION_STARTED -> {
                    // Página nueva: el canal anterior se cierra.
                    pageLoaded = false
                    channelRequested = false
                    channelReady = false
                }
                NAVIGATION_FINISHED -> {
                    pageLoaded = true
                    requestChannel()
                }
            }
        }

        override fun onRelationshipValidationResult(relation: Int, requestedOrigin: Uri, result: Boolean, extras: Bundle?) {
            // Solo si el sitio confirma (assetlinks.json) que esta app puede hablar en su nombre.
            validated = result
            requestChannel()
        }

        override fun onMessageChannelReady(extras: Bundle?) {
            channelReady = true
            // Le avisa a la página que la app está lista para recibir.
            session?.postMessage(MSG_HELLO, null)
        }

        override fun onPostMessage(message: String, extras: Bundle?) {
            // Confirma a la página que el widget quedó actualizado.
            if (WidgetStore.save(applicationContext, appKey, message)) session?.postMessage(MSG_SAVED, null)
        }
    }

    private fun requestChannel() {
        val s = session ?: return
        if (!validated || !pageLoaded || channelRequested || channelReady) return
        channelRequested = s.requestPostMessageChannel(ORIGIN, ORIGIN, Bundle())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ticker.scheduleAll(applicationContext)
        if (shouldAskNotifications()) {
            // Una sola vez: el permiso para el recordatorio de las 22:30. La app web se abre después de responder.
            asking = true
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ASKED, true).apply()
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
        }
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
                maybeLaunch()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                session = null
            }
        }
        connection = conn
        if (!CustomTabsClient.bindCustomTabsService(this, provider, conn)) openInBrowser()
    }

    private fun shouldAskNotifications(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminder.allowed(this) &&
            !getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ASKED, false)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_NOTIFICATIONS) return
        asking = false
        Ticker.scheduleAll(applicationContext)
        maybeLaunch()
    }

    /** Abre la app web cuando ya hay sesión con Chrome y no estamos esperando el permiso. */
    private fun maybeLaunch() {
        val s = session ?: return
        if (asking || launched) return
        launched = true
        launchTwa(s, pendingIntent ?: intent)
        pendingIntent = null
    }

    private fun startUrl(intent: Intent?): Uri {
        val urge = intent?.getBooleanExtra(EXTRA_URGE, false) == true
        return Uri.parse(ORIGIN.toString() + path + if (urge) "?urgencia=1" else "")
    }

    private fun launchTwa(s: CustomTabsSession, intent: Intent?) {
        TrustedWebActivityIntentBuilder(startUrl(intent)).build(s).launchTrustedWebActivity(this)
    }

    /** Si ya estaba abierta (por ejemplo, tocas el widget), abre la app web de nuevo con la misma sesión. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val s = session
        if (s != null && !asking) {
            launchTwa(s, intent)
        } else {
            // Todavía conectando con Chrome (o esperando el permiso): se abre apenas se pueda.
            pendingIntent = intent
            launched = false
            maybeLaunch()
        }
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
        startActivity(Intent(Intent.ACTION_VIEW, startUrl(intent)))
        finish()
    }

    companion object {
        val ORIGIN: Uri = Uri.parse("https://jcoronadoccorimanya08-jpg.github.io")
        const val EXTRA_URGE = "urgencia"
        const val MSG_HELLO = "rachas:hola"
        const val MSG_SAVED = "rachas:ok"
        private const val PREFS = "app"
        private const val KEY_ASKED = "asked_notifications"
        private const val REQ_NOTIFICATIONS = 7
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
