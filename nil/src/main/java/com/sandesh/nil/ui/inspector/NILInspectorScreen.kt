/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandesh.nil.core.NIL
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.ui.inspector.detail.DetailLoadingState
import com.sandesh.nil.ui.inspector.detail.EventDetailScreen
import com.sandesh.nil.ui.inspector.list.EventListScreen
import com.sandesh.nil.ui.inspector.search.BodySearchScreen

@Composable
fun NILInspectorScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {

    val events by NIL.events.collectAsStateWithLifecycle()

    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEvent by remember { mutableStateOf<NetworkEvent?>(null) }
    var isLoadingSelectedEvent by remember { mutableStateOf(false) }
    var analyseTitle by remember { mutableStateOf<String?>(null) }
    var analysePayload by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedId) {
        if (selectedId == null) {
            selectedEvent = null
            isLoadingSelectedEvent = false
            return@LaunchedEffect
        }

        isLoadingSelectedEvent = true
        selectedEvent = NIL.getEventById(selectedId.orEmpty())
        isLoadingSelectedEvent = false
        if (selectedEvent == null) {
            selectedId = null
        }
    }

    when {
        analyseTitle != null && analysePayload != null -> {
            BodySearchScreen(
                title = analyseTitle.orEmpty(),
                body = analysePayload.orEmpty(),
                onBack = {
                    analyseTitle = null
                    analysePayload = null
                },
                modifier = modifier
            )
        }

        selectedEvent != null -> {
            EventDetailScreen(
                event = selectedEvent!!,
                onBack = {
                    selectedId = null
                    selectedEvent = null
                },
                onAnalyse = { title, payload ->
                    analyseTitle = title
                    analysePayload = payload
                },
                modifier = modifier
            )
        }

        isLoadingSelectedEvent -> {
            DetailLoadingState(label = "Loading event details...")
        }

        else -> {
            EventListScreen(
                events = events,
                onClick = { selectedId = it.id },
                onBack = onBack,
                modifier = modifier
            )
        }
    }
}
