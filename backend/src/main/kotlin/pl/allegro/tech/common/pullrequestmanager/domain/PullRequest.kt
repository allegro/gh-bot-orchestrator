package pl.allegro.tech.common.pullrequestmanager.domain

import pl.allegro.tech.common.pullrequestmanager.domain.dependabot.VersionUpdate
import java.time.Instant

data class PullRequest(
    val originalEventString: String,
    val repository: Repository,
    val number: Int,
    val headSha: String,
    val lastUpdateTimestamp: Instant,
    val dependabotMetadata: DependabotMetadata?,
    val customRepositoryProperties: Map<String, Any>,
) {
    fun fullName(): String {
        return "${repository.fullName}#$number"
    }
}

data class DependabotMetadata(
    val versionUpdates: List<VersionUpdate>,
)
