package io.vertx.example

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.http.HttpClientRequest
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


private suspend fun HttpClientRequest.sendJsonObjectAwait(jsonObject: JsonObject): HttpClientResponse =
    sendJsonStringAwait(jsonObject.encode())


private suspend fun HttpClientRequest.sendJsonStringAwait(jsonString: String? = null): HttpClientResponse =
    suspendCoroutine { cont ->
        this.putHeader("content-type", "application/json")
            .handler { response ->
                cont.resume(response)
                //response.bodyHandler { buf -> cont.resume(buf.toJsonObject()) }
            }.exceptionHandler { ex ->
                cont.resumeWithException(ex)
            }
        if (jsonString == null) this.end() else this.end(jsonString)
    }

private suspend fun HttpClientResponse.bodyAsJsonObject(): JsonObject = suspendCoroutine { cont ->
    bodyHandler {
        cont.resume(it.toJsonObject())
    }.exceptionHandler {
        cont.resumeWithException(it)
    }
}

suspend fun HttpClientResponse.bodyAsString(): String = suspendCoroutine { cont ->
    bodyHandler {
        cont.resume(it.toString())
    }.exceptionHandler {
        cont.resumeWithException(it)
    }
}


@Suppress("unused")
class MainVerticle : AbstractVerticle() {

    override fun start(startFuture: Future<Void>) {
        val router = createRouter()

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(config().getInteger("http.port", 1818)) { result ->
                    if (result.succeeded()) {
                        startFuture.complete()
                    } else {
                        startFuture.fail(result.cause())
                    }
                }
        val httpClient = vertx.createHttpClient()
        GlobalScope.launch {
            val jsonString = """
              {"credential":{"id":"4053155660","secretType":"token","secret":"4053155660-ZYWkoV0vJ71L1xGalfYqZoqoPa7k8GbxmPpGor7,QcqIgq0cyvo2xqkX1PPyYkT0sZJ5FnqdqT2SeLasrN2rq"},"profile":{"name":"mesud ozil","photoUrl":"https://pbs.twimg.com/profile_images/679210356661587968/--ZRDFfm_normal.jpg","followersCount":16,"followingCount":47},"type":"twitter","enabled":true,"isEnabled":true}
            """.trimIndent()
            val request = httpClient.postAbs("http://socialhub.dev.mailtarget.id:3000/accounts").sendJsonStringAwait(jsonString)
            println("Request to  http://socialhub.dev.mailtarget.id:3000/accounts")
            println(request.bodyAsString())
        }
    }

    private fun createRouter() = Router.router(vertx).apply {
        get("/").handler(handlerRoot)
        get("/islands").handler(handlerIslands)
        get("/countries").handler(handlerCountries)
    }

    //
    // Handlers

    val handlerRoot = Handler<RoutingContext> { req ->
        req.response().end("Welcome!")
    }

    val handlerIslands = Handler<RoutingContext> { req ->
        req.response().endWithJson(MOCK_ISLANDS)
    }

    val handlerCountries = Handler<RoutingContext> { req ->
        req.response().endWithJson(MOCK_ISLANDS.map { it.country }.distinct().sortedBy { it.code })
    }

    //
    // Mock data

    private val MOCK_ISLANDS by lazy {
        listOf(
                Island("Kotlin", Country("Russia", "RU")),
                Island("Stewart Island", Country("New Zealand", "NZ")),
                Island("Cockatoo Island", Country("Australia", "AU")),
                Island("Tasmania", Country("Australia", "AU"))
        )
    }

    //
    // Utilities

    /**
     * Extension to the HTTP response to output JSON objects.
     */
    fun HttpServerResponse.endWithJson(obj: Any) {
        this.putHeader("Content-Type", "application/json; charset=utf-8").end(Json.encodePrettily(obj))
    }
}
