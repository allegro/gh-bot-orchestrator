package pl.allegro.tech.github.botorchestrator

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import com.github.zafarkhaja.semver.Version
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.util.ClassUtils
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.BODY
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.BRANCH
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.COMMIT
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.PR_NUMBER
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.PR_TITLE
import pl.allegro.tech.github.botorchestrator.PullRequestEndpointIntSpec.Companion.SC_ID
import pl.allegro.tech.github.botorchestrator.domain.ExtraParams
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.dependabot.VersionUpdate
import pl.allegro.tech.github.botorchestrator.domain.filtering.CompoundPullRequestMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.DependabotMatcher
import pl.allegro.tech.github.botorchestrator.domain.filtering.JsonPathMatcher
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinition.MatchingStrategy.ALL
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class PullRequestEndpointIntSpec(val rest: MockMvc) : BaseIntegrationSpec() {

    init {
        beforeAny {
            githubApiMock.stubTokenResponse()
        }

        test("should trigger all workflows defined in config that match filters") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            val someOtherWorkflow = workflowDefinitionFixtures.randomWorkflow(fileName = "someOtherWorkflow.yml")

            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent()
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow)
                githubApiMock.verifyWorkflowDispatch(someOtherWorkflow)
            }
        }

        test("should trigger workflow for dependabot PRs when dependabot filter matches") {
            // given
            val dependabotWorkflow = workflowDefinitionFixtures.randomWorkflow(
                filters = CompoundPullRequestMatcher(
                    matchingStrategy = ALL,
                    matchers = listOf(
                        JsonPathMatcher(
                            path = "$.action",
                            pattern = "opened|synchronize"
                        ),
                        JsonPathMatcher(
                            path = "$.pull_request.user.login",
                            pattern = "^(?!dependabot[bot]).*$"
                        ),
                        DependabotMatcher(
                            pattern = "com.example:some-lib.*",
                        )
                    )
                )
            )

            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent()
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(
                    dependabotWorkflow, ExtraParams(
                        listOf(
                            VersionUpdate(
                                "com.example:some-lib-platform", Version.parse("6.2.0"),
                                Version.parse("8.1.0")
                            )
                        ), null
                    )
                )
            }
        }

        test("should ignore non-dependabot PRs when dependabot filter is present") {
            // given
            val dependabotWorkflow = workflowDefinitionFixtures.randomWorkflow(
                filters = CompoundPullRequestMatcher(
                    matchingStrategy = ALL,
                    matchers = listOf(
                        JsonPathMatcher(
                            path = "$.action",
                            pattern = "opened|synchronize"
                        ),
                        JsonPathMatcher(
                            path = "$.pull_request.user.login",
                            pattern = "^(?!dependabot[bot]).*$"
                        ),
                        DependabotMatcher(
                            pattern = "com.example:some-lib.*",
                        )
                    )
                )
            )
            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(body = "not dependabot")
            }.andExpect {
                status { isOk() }
            }

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(dependabotWorkflow)
            }
        }

        test("should not trigger workflow for dependabot PRs when dependabot filter does not match") {
            // given
            val dependabotWorkflow = workflowDefinitionFixtures.randomWorkflow(
                filters = CompoundPullRequestMatcher(
                    matchingStrategy = ALL,
                    matchers = listOf(
                        JsonPathMatcher(
                            path = "$.action",
                            pattern = "opened|synchronize"
                        ),
                        JsonPathMatcher(
                            path = "$.pull_request.user.login",
                            pattern = "^(?!dependabot[bot]).*$"
                        ),
                        DependabotMatcher(
                            pattern = "com.example:some-lib.*",
                        )
                    )
                )
            )
            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(body = "Bumps some-library from 6.2.0 to 8.1.0.")
            }.andExpect {
                status { isOk() }
            }

            // then
            continually(1.seconds) {
                githubApiMock.noWorkflowWasDispatched(dependabotWorkflow)
            }
        }

        test("should not create check twice on duplicate pull request event") {
            // given
            val headSha = RandomStringUtils.secure().nextAlphabetic(10)
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(headSha = headSha)
            }.andExpect {
                status { isOk() }
            }

            // same `updatedAt` on the PR - treating it like the same event
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(headSha = headSha)
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow)
                githubApiMock.verifyCheckCreated(Repository(Repository.Id("some-owner", "some-repository")), """
                |{
                |  "head_sha" : "$headSha",
                |  "name" : "some check name",
                |  "output" : {
                |    "title" : "Some title",
                |    "summary" : "Some summary",
                |    "text" : "Some details"
                |  },
                |  "status" : "in_progress",
                |  "external_id" : "${'$'}{json-unit.any-string}"
                |}
                """.trimMargin())
            }
        }

        test("should dispatch new workflow and create new check on new commit in the same PR") {
            // given
            var headSha = RandomStringUtils.secure().nextAlphabetic(10)
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()
            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(headSha = headSha)
            }.andExpect {
                status { isOk() }
            }

            // Wait for first workflow to be dispatched before sending the second event
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow, countMatchingStrategy = WireMock.exactly(1))
            }

            // new commit to the PR
            var newHeadSha = RandomStringUtils.secure().nextAlphabetic(10)
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(updatedAt = Instant.now(), headSha = newHeadSha)
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(10.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow, countMatchingStrategy = WireMock.exactly(2))
                githubApiMock.verifyCheckCreated(Repository(Repository.Id("some-owner", "some-repository")), """
                |{
                |  "head_sha" : "$headSha",
                |  "name" : "some check name",
                |  "output" : {
                |    "title" : "Some title",
                |    "summary" : "Some summary",
                |    "text" : "Some details"
                |  },
                |  "status" : "in_progress",
                |  "external_id" : "${'$'}{json-unit.any-string}"
                |}
                """.trimMargin(), WireMock.exactly(1))
                githubApiMock.verifyCheckCreated(Repository(Repository.Id("some-owner", "some-repository")), """
                |{
                |  "head_sha" : "$newHeadSha",
                |  "name" : "some check name",
                |  "output" : {
                |    "title" : "Some title",
                |    "summary" : "Some summary",
                |    "text" : "Some details"
                |  },
                |  "status" : "in_progress",
                |  "external_id" : "${'$'}{json-unit.any-string}"
                |}
                """.trimMargin(), WireMock.moreThanOrExactly(1))
            }
        }

        test("should supply read-only token when it was generated successfully") {
            // given
            val token = "some generated token"
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()

            githubApiMock.stubWorkflowTokenResponse("some-repository", token)
            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(body = "")
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow, extraParams = ExtraParams(emptyList(), token))
            }
        }

        test("should not supply read-only token when it can't be generated") {
            // given
            val someWorkflow = workflowDefinitionFixtures.randomWorkflow()

            githubApiMock.stubFor(post(urlMatching("/repos/.*/.*/actions/workflows/.*/dispatches")).willReturn(ok()))
            githubApiMock.stubFor(
                post(urlMatching("/repos/.*/.*/check-runs")).willReturn(
                    ok()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{ "id": 123}""")
                )
            )

            // when
            rest.post("/pull-requests") {
                contentType = MediaType.APPLICATION_JSON
                content = examplePullRequestEvent(body = "")
            }.andExpect {
                status { isOk() }
            }

            // then
            eventually(5.seconds) {
                githubApiMock.verifyWorkflowDispatch(someWorkflow, extraParams = ExtraParams(emptyList(), null))
            }
        }
    }

    companion object {

        const val PR_NUMBER = 42L
        const val PR_TITLE = "Bump com.example:some-lib-platform from 7.2.0 to 8.0.0"
        const val SC_ID = "sc-2137"
        const val COMMIT = "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816"
        const val BRANCH = "some-branch-with-pr"
        const val BODY = "Bumps com.example:some-lib-platform from 6.2.0 to 8.1.0."
    }
}

fun examplePullRequestEvent(
    body: String = BODY,
    headSha: String = COMMIT,
    updatedAt: Instant = Instant.parse("2024-12-17T11:56:39Z"),
    delivery: String = UUID.randomUUID().toString(),
): String =
    getResourceAsText("example/pull-request-event.template.json")!!
        .replace("{org}", "some-owner")
        .replace("{repo}", "some-repository")
        .replace("{number}", PR_NUMBER.toString())
        .replace("{commit}", headSha)
        .replace("{branch}", BRANCH)
        .replace("{scId}", SC_ID)
        .replace("{title}", PR_TITLE)
        .replace("{body}", body)
        .replace("{updated_at}", updatedAt.toString())
        .replace("{delivery}", delivery)

fun getResourceAsText(path: String): String? = ClassUtils.getDefaultClassLoader()?.getResource(path)?.readText()
