package pl.allegro.tech.common.pullrequestmanager.infra.task

import com.github.kagkarlsson.scheduler.task.FailureHandler.OnFailureRetryLater
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import pl.allegro.tech.common.pullrequestmanager.application.CheckRunEventHandler
import pl.allegro.tech.common.pullrequestmanager.application.PullRequestEventHandler
import pl.allegro.tech.common.pullrequestmanager.domain.NoSlotsAvailable
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.tech.postgrestaskscheduler.handler.HandlerFactory

@Configuration
class TasksConfig(
    private val props: ReactToPullRequestChangeProps,
    private val eventHandler: PullRequestEventHandler,
    private val checkRunEventHandler: CheckRunEventHandler,
    private val workflowDefinitionsReader: WorkflowDefinitionsReader
) {

    @Bean
    fun reactToPullRequestsChanges(handlerFactory: HandlerFactory): OneTimeTask<ReactToPullRequestChangeTaskData> {
        return Tasks
            .oneTime(HANDLE_PULL_REQUEST.raw, ReactToPullRequestChangeTaskData::class.java)
            .onFailure(OnFailureRetryLater(props.sleepDuration))
            .onDeadExecutionRevive()
            .execute(handlerFactory.create { task, _ ->
                try {
                    MDC.put("pullRequest", task.data.event.data.pullRequest.htmlUrl)
                    MDC.put("pullRequestNumber", task.data.event.data.pullRequest.number.toString())
                    MDC.put("repo", task.data.event.data.repository.fullName)
                    MDC.put("ref", task.data.event.data.pullRequest.head.ref)
                    MDC.put("workflowId", task.data.workflowId.toString())
                    MDC.put("delivery", task.data.event.delivery)
                    MDC.put("action", task.data.event.data.action)

                    logger.info { "Handling task ${task.id} for PR ${task.data.event.data.pullRequest.htmlUrl}" }
                    val workflowConfiguration = workflowDefinitionsReader.getDefinitionFor(task.data.workflowId)
                    eventHandler.handle(workflowConfiguration, task.data.event)
                } catch (e: NoSlotsAvailable) {
                    logger.error(e) { "No available slots for ${task.taskName}/${task.id} for PR ${task.data.event.data.pullRequest.htmlUrl}" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed handling ${task.id} for PR ${task.data.event.data.pullRequest.htmlUrl}" }
                } finally {
                    MDC.remove("pullRequest")
                    MDC.remove("pullRequestNumber")
                    MDC.remove("repo")
                    MDC.remove("ref")
                    MDC.remove("workflowId")
                    MDC.remove("action")
                    MDC.remove("delivery")
                }
            })
    }

    @Bean
    fun reactToCheckRunEvents(handlerFactory: HandlerFactory): OneTimeTask<ReactToCheckRunTaskData> {
        return Tasks
            .oneTime(HANDLE_CHECK_RUN.raw, ReactToCheckRunTaskData::class.java)
            .onFailure(OnFailureRetryLater(props.sleepDuration))
            .onDeadExecutionRevive()
            .execute(handlerFactory.create { task, _ ->
                try {
                    MDC.put("repo", task.data.event.data.repository.fullName)
                    MDC.put("checkRunId", task.data.event.data.checkRun.id.toString())
                    MDC.put("pullRequestNumber", task.data.pullRequestNumber.toString())
                    MDC.put("workflowId", task.data.workflowId.toString())
                    MDC.put("action", task.data.event.data.action)

                    logger.info { "Handling task ${task.id} for check_run ${task.data.event.data.checkRun.id} and pull_request #${task.data.pullRequestNumber}" }
                    val workflowConfiguration = workflowDefinitionsReader.getDefinitionFor(task.data.workflowId)
                    checkRunEventHandler.handle(workflowConfiguration, task.data.event, task.data.pullRequestNumber)
                } catch (e: NoSlotsAvailable) {
                    logger.error(e) { "No available slots for ${task.taskName}/${task.id} for check_run ${task.data.event.data.checkRun.id}" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed handling ${task.id} for check_run ${task.data.event.data.checkRun.id}" }
                } finally {
                    MDC.remove("repo")
                    MDC.remove("checkRunId")
                    MDC.remove("pullRequestNumber")
                    MDC.remove("workflowId")
                    MDC.remove("action")
                }
            })
    }

    @Bean
    fun cleanUpStaleContexts(jdbcTemplate: JdbcTemplate): CleanUpStaleContexts {
        return CleanUpStaleContexts(jdbcTemplate)
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
