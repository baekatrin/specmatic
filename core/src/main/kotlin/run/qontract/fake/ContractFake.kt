package run.qontract.fake

import io.ktor.application.ApplicationCall
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.http.*
import io.ktor.http.content.TextContent
import io.ktor.request.*
import io.ktor.response.respond
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.toMap
import kotlinx.coroutines.runBlocking
import run.qontract.core.ContractBehaviour
import run.qontract.core.HttpRequest
import run.qontract.core.HttpResponse
import run.qontract.core.pattern.parsedValue
import run.qontract.core.utilities.contractGherkinForCurrentComponent
import run.qontract.core.utilities.getContractGherkin
import run.qontract.core.utilities.toMap
import run.qontract.core.value.EmptyString
import run.qontract.core.value.Value
import run.qontract.mock.MockScenario
import run.qontract.mock.matchesRequest
import java.io.Closeable
import java.util.*

class ContractFake(gherkinData: String, stubInfo: List<MockScenario> = emptyList(), host: String = "localhost", port: Int = 9000) : Closeable {
    val endPoint = "http://$host:$port"

    private val contractBehaviour = ContractBehaviour(gherkinData)
    private val expectations = stubInfo.map { expectation ->
        Pair(expectation.request, contractBehaviour.matchingMockResponse(expectation))
    }

    private val server: ApplicationEngine = embeddedServer(Netty, port) {
        install(CORS) {
            method(HttpMethod.Options)
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Put)
            method(HttpMethod.Delete)
            method(HttpMethod.Patch)
            header(HttpHeaders.Authorization)
            allowCredentials = true
            anyHost()
        }

        intercept(ApplicationCallPipeline.Call) {
            val httpRequest = ktorHttpRequestToHttpRequest(call)

            if (isSetupRequest(httpRequest)) {
                setupServerState(httpRequest)
                call.response.status(HttpStatusCode.OK)
            } else {
                when(val mock = expectations.find {
                    matchesRequest(httpRequest, it.first)
                }) {
                    null -> respondToKtorHttpResponse(call, contractBehaviour.lookup(httpRequest))
                    else -> respondToKtorHttpResponse(call, mock.second)
                }
            }
        }
    }

    override fun close() {
        server.stop(0, 5000)
    }

    companion object {
        @Throws(Throwable::class)
        fun forSupportedContract(): ContractFake {
            val gherkin = contractGherkinForCurrentComponent
            return ContractFake(gherkin, emptyList(), "localhost", 8080)
        }

        @JvmOverloads
        @Throws(Throwable::class)
        fun forService(serviceName: String?, host: String = "127.00.1", port: Int = 8080): ContractFake {
            val contractGherkin = getContractGherkin(serviceName!!)
            return ContractFake(contractGherkin, emptyList(), host, port)
        }
    }

    private fun setupServerState(httpRequest: HttpRequest) {
        val body = httpRequest.body
        contractBehaviour.setServerState(body?.let { toMap(it) } ?: mutableMapOf())
    }

    private fun isSetupRequest(httpRequest: HttpRequest): Boolean {
        return httpRequest.path == "/_server_state" && httpRequest.method == "POST"
    }

    init {
        server.start()
    }
}

internal suspend fun ktorHttpRequestToHttpRequest(call: ApplicationCall): HttpRequest {
    val(body, formFields) = bodyFromCall(call)

    val requestHeaders = HashMap(call.request.headers.toMap().mapValues { it.value[0] })

    return HttpRequest(method = call.request.httpMethod.value,
            path = call.request.path(),
            headers = requestHeaders,
            body = body,
            queryParams = toParams(call.request.queryParameters),
            formFields = formFields)
}

private suspend fun bodyFromCall(call: ApplicationCall): Pair<Value, Map<String, String>> {
    return if (call.request.contentType().match(ContentType.Application.FormUrlEncoded))
        Pair(EmptyString, call.receiveParameters().toMap().mapValues { (_, values) -> values.first() })
    else
        Pair(parsedValue(call.receiveText()), emptyMap())
}

internal fun toParams(queryParameters: Parameters) = HashMap(queryParameters.toMap().mapValues { it.value.first() })

internal fun respondToKtorHttpResponse(call: ApplicationCall, httpResponse: HttpResponse) {
    val textContent = TextContent(httpResponse.body as String, ContentType.Application.Json, HttpStatusCode.fromValue(httpResponse.status))

    try {
        val headersControlledByEngine = listOf("content-type", "content-length")
        for ((name, value) in httpResponse.headers.filterNot { it.key.toLowerCase() in headersControlledByEngine }) {
            call.response.headers.append(name, value)
        }

        runBlocking { call.respond(textContent) }
    }
    catch(e:Exception)
    {
        print(e.toString())
    }
}
