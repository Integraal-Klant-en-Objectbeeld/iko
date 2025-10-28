package com.ritense.iko.mvc.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * REST endpoints exposing OIDC claims for the currently authenticated user.
 *
 * - /admin/api/v1/auth/userinfo: Only the UserInfo endpoint claims (if available)
 * - /admin/api/v1/auth/claims: Merged claims (ID Token + UserInfo, UserInfo overrides on conflicts)
 */
@RestController
@RequestMapping("/admin/api/v1/auth")
internal class AuthClaimsController {

    @GetMapping("/userinfo")
    fun userInfo(@AuthenticationPrincipal principal: OidcUser?): Map<String, Any?> {
        val userInfo = principal?.userInfo
        return userInfo?.claims ?: emptyMap()
    }
}
