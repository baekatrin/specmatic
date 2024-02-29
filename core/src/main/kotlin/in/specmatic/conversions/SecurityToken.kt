package `in`.specmatic.conversions

import `in`.specmatic.core.APIKeySecuritySchemeConfiguration
import `in`.specmatic.core.SecuritySchemeConfiguration
import `in`.specmatic.core.SecuritySchemeWithOAuthToken

@Deprecated("This will be deprecated shortly.Use the security scheme name as the environment variable.")
const val SPECMATIC_OAUTH2_TOKEN = "SPECMATIC_OAUTH2_TOKEN"

const val SPECMATIC_BASIC_AUTH_TOKEN = "SPECMATIC_BASIC_AUTH_TOKEN"

fun getSecurityTokenForBasicAuthScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environment: Environment
): String? {
    return environment.getEnvironmentVariable(environmentVariable) ?: environment.getEnvironmentVariable(SPECMATIC_BASIC_AUTH_TOKEN)
}

fun getSecurityTokenForBearerScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environment: Environment
): String? {
    return environment.getEnvironmentVariable(environmentVariable) ?: securitySchemeConfiguration?.let {
        (it as SecuritySchemeWithOAuthToken).token
    } ?: environment.getEnvironmentVariable(SPECMATIC_OAUTH2_TOKEN)
}

fun getSecurityTokenForApiKeyScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environment: Environment
): String? {
    return environment.getEnvironmentVariable(environmentVariable) ?: securitySchemeConfiguration?.let {
        (it as APIKeySecuritySchemeConfiguration).value
    }
}