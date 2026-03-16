package pl.allegro.tech.github.botorchestrator

import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.mockito.BDDMockito.given
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import pl.allegro.tech.github.botorchestrator.domain.Repository
import pl.allegro.tech.github.botorchestrator.domain.comment.CommentUrlGenerator
import pl.allegro.tech.github.botorchestrator.infra.github.oidc.OidcTokenDecoder
import java.util.UUID

class CommentsIntSpec(
    private val commentUrlGenerator: CommentUrlGenerator,
    private val rest: MockMvc
) : BaseIntegrationSpec() {

    @MockitoBean
    lateinit var tokenVerifier: OidcTokenDecoder

    val workflowRepository = Repository(Repository.Id("some-owner", "some-repository"))
    val targetRepository = Repository(Repository.Id("some-owner", "another-repository"))
    val pullRequestNumber = 13
    val slotId = UUID.randomUUID()
    val existingCommentId = 1
    val jwtToken = "123"

    init {
        beforeAny {
            given(tokenVerifier.verifyAndRetrieveRepository(jwtToken)).willReturn(workflowRepository.fullName)
            githubApiMock.stubTokenResponse()
        }

        test("should be able to use previously generated comment callback url") {
            // given
            stubSuccessOnPublishingGithubComment()
            val url = commentUrlGenerator.generate(workflowRepository, targetRepository, pullRequestNumber, slotId)

            // when
            val request = """{ "body": "Some comment" }"""
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = request
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentPublished(
                repo = targetRepository,
                pullRequestNumber = pullRequestNumber,
                expectedRequest = request
            )
        }

        test("should fail to use callback url when different repository than expected found in token") {
            // given
            val url = commentUrlGenerator.generate(workflowRepository, targetRepository, pullRequestNumber, slotId)

            // when
            val request = """{ "body": "Some comment" }"""
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", "some jwt token")
                content = request
            }.andExpect {
                status { isForbidden() }
            }
        }

        test("should append comment by default") {
            // given
            stubSuccessOnPublishingGithubComment()
            val url = commentUrlGenerator.generate(workflowRepository, targetRepository, pullRequestNumber, slotId)

            // when
            val request = """{ "body": "Some comment" }"""
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = request
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentPublished(
                repo = targetRepository,
                pullRequestNumber = pullRequestNumber,
                expectedRequest = request
            )

            // and when
            val secondRequest = """
                { "body": "Other comment" }
                """.trimIndent()
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = secondRequest
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentUpdated(
                repo = targetRepository,
                commentId = existingCommentId,
                expectedRequest = """
                    { "body": "Some comment\nOther comment" }
                """.trimIndent()
            )
        }

        test("should be able to overwrite previous comment") {
            // given
            stubSuccessOnPublishingGithubComment()
            val url = commentUrlGenerator.generate(workflowRepository, targetRepository, pullRequestNumber, slotId)

            // when
            val request = """{ "body": "Some comment" }"""
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = request
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentPublished(
                repo = targetRepository,
                pullRequestNumber = pullRequestNumber,
                expectedRequest = request
            )

            // and when
            val secondRequest = """
                { "body": "Other comment", "mode": "OVERWRITE" }
                """.trimIndent()
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = secondRequest
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentUpdated(
                repo = targetRepository,
                commentId = existingCommentId,
                expectedRequest = """
                    { "body": "Other comment" }
                """.trimIndent()
            )
        }

        test("should be able to create multiple comments") {
            // given
            stubSuccessOnPublishingGithubComment()
            val url = commentUrlGenerator.generate(workflowRepository, targetRepository, pullRequestNumber, slotId)

            // when
            val request = """{ "body": "Some comment" }"""
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = request
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentPublished(
                repo = targetRepository,
                pullRequestNumber = pullRequestNumber,
                expectedRequest = request
            )

            // and when
            val secondRequest = """
                { "body": "Other comment", "mode": "NEW" }
                """.trimIndent()
            rest.post(url) {
                contentType = APPLICATION_JSON
                header("Authorization", jwtToken)
                content = secondRequest
            }.andExpect {
                status { isOk() }
            }

            // then
            githubApiMock.verifyCommentPublished(
                repo = targetRepository,
                pullRequestNumber = pullRequestNumber,
                expectedRequest = request
            )
        }

    }

    private fun stubSuccessOnPublishingGithubComment() {
        githubApiMock.stubFor(
            get(urlMatching("/repos/.*/.*/issues/.*/comments\\?sort=created&direction=desc&per_page=100"))
                .willReturn(ok("[]").withHeader("Content-Type", APPLICATION_JSON_VALUE))
        )

        githubApiMock.stubFor(
            patch(urlMatching("/repos/.*/.*/issues/comments/.*"))
                .willReturn(ok("""{"id": ${existingCommentId}}""").withHeader("Content-Type", APPLICATION_JSON_VALUE))
        )

        githubApiMock.stubFor(
            post(urlMatching("/repos/.*/.*/issues/.*/comments")).willReturn(ok())
                .willReturn(ok("""{"id": ${existingCommentId}}""").withHeader("Content-Type", APPLICATION_JSON_VALUE))
        )
    }
}
