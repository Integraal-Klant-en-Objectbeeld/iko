package com.ritense.iko.mvc

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Exposes authenticated user information as global model attributes so that
 */
@ControllerAdvice
internal class GlobalUserInfoAdvice {

    @ModelAttribute("username")
    fun username(@AuthenticationPrincipal principal: OidcUser?): String? {
        val claims = principal?.userInfo?.claims
        val username = claims?.get("preferred_username") as? String
        return if (!username.isNullOrBlank()) username else "Unknown username"
    }

    @ModelAttribute("email")
    fun email(@AuthenticationPrincipal principal: OidcUser?): String? {
        val claims = principal?.userInfo?.claims
        val email = claims?.get("email") as? String
        return if (!email.isNullOrBlank()) email else "Unknown email address"
    }

    @ModelAttribute("name")
    fun name(@AuthenticationPrincipal principal: OidcUser?): String? {
        val claims = principal?.userInfo?.claims
        val name = claims?.get("name") as? String
        return if (!name.isNullOrBlank()) name else "Unknown user"
    }
}
