package pl.allegro.tech.common.pullrequestmanager

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import pl.allegro.tech.common.pullrequestmanager.infra.github.GithubAuthProperties
import pl.allegro.tech.common.pullrequestmanager.infra.github.GithubAuthenticationClient
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.DefaultTokenProvider
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.TokenProvider
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TokenProviderSpec : FunSpec() {

    private val properties = GithubAuthProperties(
        appId = "123",
        installationId = "123",
        privateKey = "123",
        tokenTimeToLive = 1.hours
    )

    init {
        // amount of time which should pass before we should try to refresh the token
        val timeBeforeRefresh = properties.tokenTimeToLive * (1 - TokenProvider.LIFETIME_FRACTION_TO_REFRESH)

        test("should refresh token on first request") {
            // given
            val time = TestTimeSource()
            val token = "123"
            val authClient =
                mockk<GithubAuthenticationClient> {
                    every { authenticateAppInstallation(any()) } returns token
                }
            val tokenProvider = DefaultTokenProvider(authClient, time, properties)

            // when
            val result = tokenProvider.getToken()

            // then
            result shouldBeEqual token
            verify { authClient.authenticateAppInstallation(properties.installationId) }
        }

        test("should not refresh token when current token is fresh enough") {
            // given
            val time = TestTimeSource()
            val firstToken = "123"
            val secondToken = "456"
            val authClient =
                mockk<GithubAuthenticationClient> {
                    every { authenticateAppInstallation(any()) } returns firstToken andThen secondToken
                }
            val tokenProvider = DefaultTokenProvider(authClient, time, properties)

            // when
            tokenProvider.getToken()
            time.plusAssign(timeBeforeRefresh)
            val result = tokenProvider.getToken()

            // then
            result shouldBeEqual firstToken
            verify(exactly = 1) {
                authClient.authenticateAppInstallation(properties.installationId)
            }
        }

        test("should refresh token when more than $timeBeforeRefresh has elapsed since the latest refresh") {
            // given
            val time = TestTimeSource()
            val firstToken = "123"
            val secondToken = "456"
            val authClient =
                mockk<GithubAuthenticationClient> {
                    every { authenticateAppInstallation(any()) } returns firstToken andThen secondToken
                }
            val tokenProvider = DefaultTokenProvider(authClient, time, properties)

            // when
            tokenProvider.getToken()

            time.plusAssign(timeBeforeRefresh + 1.seconds)
            val result = tokenProvider.getToken()

            // then
            result shouldBeEqual secondToken
            verify(exactly = 2) {
                authClient.authenticateAppInstallation(properties.installationId)
            }
        }

        test("should fail if can't get the token on first request") {
            // given
            val time = TestTimeSource()
            val authClient =
                mockk<GithubAuthenticationClient> {
                    every { authenticateAppInstallation(any()) } throws RuntimeException()
                }
            val tokenProvider = DefaultTokenProvider(authClient, time, properties)

            // then
            shouldThrowExactly<RuntimeException> {
                tokenProvider.getToken()
            }

            verify(exactly = 1) {
                authClient.authenticateAppInstallation(properties.installationId)
            }
        }

        test("should return the old token when can't refresh the token") {
            // given
            val time = TestTimeSource()
            val token = "123"
            val authClient =
                mockk<GithubAuthenticationClient> {
                    every { authenticateAppInstallation(any()) } returns token andThenThrows RuntimeException()
                }
            val tokenProvider = DefaultTokenProvider(authClient, time, properties)

            // when
            tokenProvider.getToken()
            time.plusAssign(timeBeforeRefresh + 1.seconds)
            val result = tokenProvider.getToken()

            // then
            result shouldBeEqual token
            verify(exactly = 2) {
                authClient.authenticateAppInstallation(properties.installationId)
            }
        }
    }
}
