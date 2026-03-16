package pl.allegro.tech.github.botorchestrator

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.badRequest
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.Body
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.datatest.withData
import io.kotest.matchers.equals.shouldBeEqual
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import pl.allegro.tech.github.botorchestrator.infra.github.GithubAuthenticationClient
import pl.allegro.tech.github.botorchestrator.infra.github.GithubClientException

class GithubAuthenticationClientSpec(val authenticationClient: GithubAuthenticationClient) : BaseIntegrationSpec() {
    init {
        context("github authentication client") {

            val installationId = "42"
            val token = "example-token"

            test("should pass correct data") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/app/installations/$installationId/access_tokens")).willReturn(
                        ok()
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                            .withResponseBody(
                                Body(
                                    """
                                    {
                                        "token": "$token"
                                    }
                                    """.trimIndent()
                                )
                            )
                    )
                )

                // when
                val result = authenticationClient.authenticateAppInstallation(installationId)

                // then
                result shouldBeEqual token

                githubApiMock.verify(
                    postRequestedFor(urlEqualTo("/app/installations/$installationId/access_tokens"))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $SIGNED_TEMP_TOKEN"))
                )
            }

            test("should fail when no token present in the response") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/app/installations/$installationId/access_tokens")).willReturn(
                        ok()
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                            .withResponseBody(
                                Body(
                                    """
                                    {
                                        "error": "error"
                                    }
                                    """.trimIndent()
                                )
                            )
                    )
                )

                // then
                shouldThrowExactly<GithubClientException> {
                    authenticationClient.authenticateAppInstallation(installationId)
                }
            }

            context("should fail when github returns") {
                withData(
                    mapOf(
                        "3xx" to aResponse().withStatus(301),
                        "4xx" to badRequest(),
                        "5xx" to serverError()
                    )
                ) {
                    // given
                    githubApiMock.stubFor(
                        post(urlEqualTo("/app/installations/$installationId/access_tokens")).willReturn(
                            it
                                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                                .withResponseBody(
                                    Body(
                                        """
                                        {
                                            "error": "error"
                                        }
                                        """.trimIndent()
                                    )
                                )
                        )
                    )

                    // then
                    shouldThrowExactly<GithubClientException> {
                        authenticationClient.authenticateAppInstallation(installationId)
                    }
                }
            }
        }
    }
}
