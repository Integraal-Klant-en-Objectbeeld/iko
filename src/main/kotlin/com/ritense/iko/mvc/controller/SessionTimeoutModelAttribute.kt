/*
 * Copyright 2026 Den Haag, Ritense, Rotterdam, Utrecht, the Netherlands.
 *
 * Licensed under EUPL, Version 1.2 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ritense.iko.mvc.controller

import com.ritense.iko.security.AdminSessionTimeoutResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.Authentication
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/**
 * Surfaces the per-session admin session timeout into every admin page model,
 * replacing the static `@adminSessionProperties` SpEL references in
 * `layout-internal.html`. Scoped to the admin controllers package, mirroring
 * the per-user data pattern in [com.ritense.iko.mvc.controller.HomeController].
 */
@ControllerAdvice(basePackages = ["com.ritense.iko.mvc.controller"])
internal class SessionTimeoutModelAttribute(
    private val adminSessionTimeoutResolver: AdminSessionTimeoutResolver,
) {
    @ModelAttribute
    fun sessionTimeout(authentication: Authentication?, request: HttpServletRequest, model: Model) {
        if (authentication == null) {
            return
        }
        val timeout = adminSessionTimeoutResolver.resolve(request.getSession(false))
        model.addAttribute("sessionTimeoutSeconds", timeout.timeoutSeconds)
        model.addAttribute("sessionWarningSeconds", timeout.warningSeconds)
    }
}