package pl.allegro.tech.common.pullrequestmanager.api

import com.fasterxml.jackson.annotation.JsonProperty

data class CheckRunEvent(
    @field:JsonProperty("event") val data: CheckRunEventPayload,
    val delivery: String? = null
) {
    data class CheckRunEventPayload(
        val repository: GithubRepo,
        val sender: GithubPrincipal,
        val action: String,
        @field:JsonProperty("check_run") val checkRun: GithubCheckRun
    )
}

data class GithubCheckRun(
    val id: Long,
    val name: String,
    @field:JsonProperty("head_sha") val headSha: String,
    val conclusion: String? = null,
    @field:JsonProperty("started_at") val startedAt: String? = null,
    @field:JsonProperty("completed_at") val completedAt: String? = null,
    @field:JsonProperty("pull_requests") val pullRequests: List<GithubPullRequestRef> = emptyList(),
    val app: GithubApp? = null
)

data class GithubPullRequestRef(val number: Int)

data class GithubApp(val slug: String? = null)
