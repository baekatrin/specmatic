package `in`.specmatic.conversions

import `in`.specmatic.core.SecuritySchemeConfiguration
import `in`.specmatic.core.SecuritySchemeWithOAuthToken
import `in`.specmatic.core.pattern.ContractException
import io.swagger.v3.oas.models.security.SecurityScheme

data class BearerSecurityToken(
    private val type: String,
    private val securitySchemeConfiguration: SecuritySchemeConfiguration?,
    private val environmentVariable: String,
    private val environment: Environment = DefaultEnvironment()
) :
    SecurityToken {
    override fun resolve(): String? {
        return when (type) {
            BEARER_SECURITY_SCHEME, SecurityScheme.Type.OAUTH2.toString() ->
                securitySchemeConfiguration?.let {
                    (it as SecuritySchemeWithOAuthToken).token
                } ?: environment.getEnvironmentVariable(environmentVariable)

            else -> throw ContractException("Cannot use the Bearer Security Scheme implementation for scheme type: $type")
        }
    }
}