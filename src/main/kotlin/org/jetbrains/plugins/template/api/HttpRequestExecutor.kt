package org.jetbrains.plugins.template.api

import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun interface HttpRequestExecutor {
    fun execute(request: HttpRequest): HttpResponse<String>
}
