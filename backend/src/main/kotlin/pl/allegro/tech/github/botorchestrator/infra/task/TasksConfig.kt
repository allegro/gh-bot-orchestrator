package pl.allegro.tech.github.botorchestrator.infra.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.kagkarlsson.scheduler.boot.config.DbSchedulerCustomizer
import com.github.kagkarlsson.scheduler.serializer.JacksonSerializer
import com.github.kagkarlsson.scheduler.serializer.Serializer
import com.github.kagkarlsson.scheduler.task.FailureHandler.OnFailureRetryLater
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import pl.allegro.tech.github.botorchestrator.application.CheckRunEventHandler
import pl.allegro.tech.github.botorchestrator.application.PullRequestEventHandler
import pl.allegro.tech.github.botorchestrator.domain.NoSlotsAvailable
import pl.allegro.tech.github.botorchestrator.domain.workflows.WorkflowDefinitionsReader
import java.util.Optional

@Configuration
class TasksConfig(
    private val props: ReactToPullRequestChangeProps,
    private val eventHandler: PullRequestEventHandler,
    private val checkRunEventHandler: CheckRunEventHandler,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) {

    @Bean
    fun dbSchedulerCustomizer(objectMapper: ObjectMapper): DbSchedulerCustomizer {
        return object : DbSchedulerCustomizer {
            override fun serializer(): Optional<Serializer> {
                return Optional.of(JacksonSerializer(objectMapper))
            }
        }
    }

    @Bean
    fun reactToPullRequestsChanges(): OneTimeTask<ReactToPullRequestChangeTaskData> {
        return Tasks
            .oneTime(HANDLE_PULL_REQUEST)
            .onFailure(OnFailureRetryLater(props.sleepDuration))
            .onDeadExecutionRevive()
            .execute { inst, _ ->
                try {
                    MDC.put("pullRequest", inst.data.event.data.pullRequest.htmlUrl)
                    MDC.put("pullRequestNumber", inst.data.event.data.pullRequest.number.toString())
                    MDC.put("repo", inst.data.event.data.repository.fullName)
                    MDC.put("ref", inst.data.event.data.pullRequest.head.ref)
                    MDC.put("workflowId", inst.data.workflowId.toString())
                    MDC.put("delivery", inst.data.event.delivery)
                    MDC.put("action", inst.data.event.data.action)

                    logger.info { "Handling task ${inst.id} for PR ${inst.data.event.data.pullRequest.htmlUrl}" }
                    val workflowConfiguration = workflowDefinitionsReader.getDefinitionFor(inst.data.workflowId)
                    eventHandler.handle(workflowConfiguration, inst.data.event)
                } catch (e: NoSlotsAvailable) {
                    logger.error(e) { "No available slots for ${inst.taskName}/${inst.id} for PR ${inst.data.event.data.pullRequest.htmlUrl}" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed handling ${inst.id} for PR ${inst.data.event.data.pullRequest.htmlUrl}" }
                } finally {
                    MDC.remove("pullRequest")
                    MDC.remove("pullRequestNumber")
                    MDC.remove("repo")
                    MDC.remove("ref")
                    MDC.remove("workflowId")
                    MDC.remove("action")
                    MDC.remove("delivery")
                }
            }
    }

    @Bean
    fun reactToCheckRunEvents(): OneTimeTask<ReactToCheckRunTaskData> {
        return Tasks
            .oneTime(HANDLE_CHECK_RUN)
            .onFailure(OnFailureRetryLater(props.sleepDuration))
            .onDeadExecutionRevive()
            .execute { inst, _ ->
                try {
                    MDC.put("repo", inst.data.event.data.repository.fullName)
                    MDC.put("checkRunId", inst.data.event.data.checkRun.id.toString())
                    MDC.put("pullRequestNumber", inst.data.pullRequestNumber.toString())
                    MDC.put("workflowId", inst.data.workflowId.toString())
                    MDC.put("action", inst.data.event.data.action)

                    logger.info { "Handling task ${inst.id} for check_run ${inst.data.event.data.checkRun.id} and pull_request #${inst.data.pullRequestNumber}" }
                    val workflowConfiguration = workflowDefinitionsReader.getDefinitionFor(inst.data.workflowId)
                    checkRunEventHandler.handle(workflowConfiguration, inst.data.event, inst.data.pullRequestNumber)
                } catch (e: NoSlotsAvailable) {
                    logger.error(e) { "No available slots for ${inst.taskName}/${inst.id} for check_run ${inst.data.event.data.checkRun.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed handling ${inst.id} for check_run ${inst.data.event.data.checkRun.id}" }
                } finally {
                    MDC.remove("repo")
                    MDC.remove("checkRunId")
                    MDC.remove("pullRequestNumber")
                    MDC.remove("workflowId")
                    MDC.remove("action")
                }
            }
    }

    @Bean
    fun cleanUpStaleContexts(jdbcTemplate: JdbcTemplate): CleanUpStaleContexts {
        return CleanUpStaleContexts(jdbcTemplate)
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
