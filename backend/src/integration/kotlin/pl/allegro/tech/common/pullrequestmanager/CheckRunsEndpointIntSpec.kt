package pl.allegro.tech.common.pullrequestmanager

import com.github.tomakehurst.wiremock.client.WireMock.exactly
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.filtering.JsonPathMatcher
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.MatchingStrategy.ALL
import kotlin.time.Duration.Companion.seconds

class CheckRunsEndpointIntSpec(val rest: MockMvc) : BaseIntegrationSpec() {

    init {
        beforeAny {
            githubApiMock.stubTokenResponse()
        }

        test("should dispatch workflow when check_run matches all filters") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            postCheckRunEvent(exampleCheckRunEvent())

            // then
            eventually(5.seconds) {
                githubApiMock.verifyCheckCreated(
                    repo = Repository("some-owner", "some-repository"),
                    expectedRequest = $$"""
                        {
                          "head_sha": "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816",
                          "name": "some check name",
                          "output": {
                            "title": "Some title",
                            "summary": "Some summary",
                            "text": "Some details"
                          },
                          "status": "in_progress",
                          "external_id": "${json-unit.any-string}"
                        }
                    """.trimIndent()
                )
                githubApiMock.verifyWorkflowDispatch(workflow)
            }
        }

        test("should not dispatch workflow when conclusion does not match") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            // when
            postCheckRunEvent(exampleCheckRunEvent(conclusion = "failure"))

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(workflow)
            }
        }

        test("should not dispatch workflow when action does not match") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            // when
            postCheckRunEvent(exampleCheckRunEvent(action = "created"))

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(workflow)
            }
        }

        test("should not dispatch workflow when app slug does not match") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            // when
            postCheckRunEvent(exampleCheckRunEvent(appSlug = "codecov"))

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(workflow)
            }
        }

        test("should not dispatch workflow when check_run name matches myself (infinite loop prevention)") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            // when
            postCheckRunEvent(exampleCheckRunEvent(checkRunName = "myself"))

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(workflow)
            }
        }

        test("should not dispatch workflow when check_run has no pull requests") {
            // given
            val workflow = workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            // when
            postCheckRunEvent(exampleCheckRunEvent(pullRequests = "[]"))
            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(workflow)
            }
        }

        test("should dispatch workflow for each pull request when check_run has multiple pull requests") {
            // given
            workflowDefinitionFixtures.randomWorkflow(filters = matchSuccessfulCheckExceptItself())

            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            postCheckRunEvent(exampleCheckRunEvent(pullRequests = """[{"number": 42}, {"number": 43}]"""))

            // then
            val repo = Repository("some-owner", "some-repository")
            eventually(5.seconds) {
                githubApiMock.verifyCheckCreated(
                    repo = repo,
                    expectedRequest = $$"""
                        {
                          "head_sha": "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816",
                          "name": "some check name",
                          "output": {
                            "title": "Some title",
                            "summary": "Some summary",
                            "text": "Some details"
                          },
                          "status": "in_progress",
                          "external_id": "${json-unit.any-string}"
                        }
                    """.trimIndent(),
                    countMatchingStrategy = exactly(2)
                )
            }
        }
    }

    private fun matchSuccessfulCheckExceptItself() = CompoundPullRequestMatcher(
        matchingStrategy = ALL,
        matchers = listOf(
            JsonPathMatcher("$.action", "completed"),
            JsonPathMatcher("$.check_run.conclusion", "success"),
            JsonPathMatcher("$.check_run.app.slug", "github-actions"),
            JsonPathMatcher("$.check_run.name", "^(?!myself).*$")
        )
    )

    private fun postCheckRunEvent(body: String) {
        rest.post("/check-runs") {
            contentType = MediaType.APPLICATION_JSON
            content = body
        }.andExpect {
            status { isOk() }
        }
    }

    private fun exampleCheckRunEvent(
        action: String = "completed",
        conclusion: String = "success",
        appSlug: String = "github-actions",
        checkRunName: String = "build-and-test",
        pullRequests: String = """[{"number": 42}]""",
        headSha: String = "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816",
        delivery: String = java.util.UUID.randomUUID().toString(),
        startedAt: String = "2025-01-15T10:00:00Z",
        completedAt: String = "2025-01-15T10:05:00Z"
    ): String {
        return """
            {
              "event": {
                "action": "$action",
                "check_run": {
                  "id": 99887766,
                  "name": "$checkRunName",
                  "head_sha": "$headSha",
                  "conclusion": "$conclusion",
                  "started_at": "$startedAt",
                  "completed_at": "$completedAt",
                  "app": { "slug": "$appSlug" },
                  "pull_requests": $pullRequests
                },
                "repository": {
                  "id": 1000,
                  "name": "some-repository",
                  "full_name": "some-owner/some-repository",
                  "owner": { "login": "some-owner", "id": 77007731 }
                },
                "sender": { "login": "github-actions[bot]", "id": 41898282 }
              },
              "delivery": "$delivery"
            }
        """.trimIndent()
    }
}
