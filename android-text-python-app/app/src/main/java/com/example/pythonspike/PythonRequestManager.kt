package com.example.pythonspike

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

data class PythonRequestHandle(
    val requestId: String,
    val cancelToken: AtomicBoolean = AtomicBoolean(false)
)

class PythonRequestManager(
    private val requestIdProvider: () -> String = { UUID.randomUUID().toString() }
) {
    private val activeRequestId = AtomicReference<String?>(null)
    private val handles = ConcurrentHashMap<String, PythonRequestHandle>()

    fun createRequest(): PythonRequestHandle {
        val handle = PythonRequestHandle(requestIdProvider())
        handles[handle.requestId] = handle
        activeRequestId.compareAndSet(null, handle.requestId)
        return handle
    }

    fun cancel(requestId: String): Boolean {
        val handle = handles[requestId] ?: return false
        handle.cancelToken.set(true)
        return true
    }

    fun cancelActive(): PythonRequestHandle? {
        val requestId = activeRequestId.get() ?: return null
        cancel(requestId)
        return handles[requestId]
    }

    fun isCancelled(requestId: String): Boolean {
        return handles[requestId]?.cancelToken?.get() == true
    }

    fun complete(requestId: String) {
        handles.remove(requestId)
        activeRequestId.compareAndSet(requestId, null)
    }
}
