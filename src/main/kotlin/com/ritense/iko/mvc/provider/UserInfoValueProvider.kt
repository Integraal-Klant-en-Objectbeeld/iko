package com.ritense.iko.mvc.provider

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser

/**
 * Static-style helper to resolve the current user's display name from OIDC claims.
 *
 * This can be called from companion objects or anywhere without Spring DI.
 */
internal object UserInfoValueProvider {

    /**
     * Returns the first non-blank value among the provided claim names from the current authenticated OIDC user.
     * If no value is found or the user is unauthenticated, returns "Unknown".
     */
    fun getCurrentUserInfoValue(vararg claimNames: String): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as? OidcUser
        val claims = principal?.userInfo?.claims ?: return "Unknown"
        for (name in claimNames) {
            val value = claims[name]
            val asString = when (value) {
                is String -> value
                is Collection<*> -> value.filterIsInstance<String>().joinToString(" ")
                    .takeIf { it.isNotBlank() }
                else -> value?.toString()
            }
            if (!asString.isNullOrBlank()) return asString
        }
        return "Unknown"
    }
}