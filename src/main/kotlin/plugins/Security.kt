package com.codewithfk.plugins

import com.codewithfk.config.Env
import com.codewithfk.config.Security
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm

fun Application.configureSecurity() {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "QuickNotes API"
            verifier(
                JWT.require(Algorithm.HMAC256(Env.JWT_SECRET))
                    .withAudience(Security.AUDIENCE)
                    .withIssuer(Security.ISSUER)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("sub").asString() != "") {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
