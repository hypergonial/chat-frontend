package com.hypergonial.chat.model

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun platformHttpClient(config: HttpClientConfig<*>.() -> Unit) = HttpClient(Darwin) { config(this) }
