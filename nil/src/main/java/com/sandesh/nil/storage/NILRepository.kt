package com.sandesh.nil.storage

import com.sandesh.nil.database.NILDatabase
import com.sandesh.nil.model.NetworkEvent
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
    private const val UNBOUNDED_WINDOW = Int.MAX_VALUE

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private var database: NILDatabase? = null
    private var observeJob: Job? = null
    private val _events = MutableStateFlow<List<NetworkEvent>>(emptyList())
    val events: StateFlow<List<NetworkEvent>> = _events.asStateFlow()
    private val inMemoryEvents = mutableListOf<NetworkEvent>()
    private var currentQuery: String = ""

    @Volatile
    private var initialized: Boolean = false
    @Volatile
    private var disablePersistence: Boolean = false
    @Volatile
    private var requestWindowSize: Int = UNBOUNDED_WINDOW

    @Synchronized
    fun initialize(db: NILDatabase?) {
        if (initialized) return
        database = db
        initialized = true
        observeEvents("")
    }

    fun setDatabase(db: NILDatabase) {
        database = db
    }

    fun configure(disablePersistence: Boolean, requestWindowSize: Int) {
        val normalizedWindow = requestWindowSize.takeIf { it > 0 } ?: UNBOUNDED_WINDOW
        val modeChanged = this.disablePersistence != disablePersistence
        this.disablePersistence = disablePersistence
        this.requestWindowSize = normalizedWindow
        if (!initialized) return

        if (modeChanged && disablePersistence) {
            observeJob?.cancel()
            synchronized(lock) {
                inMemoryEvents.clear()
                inMemoryEvents.addAll(_events.value)
                trimInMemoryIfNeededLocked()
            }
            publishInMemory()
            return
        }

        if (modeChanged && !disablePersistence) {
            observeEvents(currentQuery)
            return
        }

        if (disablePersistence) {
            synchronized(lock) {
                trimInMemoryIfNeededLocked()
            }
            publishInMemory()
            return
        }

        scope.launch {
            prunePersistedIfNeeded()
        }
    }

    fun observeEvents(query: String) {
        ensureInitialized()
        currentQuery = query.trim()
        if (disablePersistence) {
            observeJob?.cancel()
            publishInMemory()
            return
        }
        observeJob?.cancel()
        observeJob = scope.launch {
            val db = requireNotNull(database) { "Persistence requires a configured database." }
            val source = if (currentQuery.isBlank()) {
                db.networkEventDao().observeAll()
            } else {
                db.networkEventDao().observeByQuery(currentQuery)
            }

            source.collectLatest { list ->
                _events.value = list
            }
        }
    }

    fun addEvent(event: NetworkEvent) {
        ensureInitialized()
        if (disablePersistence) {
            synchronized(lock) {
                inMemoryEvents.add(0, event)
                trimInMemoryIfNeededLocked()
            }
            publishInMemory()
            return
        }
        scope.launch {
            val db = requireNotNull(database) { "Persistence requires a configured database." }
            db.networkEventDao().insert(event)
            prunePersistedIfNeeded()
        }
    }

    suspend fun clear() {
        ensureInitialized()
        if (disablePersistence) {
            synchronized(lock) {
                inMemoryEvents.clear()
            }
            publishInMemory()
            return
        }
        requireNotNull(database) { "Persistence requires a configured database." }
            .networkEventDao()
            .clear()
    }

    fun clearAsync() {
        ensureInitialized()
        if (disablePersistence) {
            synchronized(lock) {
                inMemoryEvents.clear()
            }
            publishInMemory()
            return
        }
        scope.launch {
            requireNotNull(database) { "Persistence requires a configured database." }
                .networkEventDao()
                .clear()
        }
    }

    fun setPinned(eventId: String, pinned: Boolean) {
        ensureInitialized()
        if (disablePersistence) {
            synchronized(lock) {
                val index = inMemoryEvents.indexOfFirst { it.id == eventId }
                if (index >= 0) {
                    inMemoryEvents[index] = inMemoryEvents[index].copy(pinned = pinned)
                }
            }
            publishInMemory()
            return
        }
        scope.launch {
            requireNotNull(database) { "Persistence requires a configured database." }
                .networkEventDao()
                .setPinned(eventId, pinned)
        }
    }

    private fun ensureInitialized() {
        check(initialized) { "NILRepository is not initialized. Call NIL.initialize(context)." }
    }

    private fun publishInMemory() {
        val snapshot = synchronized(lock) {
            if (currentQuery.isBlank()) {
                inMemoryEvents.toList()
            } else {
                val needle = currentQuery.lowercase()
                inMemoryEvents.filter {
                    it.url.contains(needle, ignoreCase = true) ||
                        it.method.contains(needle, ignoreCase = true) ||
                        (it.requestBody?.contains(needle, ignoreCase = true) == true) ||
                        (it.responseBody?.contains(needle, ignoreCase = true) == true)
                }
            }
        }
        _events.value = snapshot
    }

    private suspend fun prunePersistedIfNeeded() {
        if (requestWindowSize == UNBOUNDED_WINDOW) return
        val dao = requireNotNull(database) { "Persistence requires a configured database." }.networkEventDao()
        val overflow = dao.countUnpinned() - requestWindowSize
        if (overflow > 0) {
            dao.deleteOldestUnpinned(overflow)
        }
    }

    private fun trimInMemoryIfNeededLocked() {
        if (requestWindowSize == UNBOUNDED_WINDOW) return
        var overflow = inMemoryEvents.count { !it.pinned } - requestWindowSize
        if (overflow <= 0) return
        val iterator = inMemoryEvents.listIterator(inMemoryEvents.size)
        while (iterator.hasPrevious() && overflow > 0) {
            if (!iterator.previous().pinned) {
                iterator.remove()
                overflow -= 1
            }
        }
    }
}
