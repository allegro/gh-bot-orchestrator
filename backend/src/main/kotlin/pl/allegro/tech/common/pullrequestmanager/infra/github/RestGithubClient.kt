package pl.allegro.tech.common.pullrequestmanager.infra.github

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpHeaders.ACCEPT
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.web.client.RestClient
import pl.allegro.tech.common.andamio.metrics.micrometer.Timed
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunUpdateRequest
import pl.allegro.tech.common.pullrequestmanager.domain.CheckDetails
import pl.allegro.tech.common.pullrequestmanager.domain.CheckRunUpdated
import pl.allegro.tech.common.pullrequestmanager.domain.CreateCheckRunResult
import pl.allegro.tech.common.pullrequestmanager.domain.FailedToCreateCheck
import pl.allegro.tech.common.pullrequestmanager.domain.FailedToUpdateCheckRun
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.UpdateCheckRunResult
import pl.allegro.tech.common.pullrequestmanager.domain.Workflow
import pl.allegro.tech.common.pullrequestmanager.domain.WorkflowInputs
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.DetailsPage
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.TokenProvider
import java.util.UUID

typealias Conclusion = String

//TODO: should be migrated to com.spotify:github-client
class RestGithubClient(
    private val githubBaseUrl: String,
    private val tokenProvider: TokenProvider,
    private val restClient: RestClient,
    private val objectMapper: ObjectMapper
) : GithubClient {

    override fun dispatchWorkflow(workflow: Workflow, inputs: WorkflowInputs) {
        logger.info { "Dispatching workflow ${workflow.workflowFileName}" }
        val request = TriggerWorkflowRequest(workflow.ref, inputs.toDto())

        val result = restClient.post()
            .uri("$githubBaseUrl/repos/${workflow.repository.owner}/${workflow.repository.name}/actions/workflows/${workflow.workflowFileName}/dispatches")
            .headers(::buildHeaders)
            .body(request)
            .toResult<Any>()
            .mapError { error ->
                logger.error(error) { "Failed to dispatch workflow" }
                GithubClientException(
                    "Unable to trigger a workflow: ${workflow.repository.owner}/${workflow.repository.name}/${workflow.workflowFileName}",
                    error
                )
            }
            .getOrThrow()

        logger.info { "Workflow dispatch result: $result" }
    }

    override fun createCheck(repository: Repository, headSha: String, name: String, detailsPage: DetailsPage, externalId: String): CreateCheckRunResult {
        logger.info { "Creating check for repo $repository and sha $headSha" }
        val request = CreateCheckRequest(
            headSha = headSha,
            name = name,
            output = detailsPage.toOutput(),
            detailsUrl = detailsPage.customUrl,
            status = "in_progress",
            externalId = externalId,
        )

        val result = restClient.post()
            .uri("$githubBaseUrl/repos/${repository.id.owner}/${repository.id.name}/check-runs")
            .headers(::buildHeaders)
            .body(request)
            .toResult<CreateCheckResponse>()

        val exception = result.exceptionOrNull()

        return if (exception != null && exception is GithubNonRetriableException) {
            FailedToCreateCheck(exception.message)
        } else if (exception != null) {
            logger.error(exception) { "Failed to create check for repo ${repository.fullName} with name $name" }
            throw exception
        } else {
            val checkId = result.getOrThrow()!!.id
            CheckDetails(repository = repository, checkId = checkId)
        }
    }

    override fun updateCheck(
        checkId: String,
        owner: String,
        repo: String,
        conclusion: Conclusion,
        detailsPage: CheckRunUpdateRequest.DetailsPageUpdateRequest?
    ): UpdateCheckRunResult {
        logger.info { "Updating check: PATCH $githubBaseUrl/repos/$owner/$repo/check-runs/$checkId with conclusion=$conclusion" }
        val request = UpdateCheckRequest(
            output = detailsPage?.toOutput(),
            detailsUrl = detailsPage?.customUrl,
            conclusion = conclusion
        )

        val result = restClient.patch()
            .uri("$githubBaseUrl/repos/$owner/$repo/check-runs/${checkId}")
            .headers(::buildHeaders)
            .body(request)
            .toResult<Any>()

        val exception = result.exceptionOrNull()

        return if (exception != null && exception is GithubNonRetriableException) {
            logger.error(exception) { "GitHub rejected check update for $checkId in $owner/$repo with conclusion $conclusion (non-retriable)" }
            FailedToUpdateCheckRun(exception.message)
        } else {
            if (exception != null) throw exception
            else CheckRunUpdated
        }
    }

    override fun publishNewComment(repository: Repository, pullRequestNumber: Int, body: String, commentId: UUID): Long {
        logger.atInfo {
            message = "Publishing new comment $commentId $body $pullRequestNumber $repository"
            payload = mapOf(
                "repo" to repository,
                "pullRequest" to pullRequestNumber,
                "body" to body,
                "commentId" to commentId
            )
        }

        val existingCommentId = findExistingComment(repository, pullRequestNumber, body)
        if (existingCommentId != null) {
            return existingCommentId
        }

        val request = PublishGithubCommentDto(body = body)

        return restClient.post()
            .uri("$githubBaseUrl/repos/${repository.id.owner}/${repository.id.name}/issues/${pullRequestNumber}/comments")
            .headers(::buildHeaders)
            .body(request)
            .toResult<PublishCommentResponse>()
            .getOrNull()
            ?.id
            ?: error("Failed to publish comment $commentId")
    }

    override fun overwriteComment(repository: Repository, pullRequestNumber: Int, body: String, githubCommentId: Long) {
        logger.atInfo {
            message = "Overwrite comment $githubCommentId $body $pullRequestNumber $repository"
            payload = mapOf(
                "repo" to repository,
                "pullRequestNumber" to pullRequestNumber,
                "body" to body,
                "commentId" to githubCommentId
            )
        }

        val request = PublishGithubCommentDto(body = body)

        restClient.patch()
            .uri("$githubBaseUrl/repos/${repository.id.owner}/${repository.id.name}/issues/comments/${githubCommentId}")
            .headers(::buildHeaders)
            .body(request)
            .toResult<PublishCommentResponse>()
            .getOrNull()
            ?: error("Failed to update comment $githubCommentId")
    }

    private fun findExistingComment(repository: Repository, pullRequestNumber: Int, body: String): Long? {
        val response = getAllComments(repository, pullRequestNumber)
        return response.firstOrNull { it.body == body }
            ?.id
    }

    private fun getAllComments(repo: Repository, pullRequestNumber: Int): List<GithubCommentDto> {
        return restClient.get()
            .uri("$githubBaseUrl/repos/${repo.id.owner}/${repo.id.name}/issues/${pullRequestNumber}/comments?sort=created&direction=desc&per_page=100")
            .headers(::buildHeaders)
            .toResult<List<GithubCommentDto>>()
            .getOrNull()
            ?: error("Could not find comments for pull request $pullRequestNumber in repository ${repo.fullName}")
    }

    @Timed(name = "github_response_time")
    override fun getPullRequestFiles(repo: Repository, pullRequestNumber: Int): Sequence<PullRequestFileDto> {
        val uri = "$githubBaseUrl/repos/${repo.id.owner}/${repo.id.name}/pulls/${pullRequestNumber}/files"
        return paginate(uri) { paginatedUri ->
            restClient.get()
                .uri(paginatedUri)
                .headers(::buildHeaders)
                .toResultEntity<List<PullRequestFileDto>>()
        }
    }

    override fun getWorkflowToken(repo: Repository): String? {
        try {
            return tokenProvider.getWorkflowToken(repo)
        } catch (e: Exception) {
            logger.error(e) { "Failed to get workflow token for $repo" }
            return null
        }
    }

    private fun buildHeaders(headers: HttpHeaders) {
        headers.set(GITHUB_API_VERSION_HEADER_NAME, GITHUB_API_VERSION_HEADER_VALUE)
        headers.set(AUTHORIZATION, "Bearer ${tokenProvider.getToken()}")
        headers.set(ACCEPT, GITHUB_CONTENT_TYPE)
    }

    private data class TriggerWorkflowRequest(
        val ref: String,
        val inputs: WorkflowInputsDto
    )

    @JsonInclude(NON_NULL)
    private data class CreateCheckRequest(
        @JsonProperty("head_sha") val headSha: String,
        val name: String,
        val output: Output,
        @JsonProperty("details_url") val detailsUrl: String?,
        val status: String,
        @JsonProperty("external_id") val externalId: String?,
    )

    @JsonInclude(NON_NULL)
    private data class UpdateCheckRequest(
        val output: Output?,
        @JsonProperty("details_url") val detailsUrl: String?,
        val conclusion: Conclusion
    )

    private data class CreateCheckResponse(val id: String)

    private fun WorkflowInputs.toDto(): WorkflowInputsDto {
        return WorkflowInputsDto(
            event = event,
            workflowConcurrencyGroup = workflowConcurrencyGroup,
            concurrencyGroup = concurrencyGroup,
            checkCallbackUrl = checkCallbackUrl,
            commentCallbackUrl = commentCallbackUrl,
            slotId = slotId.toString(),
            extraParams = objectMapper.writeValueAsString(extraParams)
        )
    }

    private data class WorkflowInputsDto(
        val event: String,
        @JsonProperty("workflow-concurrency-group") val workflowConcurrencyGroup: String,
        @JsonProperty("concurrency-group") val concurrencyGroup: String,
        @JsonProperty("slot-id") val slotId: String,
        @JsonProperty("check-callback-url") val checkCallbackUrl: String?,
        @JsonProperty("comment-callback-url") val commentCallbackUrl: String,
        @JsonProperty("extra-params") val extraParams: String
    )

    companion object {
        private val logger = KotlinLogging.logger { }

        const val GITHUB_API_VERSION_HEADER_NAME = "X-GitHub-Api-Version"
        const val GITHUB_API_VERSION_HEADER_VALUE = "2022-11-28"
        const val GITHUB_CONTENT_TYPE = "application/vnd.github+json"
    }
}

@JsonInclude(NON_NULL)
data class Output(
    val title: String,
    val summary: String,
    val text: String?
)

private fun CheckRunUpdateRequest.DetailsPageUpdateRequest.toOutput(): Output {
    return Output(
        title = title,
        summary = summary,
        text = details
    )
}

private fun DetailsPage.toOutput(): Output {
    return Output(
        title = title,
        summary = summary,
        text = details
    )
}

private data class PublishGithubCommentDto(val body: String)
private data class GithubCommentDto(val body: String, val id: Long)
data class PullRequestFileDto(
    val filename: String,
    val status: String
)

private data class PublishCommentResponse(val id: Long)
