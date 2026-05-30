package com.sandesh.nil.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Http
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandesh.nil.core.NIL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NIL.initialize(
            context = applicationContext,
            enableFloatingButton = true,
            inspectorPayloadCharLimit = 200_000,
            maxStoredEvents = 100,
            persistenceEnabled = true
        )

        setContent {
            SampleHostScreen()
        }
    }
}

@Composable
private fun SampleHostScreen() {
    val scope = rememberCoroutineScope()
    val client = remember {
        OkHttpClient.Builder()
            .addInterceptor(NIL.interceptor())
            .addInterceptor(SampleMockInterceptor())
            .build()
    }
    val retrofitApi = remember {
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SampleApi::class.java)
    }

    var statusText by remember { mutableStateOf("No calls yet") }



    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Top) {
        Text("NIL Sample App", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                statusText = runRetrofitCall(retrofitApi)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Api,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retrofit Call")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            statusText = runOkHttpCall(client)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Http,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OkHttp Call")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            statusText = runHttpUrlConnectionCall()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Filled.Http,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("HttpURLConnection Call")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                statusText = runMockHtmlBadGateway(client)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Error, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mock HTML 502")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                statusText = runMockNestedJson(client)
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Code, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mock Long JSON")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        scope.launch {
                            statusText = runFailedCall(client)
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.BugReport, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mock Failed Call")
                }

                Spacer(modifier = Modifier.height(12.dp))

            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(statusText, style = MaterialTheme.typography.bodyMedium)
    }
}

private suspend fun runMockHtmlBadGateway(client: OkHttpClient): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://sample.nil.mock/mock/html-502")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                "Mock HTML: HTTP ${response.code}"
            }
        }.getOrElse { throwable ->
            "Mock HTML failed: ${throwable.message}"
        }
    }
}

private suspend fun runMockNestedJson(client: OkHttpClient): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://sample.nil.mock/mock/nested-json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                "Mock JSON: HTTP ${response.code}"
            }
        }.getOrElse { throwable ->
            "Mock JSON failed: ${throwable.message}"
        }
    }
}

private suspend fun runFailedCall(client: OkHttpClient): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://sample.nil.mock/mock/failure")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                "Failure mock returned HTTP ${response.code}"
            }
        }.getOrElse { throwable ->
            "Mock failure: ${throwable.message}"
        }
    }
}

private suspend fun runRetrofitCall(api: SampleApi): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val response: Response<Post> = api.getPost()
            "Retrofit: HTTP ${response.code()} ${response.body()?.title.orEmpty()}"
        }.getOrElse { throwable ->
            "Retrofit failed: ${throwable.message}"
        }
    }
}

private suspend fun runOkHttpCall(client: OkHttpClient): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://httpbin.org/get?from=okhttp")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                "OkHttp: HTTP ${response.code}"
            }
        }.getOrElse { throwable ->
            "OkHttp failed: ${throwable.message}"
        }
    }
}

private suspend fun runHttpUrlConnectionCall(): String {
    return withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL("https://httpbin.org/get?from=http-url-connection").openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
            }

            val responseBody = NIL.interceptor(NIL.InterceptorType.HTTP_URL_CONNECTION).intercept(
                connection = connection,
                execute = { conn ->
                    conn.inputStream.bufferedReader().use { reader -> reader.readText() }
                },
                responseBodyExtractor = { it }
            )

            "HttpURLConnection: HTTP ${connection.responseCode} body=${responseBody.length} chars"
        }.getOrElse { throwable ->
            "HttpURLConnection failed: ${throwable.message}"
        }
    }
}

private interface SampleApi {
    @GET("posts/1")
    suspend fun getPost(): Response<Post>
}

private data class Post(
    val id: Int,
    val title: String
)

private class SampleMockInterceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): okhttp3.Response {
        val request = chain.request()
        val path = request.url.encodedPath

        if (path == "/mock/failure") {
            throw java.io.IOException("Simulated network failure")
        }

        if (path == "/mock/html-502") {
            val html = """
                <!doctype html>
                <html>
                  <body>
                    <h1>502 Bad Gateway</h1>
                    <p>Upstream timeout from mock edge.</p>
                  </body>
                </html>
            """.trimIndent()
            return okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(502)
                .message("Bad Gateway")
                .header("Content-Type", "text/html; charset=utf-8")
                .body(html.toResponseBody("text/html; charset=utf-8".toMediaType()))
                .build()
        }

        if (path == "/mock/nested-json") {
            val json = buildNestedJson(depth = 18)
            return okhttp3.Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header("Content-Type", "application/json")
                .body(json.toResponseBody("application/json; charset=utf-8".toMediaType()))
                .build()
        }

        return chain.proceed(request)
    }
}

private fun buildNestedJson(depth: Int): String {
    val leaf = """
        {
          "status": "ok",
          "items": [
            {"id": 1, "name": "alpha"},
            {"id": 2, "name": "beta"},
            {"id": 3, "name": "gamma"}
          ]
        }
    """.trimIndent()

    return (depth downTo 1).fold(leaf) { acc, level ->
        """{"level_$level":{"meta":{"path":"root.level_$level"},"responses":[{"id":"resp_${level}_a","status":"ok","metrics":{"latencyMs":${level * 7},"cacheHit":${level % 2 == 0}}},{"id":"resp_${level}_b","status":"partial","tags":["nested","array","level_$level"],"checks":[{"name":"schema","passed":true},{"name":"limits","passed":${level % 3 != 0}}]}],"child":$acc}}"""
    }
}
