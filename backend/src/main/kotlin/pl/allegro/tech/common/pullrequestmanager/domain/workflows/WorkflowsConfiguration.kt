package pl.allegro.tech.common.pullrequestmanager.domain.workflows

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import pl.allegro.tech.common.pullrequestmanager.infra.postgres.JdbcWorkflowDefinitionRepository

@Configuration
class WorkflowsConfiguration {

    @Bean
    fun jdbcWorkflowsConfigProvider(jdbcTemplate: NamedParameterJdbcTemplate): WorkflowDefinitionRepository =
        JdbcWorkflowDefinitionRepository(jdbcTemplate)
}
