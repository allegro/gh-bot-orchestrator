package pl.allegro.tech.github.botorchestrator.domain.workflows

import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.workflows.api.WorkflowDefinitionToAdd
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

    fun deleteDefinition(id: UUID) {
        workflowDefinitionRepository.delete(id)
    }
}
