package pl.allegro.tech.common.pullrequestmanager.domain

import com.fasterxml.jackson.annotation.JsonProperty
import pl.allegro.tech.common.pullrequestmanager.api.CheckRunUpdateRequest.DetailsPageUpdateRequest
import pl.allegro.tech.common.pullrequestmanager.domain.dependabot.VersionUpdate
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinition.DetailsPage
import pl.allegro.tech.common.pullrequestmanager.infra.github.Conclusion
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto
import java.util.*

interface GithubClient {

    fun dispatchWorkflow(workflow: Workflow, inputs: WorkflowInputs)
    fun createCheck(repository: Repository, headSha: String, name: String, detailsPage: DetailsPage, externalId: String): CreateCheckRunResult
    fun updateCheck(checkId: String, owner: String, repo: String, conclusion: Conclusion, detailsPage: DetailsPageUpdateRequest?): UpdateCheckRunResult

    fun publishNewComment(repository: Repository, pullRequestNumber: Int, body: String, commentId: UUID): Long
    fun overwriteComment(repository: Repository, pullRequestNumber: Int, body: String, githubCommentId: Long)
    fun getPullRequestFiles(repo: Repository, pullRequestNumber: Int): Sequence<PullRequestFileDto>

    fun getWorkflowToken(repo: Repository): String?
}

data class WorkflowInputs(
    val event: String,
    val concurrencyGroup: String,
    val checkCallbackUrl: String?,
    val commentCallbackUrl: String,
    val slotId: String,
    val workflowConcurrencyGroup: String,
    val extraParams: ExtraParams
)

data class ExtraParams(
    val dependabot: List<VersionUpdate>,
    @field:JsonProperty("github-clone-token") val githubCloneToken: String?
)

sealed interface CreateCheckRunResult

data class FailedToCreateCheck(val reason: String?) : CreateCheckRunResult

data class CheckDetails(val repository: Repository, val checkId: String) : CreateCheckRunResult {

    fun callbackUrl(urlProvider: UrlProvider): String {
        return "${urlProvider.baseUrl()}/check-callbacks/${repository.fullName}/${checkId}"
    }
}

sealed interface UpdateCheckRunResult
data object CheckRunUpdated : UpdateCheckRunResult
data class FailedToUpdateCheckRun(val reason: String?) : UpdateCheckRunResult
