package pl.allegro.tech.common.pullrequestmanager.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.cleanup.workflow-run-availability")
data class WorkflowRunAvailabilityCleanupProperties(
    val interval: Duration,
    val maxAge: Duration
)
