package pl.allegro.tech.github.botorchestrator

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.badRequest
import com.github.tomakehurst.wiremock.client.WireMock.badRequestEntity
import com.github.tomakehurst.wiremock.client.WireMock.created
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.noContent
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.serverError
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.tomakehurst.wiremock.http.RequestMethod.GET
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.matching.UrlPattern
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.datatest.withData
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.json.JSONObject
import org.springframework.http.HttpHeaders
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.PR_NUMBER
import pl.allegro.tech.github.botorchestrator.api.CheckRunUpdateRequest.DetailsPageUpdateRequest
import pl.allegro.tech.github.botorchestrator.api.PullRequestEvent
import pl.allegro.tech.github.botorchestrator.domain.CheckDetails
import pl.allegro.tech.github.botorchestrator.domain.ExtraParams
import pl.allegro.tech.github.botorchestrator.domain.FailedToCreateCheck
import pl.allegro.tech.github.botorchestrator.domain.FailedToUpdateCheckRun
import pl.allegro.tech.github.botorchestrator.domain.GithubClient
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.Repository.Id
import pl.allegro.tech.github.botorchestrator.domain.TakenSlot
import pl.allegro.tech.github.botorchestrator.domain.Workflow
import pl.allegro.tech.github.botorchestrator.domain.WorkflowInputs
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.DetailsPage
import pl.allegro.tech.github.botorchestrator.infra.github.GithubClientException
import pl.allegro.tech.github.botorchestrator.infra.github.GithubServerProperties
import pl.allegro.tech.github.botorchestrator.infra.github.PullRequestFileDto
import pl.allegro.tech.github.botorchestrator.infra.github.RestGithubClient.Companion.GITHUB_API_VERSION_HEADER_NAME
import pl.allegro.tech.github.botorchestrator.infra.github.RestGithubClient.Companion.GITHUB_API_VERSION_HEADER_VALUE
import pl.allegro.tech.github.botorchestrator.infra.github.RestGithubClient.Companion.GITHUB_CONTENT_TYPE
import java.time.Instant
import java.util.UUID.randomUUID

class RestGithubClientSpec(
    val githubClient: GithubClient,
    val objectMapper: ObjectMapper,
    val githubServerProperties: GithubServerProperties
) : BaseIntegrationSpec() {

    init {

        beforeAny {
            githubApiMock.stubTokenResponse()
        }

        val pullRequestEvent = examplePullRequestEvent()

        context("dispatch workflow request") {
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val workflowOrg = someWorkflow.repository.id.owner
            val workflowGithubRepo = someWorkflow.repository.id.name
            val workflowFileName = someWorkflow.fileName
            val checkUrl = "some-callback-url"
            val commentUrl = "some-comment-url"
            val slot = TakenSlot(randomUUID(), 0, Instant.now())

            test("should pass correct data") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/repos/$workflowOrg/$workflowGithubRepo/actions/workflows/$workflowFileName/dispatches"))
                        .willReturn(noContent())
                )

                // when
                dispatchWorkflow(someWorkflow, pullRequestEvent.data, slot, checkUrl, commentUrl)

                // then
                githubApiMock.verify(
                    postRequestedFor(
                        urlEqualTo("/repos/$workflowOrg/$workflowGithubRepo/actions/workflows/$workflowFileName/dispatches")
                    )
                        .withHeader(GITHUB_API_VERSION_HEADER_NAME, equalTo(GITHUB_API_VERSION_HEADER_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(GITHUB_CONTENT_TYPE))
                        .withRequestBody(
                            equalToJson(
                                """
                                {
                                  "ref": "${someWorkflow.ref}",
                                  "inputs": {
                                    "event": ${JSONObject.quote(pullRequestEvent.data)},
                                    "workflow-concurrency-group": "${someWorkflow.id}-${slot.workflowConcurrencyGroup}",
                                    "concurrency-group": "${someWorkflow.id}-${someWorkflow.repository.id.name}-${PR_NUMBER}",
                                    "slot-id": "some-owner/some-repository#$PR_NUMBER | ${slot.id}",
                                    "check-callback-url": "$checkUrl",
                                    "comment-callback-url": "$commentUrl",
                                    "extra-params": "{\"dependabot\":[],\"github-clone-token\":\"abc123xyz\"}"
                                  }
                                }
                                """
                            )
                        )
                )
            }

            context("should fail on response with status code other than 2xx") {
                withData(
                    aResponse().withStatus(301),
                    badRequest(),
                    serverError()
                ) {
                    // given
                    githubApiMock.stubFor(
                        post(urlEqualTo("repos/$workflowOrg/$workflowGithubRepo/actions/workflows/$workflowFileName/dispatches"))
                            .willReturn(it)
                    )

                    // when
                    shouldThrowExactly<GithubClientException> {
                        dispatchWorkflow(someWorkflow, pullRequestEvent, slot, checkUrl, commentUrl)
                    }
                }
            }
        }

        context("should create check on pull request") {
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val checkId = "33693053267"
            val owner = someWorkflow.repository.id.owner
            val repoName = someWorkflow.repository.id.name
            val repo = Repository(owner, repoName)
            val headSha = "123"
            val checkName = "some check name"
            val detailsPage = DetailsPage("Some title", "Some summary", "Some details", "custom-url")

            test("should pass correct data") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/repos/$owner/$repoName/check-runs"))
                        .willReturn(
                            created()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                    """
                                    {
                                        "id": $checkId
                                    }
                                """.trimIndent()
                                )
                        )
                )

                // when
                val result = githubClient.createCheck(repo, headSha, checkName, detailsPage, "external-id")

                // then
                result shouldBe CheckDetails(repo, checkId)

                // and
                githubApiMock.verify(
                    postRequestedFor(
                        urlEqualTo("/repos/$owner/$repoName/check-runs")
                    )
                        .withHeader(GITHUB_API_VERSION_HEADER_NAME, equalTo(GITHUB_API_VERSION_HEADER_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(GITHUB_CONTENT_TYPE))
                        .withRequestBody(
                            equalToJson(
                                """
                                {
                                  "head_sha": "$headSha",
                                  "name": "$checkName",
                                  "output": {
                                    "title": "${detailsPage.title}",
                                    "summary": "${detailsPage.summary}",
                                    "text": "${detailsPage.details}"
                                  },
                                  "details_url": "${detailsPage.customUrl}",
                                  "external_id": "external-id",
                                  "status": "in_progress"
                                }
                                """
                            )
                        )
                )
            }

            test("should ignore fields with null values") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/repos/$owner/$repoName/check-runs"))
                        .willReturn(
                            created()
                                .withHeader("Content-Type", "application/json")
                                .withBody(
                                    """
                                    {
                                        "id": $checkId
                                    }
                                """.trimIndent()
                                )
                        )
                )

                // when
                val result = githubClient.createCheck(repo, headSha, checkName, detailsPage.copy(details = null, customUrl = null), "external-id")

                // then
                result shouldBe CheckDetails(repo, checkId)

                // and
                githubApiMock.verify(
                    postRequestedFor(
                        urlEqualTo("/repos/$owner/$repoName/check-runs")
                    )
                        .withHeader(GITHUB_API_VERSION_HEADER_NAME, equalTo(GITHUB_API_VERSION_HEADER_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(GITHUB_CONTENT_TYPE))
                        .withRequestBody(
                            equalToJson(
                                """
                                {
                                  "head_sha": "$headSha",
                                  "name": "$checkName",
                                  "output": {
                                    "title": "${detailsPage.title}",
                                    "summary": "${detailsPage.summary}"
                                  },
                                  "external_id": "external-id",
                                  "status": "in_progress"
                                }
                                """
                            )
                        )
                )
            }

            context("should fail on response with status code other than 2xx") {
                withData(
                    aResponse().withStatus(301),
                    badRequest(),
                    serverError()
                ) {
                    // given
                    githubApiMock.stubFor(
                        post(urlEqualTo("/repos/$owner/$repoName/check-runs"))
                            .willReturn(it)
                    )

                    // when
                    shouldThrowExactly<GithubClientException> {
                        githubClient.createCheck(repo, headSha, checkName, detailsPage, "external-id")
                    }
                }
            }

            test("should return non-repeatable failure when branch (or head sha) does not exist") {
                // given
                githubApiMock.stubFor(
                    post(urlEqualTo("/repos/$owner/$repoName/check-runs"))
                        .willReturn(badRequestEntity().withBody("Something went wrong"))
                )

                // when
                val result = githubClient.createCheck(repo, headSha, checkName, detailsPage, "external-id")

                // then
                result shouldBe FailedToCreateCheck(reason = "422 UNPROCESSABLE_ENTITY Something went wrong")
            }

            test("should pass correct data when performing update") {
                // given
                githubApiMock.stubFor(
                    patch(urlEqualTo("/repos/$owner/$repoName/check-runs/${checkId}"))
                        .willReturn(ok())
                )

                // when
                githubClient.updateCheck(
                    checkId, owner, repoName, "failure", DetailsPageUpdateRequest(
                        title = detailsPage.title,
                        summary = detailsPage.summary,
                        details = detailsPage.details,
                        customUrl = detailsPage.customUrl
                    )
                )

                // then
                githubApiMock.verify(
                    patchRequestedFor(urlEqualTo("/repos/$owner/$repoName/check-runs/${checkId}"))
                        .withHeader(GITHUB_API_VERSION_HEADER_NAME, equalTo(GITHUB_API_VERSION_HEADER_VALUE))
                        .withHeader(HttpHeaders.AUTHORIZATION, equalTo("Bearer $ACCESS_TOKEN"))
                        .withHeader(HttpHeaders.ACCEPT, equalTo(GITHUB_CONTENT_TYPE))
                        .withRequestBody(
                            equalToJson(
                                """
                                {
                                  "output": {
                                    "title": "${detailsPage.title}",
                                    "summary": "${detailsPage.summary}",
                                    "text": "${detailsPage.details}"
                                  },
                                  "details_url": "${detailsPage.customUrl}",
                                  "conclusion": "failure"
                                }
                                """
                            )
                        )
                )
            }

            test("should return non-repeatable failure when branch (or head sha) does not exist when performing update") {
                // given
                githubApiMock.stubFor(
                    patch(urlEqualTo("/repos/$owner/$repoName/check-runs/${checkId}"))
                        .willReturn(badRequestEntity().withBody("Something went wrong"))
                )

                // when
                val result = githubClient.updateCheck(
                    checkId, owner, repoName, "success", DetailsPageUpdateRequest(
                        title = detailsPage.title,
                        summary = detailsPage.summary,
                        details = detailsPage.details,
                        customUrl = detailsPage.customUrl
                    )
                )

                // then
                result shouldBe FailedToUpdateCheckRun(reason = "422 UNPROCESSABLE_ENTITY Something went wrong")
            }

        }

        test("should get all pages of pull request files") {
            // given
            val org = "org"
            val repo = "repo"
            val prNumber = 1

            // and
            githubApiMock.stubTokenResponse()
            stubFirstPageOfPullRequestFilesPointingToSecondPage(org, repo, prNumber)
            stubSecondPageOfPullRequestFiles(org, repo, prNumber)

            // when
            val files = githubClient.getPullRequestFiles(Repository(Id(org, repo)), prNumber)
                .toList()

            // then
            files shouldContainExactlyInAnyOrder listOf(
                PullRequestFileDto("file-on-first-page.txt", "modified"),
                PullRequestFileDto("file-on-second-page.txt", "modified"),
            )
        }

        test("should skip second page if the first one already contains the matching file") {
            // given
            val org = "org"
            val repo = "repo"
            val prNumber = 1

            // and
            githubApiMock.stubTokenResponse()
            val firstPageUrl = stubFirstPageOfPullRequestFilesPointingToSecondPage(org, repo, prNumber)
            val secondPageUrl = stubSecondPageOfPullRequestFiles(org, repo, prNumber)

            // when
            val files = githubClient.getPullRequestFiles(Repository(Id(org, repo)), prNumber)

            // then
            files.any { it.filename.endsWith(".txt") }.shouldBeTrue()
            githubApiMock.verify(1, RequestPatternBuilder(GET, firstPageUrl))
            githubApiMock.verify(0, RequestPatternBuilder(GET, secondPageUrl))
        }

        test("should return token for the workflow") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val token = "aaa123bbb"
            githubApiMock.stubWorkflowTokenResponse(someWorkflow.repository.id.name, token)

            // when
            val result = githubClient.getWorkflowToken(someWorkflow.repository)

            // then
            result.shouldNotBeNull()
            result shouldBeEqual token
        }

        test("should return null when failed to generate token for the workflow") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()

            // when
            val token = githubClient.getWorkflowToken(someWorkflow.repository)

            // then
            token.shouldBeNull()
        }
    }

    @Suppress("SameParameterValue")
    private fun stubFirstPageOfPullRequestFilesPointingToSecondPage(org: String, repo: String, prNumber: Int): UrlPattern {
        val urlPattern = urlMatching("/repos/${org}/${repo}/pulls/${prNumber}/files\\?per_page=100")

        githubApiMock.stubFor(
            get(urlPattern).willReturn(
                okJson(
                    """
                    [
                      {
                        "filename": "file-on-first-page.txt",
                        "status": "modified"
                      }
                    ]
                    """.trimIndent()
                )
                    .withHeader(
                        HttpHeaders.LINK,
                        "<${githubServerProperties.baseUrl}/repos/org/repo/pulls/1/files?page=2>; rel=\"next\", " +
                            "<${githubServerProperties.baseUrl}/repos/org/repo/pulls/1/files?page=2>; rel=\"last\", " +
                            "<${githubServerProperties.baseUrl}/repos/org/repo/pulls/1/files?page=1>; rel=\"first\""
                    )
            )
        )
        return urlPattern
    }

    @Suppress("SameParameterValue")
    private fun stubSecondPageOfPullRequestFiles(org: String, repo: String, prNumber: Int): UrlPattern {
        val urlPattern = urlMatching("/repos/${org}/${repo}/pulls/${prNumber}/files\\?page=2")

        githubApiMock.stubFor(
            get(urlPattern).willReturn(
                okJson(
                    """
                    [
                      {
                        "filename": "file-on-second-page.txt",
                        "status": "modified"
                      }
                    ]
                    """.trimIndent()
                )
            )
        )
        return urlPattern
    }

    private fun dispatchWorkflow(workflowDefinition: WorkflowDefinition, pullRequestEvent: String, slot: TakenSlot, callbackUrl: String, commentUrl: String) {
        githubClient.dispatchWorkflow(
            workflow = Workflow(
                Id(workflowDefinition.repository.id.owner, workflowDefinition.repository.id.name),
                workflowDefinition.ref,
                workflowDefinition.fileName,
            ),
            inputs = WorkflowInputs(
                event = pullRequestEvent,
                concurrencyGroup = "${workflowDefinition.id}-${workflowDefinition.repository.id.name}-${PR_NUMBER}",
                checkCallbackUrl = callbackUrl,
                commentCallbackUrl = commentUrl,
                slotId = "${workflowDefinition.repository.fullName}#$PR_NUMBER | ${slot.id}",
                workflowConcurrencyGroup = "${workflowDefinition.id}-${slot.workflowConcurrencyGroup}",
                extraParams = ExtraParams(emptyList(), "abc123xyz")
            )
        )
    }

    private val String.data: String
        get() = objectMapper.writeValueAsString(objectMapper.readValue(this, PullRequestEvent::class.java).data)

}
