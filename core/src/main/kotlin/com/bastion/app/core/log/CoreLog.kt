package com.bastion.app.core.log

/**
 * Logging mínimo para código de :core (Kotlin/JVM plano, sin dependencia a android.util.Log ni
 * a RemoteLogger, que viven en :app). Cada app anfitriona (:app Android, :desktopApp) instala un
 * [Sink] al arrancar. Sin sink instalado, cae a stdout (útil en tests/desarrollo).
 *
 * :app instala un sink que reenvía a RemoteLogger (HIM-009/HIM-011) — comportamiento idéntico
 * al de antes de HIM-016, cero regresión. :desktopApp instala su propio sink simple.
 */
object CoreLog {
    interface Sink {
        fun log(level: String, tag: String, msg: String, throwable: Throwable? = null)
        fun setContext(sessionId: String?, host: String?) {}
    }

    @Volatile
    private var sink: Sink = StdoutSink

    fun install(newSink: Sink) {
        sink = newSink
    }

    fun i(tag: String, msg: String) = sink.log("INFO", tag, msg)
    fun w(tag: String, msg: String) = sink.log("WARN", tag, msg)
    fun e(tag: String, msg: String, throwable: Throwable? = null) = sink.log("ERROR", tag, msg, throwable)
    fun setContext(sessionId: String?, host: String?) = sink.setContext(sessionId, host)

    fun logger(tag: String): ComponentLog = ComponentLog(tag)

    class ComponentLog(private val tag: String) {
        fun i(msg: String) = i(tag, msg)
        fun w(msg: String) = w(tag, msg)
        fun e(msg: String, throwable: Throwable? = null) = e(tag, msg, throwable)
        fun state(from: Any, to: Any) = i("state $from → $to")
    }

    private object StdoutSink : Sink {
        override fun log(level: String, tag: String, msg: String, throwable: Throwable?) {
            println("[$level] $tag: $msg")
            throwable?.printStackTrace()
        }
    }
}
