package pl.allegro.tech.common.pullrequestmanager.domain.filtering

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

class DependabotMatcher(val artifactRegex: Regex) : PullRequestMatcher {

    constructor(pattern: String) : this(pattern.toRegex())

    override fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto>): Boolean {
        val matches = pullRequest.dependabotMetadata?.versionUpdates?.any { v -> artifactRegex.matches(v.artifact) } ?: false
        if (!matches) {
            logger.atDebug {
                message = "PR did not match Dependabot filter for regex: $artifactRegex"
                payload = mapOf("dependabotVersionUpdates" to pullRequest.dependabotMetadata?.versionUpdates?.joinToString())
            }
        }

        return matches
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
