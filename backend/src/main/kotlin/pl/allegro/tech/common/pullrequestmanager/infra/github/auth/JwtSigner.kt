package pl.allegro.tech.common.pullrequestmanager.infra.github.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.KeyPair
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.RSAPublicKeySpec
import java.time.Clock
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * Creates a temporary JWT token with app credentials, which is needed to
 * request app installation access token
 */
class JwtSigner(
    privateKey: String,
    private val appId: String,
    private val clock: Clock
) {

    private val signingKey: KeyPair by lazy {
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKeySpec = privateKey.toPKSC8Spec()
        val private = keyFactory.generatePrivate(privateKeySpec) as RSAPrivateCrtKey
        val publicKeySpec = RSAPublicKeySpec(private.modulus, private.publicExponent)
        val public = keyFactory.generatePublic(publicKeySpec)
        KeyPair(public, private)
    }

    fun createNewSignedToken(): String {
        val now = clock.millis()
        return JWT.create()
            .withIssuer(appId)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + 1.minutes.inWholeMilliseconds))
            .sign(Algorithm.RSA256(signingKey.public as RSAPublicKey, signingKey.private as RSAPrivateKey))
    }

    private fun String.toPKSC8Spec() =
        PKCS8EncodedKeySpec(
            Base64.getDecoder().decode(
                this.replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\n", "")
                    .trim()
            )
        )
}
