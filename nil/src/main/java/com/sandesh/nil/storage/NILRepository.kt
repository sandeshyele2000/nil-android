/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.storage

import com.sandesh.nil.database.NILDatabase
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.model.NetworkEventSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Single source of truth for NIL events.
 * Owns persistence subscription and exposes reactive state for UI.
 */
object NILRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var database: NILDatabase? = null
    private var persistenceEnabled: Boolean = true
    private var maxStoredEvents: Int = 100
    private var activeQuery: String = ""
    private var observeJob: Job? = null
    private val _events = MutableStateFlow<List<NetworkEventSummary>>(emptyList())
    private val inMemoryEvents = mutableListOf<NetworkEvent>()
    val events: StateFlow<List<NetworkEventSummary>> = _events.asStateFlow()

    @Volatile
    private var initialized: Boolean = false

    @Synchronized
    fun initialize(
        db: NILDatabase?,
        persistenceEnabled: Boolean,
        maxStoredEvents: Int
    ) {
        if (initialized) return
        this.database = db
        this.persistenceEnabled = persistenceEnabled
        this.maxStoredEvents = maxStoredEvents.coerceAtLeast(1)
        initialized = true
        observeEvents("")
    }

    fun observeEvents(query: String) {
        ensureInitialized()
        activeQuery = query.trim()
        observeJob?.cancel()
        if (!persistenceEnabled) {
            emitInMemoryFiltered()
            return
        }

        val db = database ?: return
        observeJob = scope.launch {
            db.networkEventDao().trimToLatest(maxStoredEvents)
            val source = if (activeQuery.isBlank()) {
                db.networkEventDao().observeAll()
            } else {
                db.networkEventDao().observeByQuery(activeQuery)
            }

            source.collectLatest { list ->
                _events.value = list
            }
        }
    }

    fun addEvent(event: NetworkEvent) {
        ensureInitialized()
        if (!persistenceEnabled) {
            synchronized(inMemoryEvents) {
                inMemoryEvents.add(0, event)
                if (inMemoryEvents.size > maxStoredEvents) {
                    inMemoryEvents.subList(maxStoredEvents, inMemoryEvents.size).clear()
                }
            }
            emitInMemoryFiltered()
            return
        }

        val db = database ?: return
        scope.launch {
            db.networkEventDao().insert(event)
            db.networkEventDao().trimToLatest(maxStoredEvents)
        }
    }

    suspend fun clear() {
        ensureInitialized()
        if (!persistenceEnabled) {
            synchronized(inMemoryEvents) {
                inMemoryEvents.removeAll { !it.pinned }
            }
            emitInMemoryFiltered()
            return
        }
        database?.networkEventDao()?.clear()
    }

    fun clearAsync() {
        ensureInitialized()
        if (!persistenceEnabled) {
            synchronized(inMemoryEvents) {
                inMemoryEvents.removeAll { !it.pinned }
            }
            emitInMemoryFiltered()
            return
        }
        scope.launch {
            database?.networkEventDao()?.clear()
        }
    }

    fun setPinned(eventId: String, pinned: Boolean) {
        ensureInitialized()
        if (!persistenceEnabled) {
            synchronized(inMemoryEvents) {
                val index = inMemoryEvents.indexOfFirst { it.id == eventId }
                if (index >= 0) {
                    inMemoryEvents[index] = inMemoryEvents[index].copy(pinned = pinned)
                }
            }
            emitInMemoryFiltered()
            return
        }
        scope.launch {
            database?.networkEventDao()?.setPinned(eventId, pinned)
        }
    }

    suspend fun getEventById(eventId: String): NetworkEvent? {
        ensureInitialized()
        if (!persistenceEnabled) {
            return synchronized(inMemoryEvents) {
                inMemoryEvents.firstOrNull { it.id == eventId }
            }
        }
        return database?.networkEventDao()?.getEventById(eventId)
    }

    private fun ensureInitialized() {
        check(initialized) { "NILRepository is not initialized. Call NIL.initialize(context)." }
    }

    private fun emitInMemoryFiltered() {
        val snapshot = synchronized(inMemoryEvents) { inMemoryEvents.toList() }
        if (activeQuery.isBlank()) {
            _events.value = snapshot.map(NetworkEvent::toSummary)
            return
        }
        val q = activeQuery.lowercase()
        _events.value = snapshot
            .filter { event ->
                event.url.contains(q, ignoreCase = true) ||
                    event.method.contains(q, ignoreCase = true)
            }
            .map(NetworkEvent::toSummary)
    }
}

private fun NetworkEvent.toSummary(): NetworkEventSummary = NetworkEventSummary(
    id = id,
    url = url,
    method = method,
    statusCode = statusCode,
    durationMs = durationMs,
    timestamp = timestamp,
    pinned = pinned
)
