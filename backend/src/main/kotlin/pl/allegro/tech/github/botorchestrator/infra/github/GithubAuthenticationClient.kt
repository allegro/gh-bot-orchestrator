package pl.allegro.tech.github.botorchestrator.infra.github

import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestClient
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.infra.github.auth.JwtSigner

class GithubAuthenticationClient(
    private val githubBaseUrl: String,
    private val jwtSigner: JwtSigner,
    private val restClient: RestClient
) {

    /**
     * Creates a new token for GitHub App as an installation with all the permissions available to this installation
     */
    fun authenticateAppInstallation(installationId: String): String {
        return getToken(installationId, emptyMap())
            .mapError { GithubClientException("Unable to get an installation token", it) }
            .getOrThrow()
    }

    /**
     * Creates a token to be passed to workflow. Token scope is limited to read-only for the specific repository
     */
    fun getWorkflowToken(repo: Repository, installationId: String): String {
        return getToken(installationId, installationTokenRequest(repo))
            .mapError { GithubClientException("Unable to get an installation token for the workflow", it) }
            .getOrThrow()
    }

    private fun getToken(installationId: String, body: Map<String, Any>): Result<String> {
        val temporaryToken = jwtSigner.createNewSignedToken()
        return restClient.post()
            .uri("$githubBaseUrl/app/installations/$installationId/access_tokens")
            .headers {
                it.set(HttpHeaders.AUTHORIZATION, "Bearer $temporaryToken")
            }
            .body(body)
            .toResult<AuthenticationResponse>()
            .mapCatching { it?.token ?: throw GithubClientException("invalid response: no `token` found in the response") }
    }

    private fun installationTokenRequest(repo: Repository): Map<String, Any> = mapOf(
        "repositories" to listOf(repo.id.name),
        "permissions" to mapOf("contents" to "read")
    )

    private data class AuthenticationResponse(val token: String?)
}
