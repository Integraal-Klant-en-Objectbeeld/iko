package com.ritense.iko.mvc.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/api/v1/auth")
internal class AuthClaimsController {

    @GetMapping("/userinfo")
    fun userInfo(@AuthenticationPrincipal principal: OidcUser): Map<String?, Any?>? {
        val userInfo = principal.userInfo
        return userInfo?.claims
    }
}
