package pl.allegro.tech.common.pullrequestmanager

import pl.allegro.tech.common.pullrequestmanager.api.GithubPrincipal
import pl.allegro.tech.common.pullrequestmanager.api.GithubPullRequest
import pl.allegro.tech.common.pullrequestmanager.api.GithubRef
import pl.allegro.tech.common.pullrequestmanager.api.GithubRepo
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent

object PullRequestEventFixture {

    fun pullRequestEvent(applyModifications: PullRequestEvent.() -> PullRequestEvent = { this }): PullRequestEvent {
        return PullRequestEvent(
            data = PullRequestEvent.PullRequestEventPayload(
                repository = GithubRepo(
                    id = 123L,
                    name = "some-repository",
                    fullName = "some-org/some-repository",
                    owner = GithubPrincipal(
                        login = "some-org",
                        id = 1L
                    ),
                    topics = setOf("source")
                ),
                pullRequest = GithubPullRequest(
                    url = "http://example.com",
                    number = 234,
                    title = "Some PR title",
                    body = "Pull request introducing some feature",
                    user = GithubPrincipal(
                        login = "some-user",
                        id = 8L
                    ),
                    head = GithubRef(
                        ref = "som-ref",
                        sha = "909abe27a85c9accde8c1fdb19d0fb234cff3e1e",
                        repo = GithubRepo(
                            id = 123L,
                            name = "some-repository",
                            fullName = "some-org/some-repository",
                            owner = GithubPrincipal(
                                login = "some-org",
                                id = 1L
                            ),
                            topics = setOf()
                        )
                    ),
                    base = GithubRef(
                        ref = "master",
                        sha = "760ac584a765095f6c1941401fe703f130b882f9",
                        repo = GithubRepo(
                            id = 123L,
                            name = "some-repository",
                            fullName = "some-org/some-repository",
                            owner = GithubPrincipal(
                                login = "some-org",
                                id = 1L
                            ),
                            topics = setOf()
                        )
                    ),
                    htmlUrl = "http://example.com",
                    updatedAt = "2024-12-17T11:56:39.000Z",
                    merged = false
                ),
                sender = GithubPrincipal(
                    login = "dependabot[bot]",
                    id = 5L
                ),
                action = "opened"
            ),
            delivery = "123-456"
        ).applyModifications()
    }
}

fun PullRequestEvent.withAction(action: String): PullRequestEvent {
    return this.copy(data = data.copy(action = action))
}
