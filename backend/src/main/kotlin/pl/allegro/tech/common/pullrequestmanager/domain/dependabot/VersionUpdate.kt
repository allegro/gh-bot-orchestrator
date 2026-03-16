package pl.allegro.tech.common.pullrequestmanager.domain.dependabot

import com.github.zafarkhaja.semver.Version
import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent.PullRequestEventPayload

data class VersionUpdate(val artifact: String, val from: Version, val to: Version) {
    companion object {

        private val logger = KotlinLogging.logger {}
        private val DEPENDABOT_BUMP_PATTERN =
            "^.*(?>Updates|Bumps) `?(?<artifact>[^ ]*?)`? from v?(?<from>\\d[^ ]*) to v?(?<to>\\d[^ \n]*?)\\.?\$".toRegex(
                RegexOption.MULTILINE
            )

        fun from(pullRequestDetails: PullRequestEventPayload): List<VersionUpdate> {
            try {
                return DEPENDABOT_BUMP_PATTERN.findAll(pullRequestDetails.pullRequest.body ?: "")
                    .mapNotNull(Companion::buildVersionUpdate)
                    .toList()
            } catch (e: Exception) {
                logger.error(e) { "Can't infer versions from PR: $pullRequestDetails" }
                return emptyList()
            }
        }

        private fun buildVersionUpdate(match: MatchResult): VersionUpdate? {
            try {
                val artifact = match.groups["artifact"]?.value?.let(Companion::sanitizeArtifactName)
                val from = match.toVersion("from")
                val to = match.toVersion("to")
                return if (artifact != null && from != null && to != null) VersionUpdate(artifact, from, to) else null
            } catch (e: Exception) {
                logger.error(e) { "Can't infer versions from matched string: ${match.groups}" }
                return null
            }
        }

        /**
         * Trims artifact name and, in case it is a markdown link, extracts the label
         */
        private fun sanitizeArtifactName(name: String): String {
            val trimmed = name.trim()
            return if (trimmed.startsWith('[')) {
                trimmed.substring(1, trimmed.lastIndexOf(']'))
            } else {
                trimmed
            }
        }

        private fun MatchResult.toVersion(groupName: String): Version? = this.groups[groupName]?.value?.let { Version.parse(it, false) }
    }
}
