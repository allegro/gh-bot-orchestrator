package pl.allegro.tech.common.pullrequestmanager.domain

data class Workflow(
    val repository: Repository.Id,
    val ref: String,
    val workflowFileName: String
)
