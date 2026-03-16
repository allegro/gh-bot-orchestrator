package pl.allegro.tech.common.pullrequestmanager.domain.workflows

import org.springframework.stereotype.Component
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinitionToAdd
import java.util.*

@Component
data class WorkflowDefinitionsEditor(
    private val workflowDefinitionRepository: WorkflowDefinitionRepository
) {
    fun saveDefinition(workflowDefinitionToAdd: WorkflowDefinitionToAdd): UUID {
        return workflowDefinitionRepository.insert(workflowDefinitionToAdd)
    }

    fun updateDefinition(id: UUID, workflowDefinitionToAdd: WorkflowDefinitionToAdd) {
        workflowDefinitionRepository.update(id, workflowDefinitionToAdd)
    }
}
