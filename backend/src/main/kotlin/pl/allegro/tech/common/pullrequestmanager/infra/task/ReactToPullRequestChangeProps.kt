package pl.allegro.tech.common.pullrequestmanager.infra.task

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration as JavaDuration

@ConfigurationProperties(prefix = "app.tasks.handle-pull-request-change")
data class ReactToPullRequestChangeProps(
    val sleepDuration: JavaDuration
)
