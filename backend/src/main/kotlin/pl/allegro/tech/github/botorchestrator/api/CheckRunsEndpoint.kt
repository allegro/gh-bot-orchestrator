package pl.allegro.tech.github.botorchestrator.api

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.*
import pl.allegro.tech.github.botorchestrator.infra.task.ReactToCheckRunScheduler

@RestController
@RequestMapping("/check-runs")
class CheckRunsEndpoint(private val scheduler: ReactToCheckRunScheduler) {
    @PostMapping
    fun handle(@RequestBody event: CheckRunEvent) {
        logger.info { "Received check_run event with delivery=${event.delivery}, repo=${event.data.repository.fullName}, checkRunId=${event.data.checkRun.id}, action=${event.data.action}, conclusion=${event.data.checkRun.conclusion}" }
        scheduler.schedule(event)
    }

    companion object {
        private val logger = KotlinLogging.logger {}
    }
}
