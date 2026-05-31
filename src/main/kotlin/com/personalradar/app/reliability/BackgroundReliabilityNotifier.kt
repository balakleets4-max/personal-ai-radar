package com.personalradar.app.reliability

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArraySet

object BackgroundReliabilityNotifier {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArraySet<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun notifyChanged() {
        mainHandler.post {
            listeners.forEach { listener -> listener.invoke() }
        }
    }
}
