/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.core

import android.app.Application
import android.content.Context
import com.sandesh.nil.database.DatabaseProvider
import com.sandesh.nil.interceptor.NILInterceptor
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.model.NetworkEventSummary
import com.sandesh.nil.overlay.NILFloatingButtonController
import com.sandesh.nil.storage.NILRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NIL {
    enum class InterceptorType {
        OK_HTTP,
        HTTP_URL_CONNECTION
    }

    private const val DEFAULT_INSPECTOR_PAYLOAD_CHAR_LIMIT = 200_000
    private const val DEFAULT_MAX_STORED_EVENTS = 100

    private val interceptor = NILInterceptor()

    @Volatile
    private var initialized = false
    @Volatile
    private var persistenceEnabled: Boolean = true
    @Volatile
    private var inspectorPayloadCharLimit: Int = DEFAULT_INSPECTOR_PAYLOAD_CHAR_LIMIT
    private val _isLoggingPaused = MutableStateFlow(false)
    val isLoggingPaused: StateFlow<Boolean> get() = _isLoggingPaused

    val events: StateFlow<List<NetworkEventSummary>> get() = NILRepository.events

    /**
     * SDK initialization
     */
    fun initialize(
        context: Context,
        enableFloatingButton: Boolean = false,
        inspectorPayloadCharLimit: Int = DEFAULT_INSPECTOR_PAYLOAD_CHAR_LIMIT,
        maxStoredEvents: Int = DEFAULT_MAX_STORED_EVENTS,
        persistenceEnabled: Boolean = true
    ) {
        this.inspectorPayloadCharLimit = inspectorPayloadCharLimit.coerceAtLeast(10_000)
        this.persistenceEnabled = persistenceEnabled
        if (initialized) return

        val appContext = context.applicationContext

        val database = if (persistenceEnabled) DatabaseProvider.getDatabase(appContext) else null
        NILRepository.initialize(
            db = database,
            persistenceEnabled = persistenceEnabled,
            maxStoredEvents = maxStoredEvents
        )

        if (enableFloatingButton && appContext is Application) {
            NILFloatingButtonController.initialize(appContext)
        }

        initialized = true
    }

    /**
     * Interceptor entry point. Defaults to OkHttp when no type is provided.
     */
    fun interceptor(): NILInterceptor = interceptor

    /**
     * Type-safe interceptor entry point with explicit transport selection.
     */
    fun interceptor(type: InterceptorType): NILInterceptor {
        return interceptor
    }

    @Deprecated(
        message = "Use the enum overload instead to avoid invalid transport strings.",
        replaceWith = ReplaceWith("interceptor(InterceptorType.HTTP_URL_CONNECTION)")
    )
    fun interceptor(type: String): NILInterceptor = interceptor(type.toInterceptorType())

    fun setFilter(query: String) {
        NILRepository.observeEvents(query)
    }

    fun pauseLogging() {
        _isLoggingPaused.value = true
    }

    fun resumeLogging() {
        _isLoggingPaused.value = false
    }

    fun shouldLogEvents(): Boolean = !_isLoggingPaused.value

    fun inspectorPayloadCharLimit(): Int = inspectorPayloadCharLimit

    fun clearEvents() {
        NILRepository.clearAsync()
    }

    suspend fun clearEventsAwait() {
        NILRepository.clear()
    }

    fun setEventPinned(eventId: String, pinned: Boolean) {
        NILRepository.setPinned(eventId = eventId, pinned = pinned)
    }

    suspend fun getEventById(eventId: String): NetworkEvent? {
        return NILRepository.getEventById(eventId)
    }

}

private fun String.toInterceptorType(): NIL.InterceptorType {
    return when (trim().lowercase()) {
        "okhttp" -> NIL.InterceptorType.OK_HTTP
        "httpurl", "httpurlconnection" -> NIL.InterceptorType.HTTP_URL_CONNECTION
        else -> throw IllegalArgumentException("Unsupported interceptor type: $this")
    }
}
