package pl.allegro.tech.github.botorchestrator.infra.task

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration as JavaDuration

@ConfigurationProperties(prefix = "app.tasks.handle-check-run")
data class ReactToCheckRunProps(
    val sleepDuration: JavaDuration,
    val noSlotsSleepDuration: JavaDuration
)
