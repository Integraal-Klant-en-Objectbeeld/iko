package com.ritense.iko.mvc.provider

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser

internal object SecurityContextHelper {

    fun getUserPropertyByKey(key: String): String {
        val authentication = SecurityContextHolder.getContext().authentication
        val principal = authentication?.principal as OidcUser
        return principal.userInfo?.claims?.get(key).toString()
    }
}