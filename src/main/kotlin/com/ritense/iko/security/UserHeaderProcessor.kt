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

package com.ritense.iko.security

import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.camel.Exchange
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jwt.Jwt

class UserHeaderProcessor {

    fun addEmailHeader(exchange: Exchange) {
        val email = currentUserEmail()
        if (email.isNullOrBlank()) {
            logger.warn { "No email available on current authentication; skipping $X_GEBRUIKER_HEADER header" }
            return
        }
        exchange.message.setHeader(X_GEBRUIKER_HEADER, email)
    }

    private fun currentUserEmail(): String? {
        val principal = SecurityContextHolder.getContext().authentication?.principal ?: return null
        return when (principal) {
            is Jwt -> principal.getClaimAsString(EMAIL_CLAIM)
            is OidcUser -> principal.userInfo?.claims?.get(EMAIL_CLAIM) as? String
            else -> null
        }
    }

    companion object {
        const val X_GEBRUIKER_HEADER = "x-gebruiker"
        private const val EMAIL_CLAIM = "email"
        private val logger = KotlinLogging.logger {}
    }
}