package com.ritense.iko.mvc.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Global MVC advice to expose commonly used model attributes.
 *
 * Provides the currently authenticated user's "username" derived from the OIDC UserInfo endpoint
 * (same source as /admin/api/v1/auth/userinfo), preferring the `preferred_username` claim and
 * falling back to `name` and then `email` when not available.
 */
@ControllerAdvice
internal class GlobalUserInfoAdvice {

    @ModelAttribute("username")
    fun username(@AuthenticationPrincipal principal: OidcUser?): String? {
        val claims = principal?.userInfo?.claims ?: return null
        val name = claims["name"] as? String
        return if (!name.isNullOrBlank()) name else "Unknown user"
    }
}
