package pl.allegro.tech.common.pullrequestmanager

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.CountMatchingStrategy
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.absent
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.http.Body
import com.github.tomakehurst.wiremock.junit.Stubbing
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import pl.allegro.tech.common.pullrequestmanager.domain.ExtraParams
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.common.pullrequestmanager.infra.github.RestGithubClient.Companion.GITHUB_API_VERSION_HEADER_NAME
import pl.allegro.tech.common.pullrequestmanager.infra.github.RestGithubClient.Companion.GITHUB_API_VERSION_HEADER_VALUE
import pl.allegro.tech.common.pullrequestmanager.infra.github.RestGithubClient.Companion.GITHUB_CONTENT_TYPE

class GithubApiMock(
    private val wiremock: WireMockServer,
    private val accessToken: String
) : Stubbing by wiremock {

    fun stubTokenResponse() {
        wiremock.stubFor(
            post(urlMatching("/app/installations/.*/access_tokens"))
                .withRequestBody(equalTo("{}"))
                .willReturn(
                ok()
                    .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
                    .withResponseBody(
                        Body(
                            """
                            {
                                "token": "$accessToken"
                            }
                            """.trimIndent()
                        )
                    )
            )
        )
    }

    fun stubWorkflowTokenResponse(repoName: String, token: String) {
        wiremock.stubFor(
            post(urlMatching("/app/installations/.*/access_tokens"))
                .withRequestBody(equalToJson("""
                    {
                      "repositories": ["$repoName"],
                      "permissions": {
                        "contents": "read"
                      }
                    }
                    """.trimIndent()))
                .willReturn(
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
    }

    fun stubCheckUpdateResponse() {
        wiremock.stubFor(
            patch(urlMatching("/repos/[a-z\\-]*/[a-z\\-]*/check-runs/[a-z\\-]*"))
                .willReturn(ok())
            )
    }

    fun verifyWorkflowDispatch(
        workflow: WorkflowDefinition,
        extraParams: ExtraParams? = null,
        countMatchingStrategy: CountMatchingStrategy = moreThanOrExactly(1)
    ) {
        var bodyMatcher = matchingJsonPath("$.ref", equalTo(workflow.ref))
        if (extraParams != null) {
            val extraParams = objectMapper.writeValueAsString(extraParams)
            bodyMatcher = bodyMatcher.and(matchingJsonPath("$.inputs['extra-params']", equalTo("$extraParams")))
        }

        wiremock.verify(
            countMatchingStrategy,
            postRequestedFor(
                urlEqualTo("/repos/${workflow.repository.id.owner}/${workflow.repository.id.name}/actions/workflows/${workflow.fileName}/dispatches")
            )
                .withDefaultHeaders()
                .withRequestBody(bodyMatcher)
        )
    }

    fun noWorkflowWasDispatched() {
        wiremock.verify(
            0,
            postRequestedFor(urlPathMatching("/repos/.*/.*/actions/workflows/.*/dispatches"))
        )
    }

    fun noWorkflowWasDispatched(workflow: WorkflowDefinition) {
        wiremock.verify(
            0,
            postRequestedFor(urlPathEqualTo("/repos/${workflow.repository.id.owner}/${workflow.repository.id.name}/actions/workflows/${workflow.fileName}/dispatches"))
        )
    }

    private fun RequestPatternBuilder.withDefaultHeaders(): RequestPatternBuilder = this
        .withHeader(GITHUB_API_VERSION_HEADER_NAME, equalTo(GITHUB_API_VERSION_HEADER_VALUE))
        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $accessToken"))
        .withHeader(HttpHeaders.ACCEPT, equalTo(GITHUB_CONTENT_TYPE))

    fun verifyCommentPublished(repo: Repository, pullRequestNumber: Int, expectedRequest: String) {
        wiremock.verify(
            1,
            postRequestedFor(urlEqualTo("/repos/${repo.id.owner}/${repo.id.name}/issues/${pullRequestNumber}/comments"))
                .withRequestBody(equalToJson(expectedRequest))
        )
    }

    fun verifyCommentUpdated(repo: Repository, commentId: Int, expectedRequest: String) {
        wiremock.verify(
            1,
            patchRequestedFor(urlEqualTo("/repos/${repo.id.owner}/${repo.id.name}/issues/comments/${commentId}"))
                .withRequestBody(equalToJson(expectedRequest))
        )
    }

    fun verifyCheckCreated(repo: Repository, expectedRequest: String, countMatchingStrategy: CountMatchingStrategy = moreThanOrExactly(1)) {
        wiremock.verify(
            countMatchingStrategy,
            postRequestedFor(urlEqualTo("/repos/${repo.id.owner}/${repo.id.name}/check-runs"))
                .withRequestBody(equalToJson(expectedRequest))
        )
    }

    fun verifyCheckUpdated(repo: Repository, checkRunId: String, expectedRequest: String) {
        wiremock.verify(
            1,
            patchRequestedFor(urlEqualTo("/repos/${repo.id.owner}/${repo.id.name}/check-runs/${checkRunId}"))
                .withRequestBody(equalToJson(expectedRequest))
        )
    }

    companion object {
        private val objectMapper = ObjectMapper()
    }
}
