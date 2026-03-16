package pl.allegro.tech.github.botorchestrator.infra.github.oidc

import com.auth0.jwt.exceptions.SignatureVerificationException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component

@Component
class OwnershipValidator(private val tokenVerifier: OidcTokenDecoder) {

    fun belongsToExpectedRepository(jwtToken: String, expectedRepository: String): Boolean {
        try {
            val repository = tokenVerifier.verifyAndRetrieveRepository(jwtToken.removePrefix("Bearer "))

            if (repository != expectedRepository) {
                logger.warn { "Invalid repository: $repository in token. Expected: $expectedRepository" }
                return false
            }

            return true
        } catch (e: SignatureVerificationException) {
            logger.warn { "JWT verification failed: ${e.message}" }
            return false
        }
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }

}
