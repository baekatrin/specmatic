package `in`.specmatic.conversions

import `in`.specmatic.core.APIKeySecuritySchemeConfiguration
import `in`.specmatic.core.BasicAuthSecuritySchemeConfiguration
import `in`.specmatic.core.SecuritySchemeConfiguration
import `in`.specmatic.core.SecuritySchemeWithOAuthToken

@Deprecated("This will be deprecated shortly.Use the security scheme name as the environment variable.")
const val SPECMATIC_OAUTH2_TOKEN = "SPECMATIC_OAUTH2_TOKEN"

const val SPECMATIC_BASIC_AUTH_TOKEN = "SPECMATIC_BASIC_AUTH_TOKEN"

fun getSecurityTokenForBasicAuthScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environmentAndPropertiesConfiguration: EnvironmentAndPropertiesConfiguration
): String? {
    return environmentAndPropertiesConfiguration.getCachedSetting(environmentVariable) ?: securitySchemeConfiguration?.let {
        (securitySchemeConfiguration as BasicAuthSecuritySchemeConfiguration).token
    } ?: environmentAndPropertiesConfiguration.getCachedEnvironmentVariable(SPECMATIC_BASIC_AUTH_TOKEN)
}

fun getSecurityTokenForBearerScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environmentAndPropertiesConfiguration: EnvironmentAndPropertiesConfiguration
): String? {
    return environmentAndPropertiesConfiguration.getCachedSetting(environmentVariable) ?: securitySchemeConfiguration?.let {
        (it as SecuritySchemeWithOAuthToken).token
    } ?: environmentAndPropertiesConfiguration.getCachedEnvironmentVariable(SPECMATIC_OAUTH2_TOKEN)
}

fun getSecurityTokenForApiKeyScheme(
    securitySchemeConfiguration: SecuritySchemeConfiguration?,
    environmentVariable: String,
    environmentAndPropertiesConfiguration: EnvironmentAndPropertiesConfiguration
): String? {
    return environmentAndPropertiesConfiguration.getCachedSetting(environmentVariable) ?: securitySchemeConfiguration?.let {
        (it as APIKeySecuritySchemeConfiguration).value
    }
}