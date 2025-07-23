package com.ritense.iko.connectors.openzaak

import java.nio.charset.Charset
import java.util.Date

class TokenGeneratorService {

    fun generateToken(
        clientId: String,
        secretKey: String,
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

        return jwtBuilder
            .signWith(signingKey, io.jsonwebtoken.SignatureAlgorithm.HS256)
            .compact()
    }

}