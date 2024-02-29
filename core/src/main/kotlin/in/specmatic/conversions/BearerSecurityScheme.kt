package `in`.specmatic.conversions

import `in`.specmatic.core.*
import `in`.specmatic.core.pattern.Pattern
import `in`.specmatic.core.pattern.Row
import `in`.specmatic.core.pattern.StringPattern
import org.apache.http.HttpHeaders.AUTHORIZATION

data class BearerSecurityScheme(private val configuredToken: String? = null) : OpenAPISecurityScheme {
    override fun matches(httpRequest: HttpRequest): Result {
        httpRequest.headers.let {
            val authHeaderValue: String = it[AUTHORIZATION]
                ?: return Result.Failure("$AUTHORIZATION header is missing in request")

            if (!authHeaderValue.lowercase().startsWith("bearer"))
                return Result.Failure("$AUTHORIZATION header must be prefixed with \"Bearer\"")
        }
        return Result.Success()
    }

    override fun removeParam(httpRequest: HttpRequest): HttpRequest {
        return httpRequest.copy(headers = httpRequest.headers.minus(AUTHORIZATION))
    }

    override fun addTo(httpRequest: HttpRequest): HttpRequest {
        return httpRequest.copy(
            headers = httpRequest.headers.plus(
                AUTHORIZATION to getAuthorizationHeaderValue()
            )
        )
    }

    override fun addTo(requestPattern: HttpRequestPattern, row: Row): HttpRequestPattern {
        return addToHeaderType(AUTHORIZATION, row, requestPattern)
    }

    override fun isInRow(row: Row): Boolean = row.containsField(AUTHORIZATION)
    override fun headerInRequest(request: HttpRequest, resolver: Resolver): Map<String, Pattern> {
        return headerPatternFromRequest(request, AUTHORIZATION)
    }

    override fun queryInRequest(request: HttpRequest, resolver: Resolver): Map<String, Pattern> {
        return emptyMap()
    }

    private fun getAuthorizationHeaderValue(): String {
        return "Bearer " + (configuredToken ?: StringPattern().generate(Resolver()).toStringLiteral())
    }
}
