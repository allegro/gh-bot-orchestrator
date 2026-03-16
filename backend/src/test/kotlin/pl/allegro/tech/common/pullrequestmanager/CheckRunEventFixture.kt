package pl.allegro.tech.common.pullrequestmanager

import pl.allegro.tech.common.pullrequestmanager.api.CheckRunEvent
import pl.allegro.tech.common.pullrequestmanager.api.GithubApp
import pl.allegro.tech.common.pullrequestmanager.api.GithubCheckRun
import pl.allegro.tech.common.pullrequestmanager.api.GithubPrincipal
import pl.allegro.tech.common.pullrequestmanager.api.GithubPullRequestRef
import pl.allegro.tech.common.pullrequestmanager.api.GithubRepo
import java.util.UUID

object CheckRunEventFixture {

    fun checkRunEvent(
        action: String = "completed",
        conclusion: String = "success",
        appSlug: String = "github-actions",
        checkRunName: String = "build-and-test",
        headSha: String = "d1a6c13bff04dc0f090f97c57a61ddd9f8af1816",
        pullRequests: List<GithubPullRequestRef> = listOf(GithubPullRequestRef(number = 42)),
        startedAt: String? = "2025-01-15T10:00:00Z",
        completedAt: String? = "2025-01-15T10:05:00Z",
        delivery: String = UUID.randomUUID().toString()
    ): CheckRunEvent = CheckRunEvent(
        data = CheckRunEvent.CheckRunEventPayload(
            action = action,
            checkRun = GithubCheckRun(
                id = 99887766L,
                name = checkRunName,
                headSha = headSha,
                conclusion = conclusion,
                startedAt = startedAt,
                completedAt = completedAt,
                pullRequests = pullRequests,
                app = GithubApp(slug = appSlug)
            ),
            repository = GithubRepo(
                id = 1000L,
                name = "some-repository",
                fullName = "some-owner/some-repository",
                owner = GithubPrincipal(login = "some-owner", id = 77007731L)
            ),
            sender = GithubPrincipal(login = "github-actions[bot]", id = 41898282L)
        ),
        delivery = delivery
    )
}
