package pl.allegro.tech.common.pullrequestmanager.domain.workflows

import pl.allegro.tech.common.pullrequestmanager.domain.workflows.api.WorkflowDefinitionToAdd
import java.util.UUID

interface WorkflowDefinitionRepository {

    fun findAll(): List<PersistedWorkflowDefinition>
    fun findById(id: UUID): PersistedWorkflowDefinition?
    fun findEnabled(): Set<UUID>
    fun exists(repositoryName: String, repositoryOwner: String, ref: String, path: String): Boolean
    fun insert(workflowDefinitionToAdd: WorkflowDefinitionToAdd): UUID
    fun update(id: UUID, workflowDefinitionToAdd: WorkflowDefinitionToAdd)
    fun delete(id: UUID)
}
