/**
 * Created by Sandesh Yele on 16/05/26.
 */

package com.sandesh.nil.ui.inspector.detail

import android.net.Uri
import androidx.compose.material.icons.filled.Code
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sandesh.nil.core.NIL
import com.sandesh.nil.model.NetworkEvent
import com.sandesh.nil.utils.CurlGenerator
import com.sandesh.nil.utils.ShareFileUtil
import com.sandesh.nil.utils.XhrGenerator
import androidx.compose.ui.platform.LocalContext

@Composable
fun EventDetailScreen(
    event: NetworkEvent,
    onBack: () -> Unit,
    onAnalyse: (title: String, payload: String) -> Unit,
    modifier: Modifier
) {
    var requestHeadersExpanded by rememberSaveable { mutableStateOf(false) }
    var requestParamsExpanded by rememberSaveable { mutableStateOf(false) }
    var requestBodyExpanded by rememberSaveable { mutableStateOf(true) }
    var responseHeadersExpanded by rememberSaveable { mutableStateOf(false) }
    var responseBodyExpanded by rememberSaveable { mutableStateOf(true) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val requestParams = remember(event.url) { parseRequestParams(event.url) }
    val requestHeaderPairs = remember(event.requestHeaders) { parseHeaderPairs(event.requestHeaders) }
    val responseHeaderPairs = remember(event.responseHeaders) { parseHeaderPairs(event.responseHeaders) }
    val payloadCharLimit = NIL.inspectorPayloadCharLimit()
    val requestBodySearchEnabled = (event.requestBody?.length ?: 0) <= payloadCharLimit
    val responseBodySearchEnabled = (event.responseBody?.length ?: 0) <= payloadCharLimit

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Text(
                text = "Event Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = event.url,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(CurlGenerator.fromEvent(event))) }
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy cURL" ,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy cURL")
                }
                OutlinedButton(
                    onClick = {
                        ShareFileUtil.shareTextFile(
                            context = context,
                            fileName = "request_${event.id}.xhr.js",
                            content = XhrGenerator.fromEvent(event),
                            mimeType = "application/javascript"
                        )
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Export XHR",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export XHR")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    CollapsibleSection(
                        title = "Request Headers",
                        expanded = requestHeadersExpanded,
                        onToggle = { requestHeadersExpanded = !requestHeadersExpanded },
                    ) {
                        SectionContent(
                            onSearch = {
                                onAnalyse("Request Headers", toKeyValueText(requestHeaderPairs))
                            },
                            onShare = {
                                ShareFileUtil.shareTextFile(
                                    context = context,
                                    fileName = "request_headers_${event.id}.txt",
                                    content = toKeyValueText(requestHeaderPairs),
                                    mimeType = "text/plain"
                                )
                            }
                        ) {
                            KeyValueList(
                                pairs = requestHeaderPairs,
                                emptyLabel = "No request headers"
                            )
                        }
                    }
                }
                item {
                    CollapsibleSection(
                        title = "Request Params",
                        expanded = requestParamsExpanded,
                        onToggle = { requestParamsExpanded = !requestParamsExpanded },
                    ) {
                        SectionContent(
                            onSearch = {
                                onAnalyse("Request Params", toKeyValueText(requestParams))
                            },
                            onShare = {
                                ShareFileUtil.shareTextFile(
                                    context = context,
                                    fileName = "request_params_${event.id}.txt",
                                    content = toKeyValueText(requestParams),
                                    mimeType = "text/plain"
                                )
                            }
                        ) {
                            KeyValueList(
                                pairs = requestParams,
                                emptyLabel = "No query params"
                            )
                        }
                    }
                }
                item {
                    CollapsibleSection(
                        title = "Request Body",
                        expanded = requestBodyExpanded,
                        onToggle = { requestBodyExpanded = !requestBodyExpanded }
                    ) {
                        SectionContent(
                            onSearch = {
                                onAnalyse("Request Body", event.requestBody.orEmpty())
                            },
                            searchEnabled = requestBodySearchEnabled,
                            onShare = {
                                ShareFileUtil.shareTextFile(
                                    context = context,
                                    fileName = "request_body_${event.id}.txt",
                                    content = event.requestBody.orEmpty(),
                                    mimeType = "text/plain"
                                )
                            }
                        ) {
                            BodyViewer(
                                body = event.requestBody,
                                headers = event.requestHeaders
                            )
                        }
                    }
                }
                item {
                    CollapsibleSection(
                        title = "Response Headers",
                        expanded = responseHeadersExpanded,
                        onToggle = { responseHeadersExpanded = !responseHeadersExpanded },
                    ) {
                        SectionContent(
                            onSearch = {
                                onAnalyse("Response Headers", toKeyValueText(responseHeaderPairs))
                            },
                            onShare = {
                                ShareFileUtil.shareTextFile(
                                    context = context,
                                    fileName = "response_headers_${event.id}.txt",
                                    content = toKeyValueText(responseHeaderPairs),
                                    mimeType = "text/plain"
                                )
                            }
                        ) {
                            KeyValueList(
                                pairs = responseHeaderPairs,
                                emptyLabel = "No response headers"
                            )
                        }
                    }
                }
                item {
                    CollapsibleSection(
                        title = "Response Body",
                        expanded = responseBodyExpanded,
                        onToggle = { responseBodyExpanded = !responseBodyExpanded }
                    ) {
                        SectionContent(
                            onSearch = {
                                onAnalyse("Response Body", event.responseBody.orEmpty())
                            },
                            searchEnabled = responseBodySearchEnabled,
                            onShare = {
                                ShareFileUtil.shareTextFile(
                                    context = context,
                                    fileName = "response_body_${event.id}.txt",
                                    content = event.responseBody.orEmpty(),
                                    mimeType = "text/plain"
                                )
                            }
                        ) {
                            BodyViewer(
                                body = event.responseBody,
                                headers = event.responseHeaders
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun parseRequestParams(url: String): List<DetailPair> {
    val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return emptyList()
    val names = uri.queryParameterNames
    if (names.isEmpty()) return emptyList()

    return buildList {
        names.sorted().forEach { key ->
            val values = uri.getQueryParameters(key)
            if (values.isEmpty()) {
                add(DetailPair(key, ""))
            } else {
                values.forEach { value ->
                    add(DetailPair(key, value))
                }
            }
        }
    }
}

private fun parseHeaderPairs(headers: String?): List<DetailPair> {
    return headers
        .orEmpty()
        .lineSequence()
        .mapNotNull { line ->
            val splitIndex = line.indexOf(':')
            if (splitIndex <= 0) return@mapNotNull null
            val key = line.substring(0, splitIndex).trim()
            val value = line.substring(splitIndex + 1).trim()
            DetailPair(key = key, value = value)
        }
        .toList()
}
