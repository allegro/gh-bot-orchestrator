package pl.allegro.tech.github.botorchestrator.infra.github.oidc

import com.auth0.jwk.Jwk
import com.auth0.jwk.UrlJwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm.RSA256
import com.auth0.jwt.interfaces.DecodedJWT
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit

// TODO: add a test for the case when thumbprint is not found
@Component
class OidcTokenDecoder(private val objectMapper: ObjectMapper) {

    fun verifyAndRetrieveRepository(jwtToken: String): String {
        val jwt = JWT.decode(jwtToken)
        if (jwt.issuer !in ISSUERS) error("Invalid issuer: ${jwt.issuer}")
        val jwk = jwkProvider.findByCertificateThumbprint(jwt) ?: error("Certificate thumbprint not found")
        val verifier = RSA256(jwk.getPublicKey() as RSAPublicKey)
        verifier.verify(jwt)
        val oidcExtra = objectMapper.readValue<OidcExtra>(jwt.getClaim("oidc_extra").asString())
        return oidcExtra.repository
    }

    companion object {
        private val ISSUERS = setOf("vstoken.actions.githubusercontent.com", "https://token.actions.githubusercontent.com")

        private val GITHUB_JWKS_URL = URI("https://token.actions.githubusercontent.com/.well-known/jwks").toURL()
        private val jwkProvider = CachedJwtProvider(GITHUB_JWKS_URL)
    }
}

data class OidcExtra(val repository: String)

private class CachedJwtProvider(url: URL) : UrlJwkProvider(url) {

    @Volatile
    private var cache = Cache(super.getAll())

    override fun getAll(): List<Jwk> {
        if (cache.isExpired()) {
            cache = Cache(super.getAll())
        }
        return cache.jwks
    }

    fun findByCertificateThumbprint(jwt: DecodedJWT): Jwk? = all.find { it.certificateThumbprint == jwt.getHeaderClaim("x5t")?.asString() }
}

private data class Cache(val jwks: List<Jwk>, val lastUpdatedTimestamp: Instant = Instant.now()) {
    fun isExpired() = lastUpdatedTimestamp.isBefore(Instant.now().minus(10, ChronoUnit.MINUTES))
}
