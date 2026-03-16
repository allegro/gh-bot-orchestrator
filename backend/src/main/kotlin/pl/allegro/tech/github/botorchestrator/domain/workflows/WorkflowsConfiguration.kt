package pl.allegro.tech.github.botorchestrator.domain.workflows

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import pl.allegro.tech.github.botorchestrator.infra.postgres.JdbcWorkflowDefinitionRepository

@Configuration
class WorkflowsConfiguration {

    @Bean
    fun jdbcWorkflowsConfigProvider(jdbcTemplate: NamedParameterJdbcTemplate): WorkflowDefinitionRepository =
        JdbcWorkflowDefinitionRepository(jdbcTemplate)
}
