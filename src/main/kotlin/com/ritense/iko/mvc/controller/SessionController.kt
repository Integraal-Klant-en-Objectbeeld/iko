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

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

/**
 * Keep-alive endpoint backing the "Continue" action of the session timeout
 * modal. The endpoint lives under `/admin`, so it is covered by the admin
 * filter chain and requires an authenticated session. Handling the request
 * touches the existing HTTP session, resetting its inactivity timer.
 */
@Controller
@RequestMapping("/admin/session")
internal class SessionController {
    @GetMapping("/ping")
    fun ping(request: HttpServletRequest): ResponseEntity<Void> {
        // Accessing the existing session resets its inactivity timer.
        request.getSession(false)
        return ResponseEntity.noContent().build()
    }
}