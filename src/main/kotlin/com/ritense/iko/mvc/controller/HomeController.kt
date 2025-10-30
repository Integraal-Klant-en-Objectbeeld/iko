package com.ritense.iko.mvc.controller

import com.ritense.iko.mvc.controller.ConnectorController.Companion.hxRequest
import com.ritense.iko.mvc.model.MenuItem
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.ModelAndView

/**
 * HomeController renders the top‑level admin page and exposes the set of
 * navigation items.  We replicate the original controller here and add a
 * new navigation entry for “Connectors”.
 */
@Controller
@RequestMapping("/admin")
class HomeController {

    @GetMapping
    fun home(
        @RequestHeader(HX_REQUEST_HEADER) isHxRequest: Boolean = false
    ): ModelAndView {
        return hxRequest(isHxRequest, "layout-internal", "details", mapOf())
    }

    companion object {
        const val HX_REQUEST_HEADER = "Hx-Request"
        const val PAGE_DEFAULT = 10
        const val BASE_FRAGMENT_ADG = "fragments/internal/aggregated-data-profile"
        const val BASE_FRAGMENT_RELATION = "fragments/internal/relation"

        /**
         * Entries used to populate the sidebar navigation.  A new “Connectors” item
         * has been added which links to the connectors overview page.
         */
        val menuItems: List<MenuItem> = listOf(
            MenuItem("Aggregated Data Profiles", "/admin/aggregated-data-profiles"),
            MenuItem("Connectors", "/admin/connectors"),
        )
    }

    @ModelAttribute("username")
    fun username(@AuthenticationPrincipal principal: OidcUser): String? {
        val claims = principal.userInfo?.claims ?: return null
        val username = claims["preferred_username"] as? String
        return if (!username.isNullOrBlank()) username else "Unknown username"
    }

    @ModelAttribute("email")
    fun email(@AuthenticationPrincipal principal: OidcUser): String? {
        val claims = principal.userInfo?.claims ?: return null
        val email = claims["email"] as? String
        return if (!email.isNullOrBlank()) email else "Unknown email address"
    }

    @ModelAttribute("name")
    fun name(@AuthenticationPrincipal principal: OidcUser): String? {
        val claims = principal.userInfo?.claims ?: return null
        val name = claims["name"] as? String
        return if (!name.isNullOrBlank()) name else "Unknown user"
    }

}
