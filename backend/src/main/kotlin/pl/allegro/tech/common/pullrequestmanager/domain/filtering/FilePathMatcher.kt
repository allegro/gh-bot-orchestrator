package pl.allegro.tech.common.pullrequestmanager.domain.filtering

import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

class FilePathMatcher(val files: Regex, val statuses: Set<String>) : PullRequestMatcher {

    constructor(files: String, statuses: Set<String>) : this(files.toRegex(), statuses)

    override fun requiresPullRequestFiles(): Boolean {
        return true
    }

    override fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto>): Boolean {
        return pullRequestFiles.any {
            val filenameMatches = files.matches(it.filename)
            val statusMatches = statuses.contains(it.status)

            logger.debug { "File '${it.filename}'. Result of matching with $files: $filenameMatches" }
            logger.debug { "File '${it.filename}' with status '${it.status}'. Result of matching with $statuses: $statusMatches" }

            filenameMatches && statusMatches
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
