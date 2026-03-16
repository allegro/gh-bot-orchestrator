package pl.allegro.tech.github.botorchestrator.domain

data class Workflow(
    val repository: Repository.Id,
    val ref: String,
    val workflowFileName: String
)
