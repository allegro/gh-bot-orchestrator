package pl.allegro.tech.github.botorchestrator.infra.github.auth

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.infra.github.GithubAuthProperties
import pl.allegro.tech.github.botorchestrator.infra.github.GithubAuthenticationClient
import pl.allegro.tech.github.botorchestrator.infra.github.auth.TokenProvider.Companion.LIFETIME_FRACTION_TO_REFRESH
import kotlin.time.ComparableTimeMark
import kotlin.time.TimeSource
import kotlin.time.times

interface TokenProvider {

    /**
     * Creates an app installation token and caches it.
     * Token gets refreshed on read, when less than [LIFETIME_FRACTION_TO_REFRESH]
     * of its lifetime left
     */
    fun getToken(): String

    fun getWorkflowToken(repo: Repository): String

    companion object {

        const val LIFETIME_FRACTION_TO_REFRESH = 0.25
    }
}

class DefaultTokenProvider(
    private val authClient: GithubAuthenticationClient,
    private val timeSource: TimeSource.WithComparableMarks,
    private val properties: GithubAuthProperties
) : TokenProvider {

    @Volatile
    private var nextRefreshTime: ComparableTimeMark = timeSource.markNow()

    @Volatile
    private var token: String? = null

    override fun getToken(): String {
        val now = timeSource.markNow()
        val remaining = nextRefreshTime - now
        if (remaining < LIFETIME_FRACTION_TO_REFRESH * properties.tokenTimeToLive) {
            logger.debug { "Refreshing the token: now=$now nextRefreshTime=$nextRefreshTime" }
            refresh()
        }

        return token ?: throw RuntimeException("Token is not available")
    }

    override fun getWorkflowToken(repo: Repository): String = authClient.getWorkflowToken(repo, properties.installationId)

    private fun refresh() {
        try {
            token = authClient.authenticateAppInstallation(properties.installationId)
            nextRefreshTime = timeSource.markNow() + properties.tokenTimeToLive
            logger.debug { "Token is refreshed, nextRefreshTime=$nextRefreshTime" }
        } catch (e: Exception) {
            logger.error(e) { "Can't refresh token:" }
        }
    }

    companion object {

        private val logger = KotlinLogging.logger {}
    }
}
