package com.ritense.iko.openzaak

import java.nio.charset.Charset
import java.util.Date

class TokenGeneratorService {

    fun generateToken(
        secretKey: String = "e09b8bc5-5831-4618-ab28-41411304309d",
        clientId: String = "valtimo_client"
    ): String {
        if (secretKey.length < 32) {
            throw IllegalStateException("SecretKey needs to be at least 32 in length")
        }
        val signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secretKey.toByteArray(Charset.forName("UTF-8")))

        val jwtBuilder = io.jsonwebtoken.Jwts.builder()
        jwtBuilder
            .issuer(clientId)
            .issuedAt(Date())
            .claim("client_id", clientId)

        appendUserInfo(jwtBuilder)
        return jwtBuilder
            .signWith(signingKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact()
    }

    private fun appendUserInfo(jwtBuilder: io.jsonwebtoken.JwtBuilder) {
        val userLogin = "Valtimo"
        val userId = "Valtimo"

        jwtBuilder
            .claim("user_id", userId)
            .claim("user_representation", "")

       /* if (authenticated) {
            val roles = com.ritense.valtimo.contract.utils.SecurityUtils.getCurrentUserRoles()
            if (!roles.isNullOrEmpty()) {
                jwtBuilder.claim("roles", roles)
            }
        }*/
    }

}