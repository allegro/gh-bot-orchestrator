package pl.allegro.tech.github.botorchestrator.domain

data class Repository(val id: Id) {

    constructor(owner: String, repo: String) : this(Id(owner, repo))

    val fullName: String = "${id.owner}/${id.name}"

    data class Id(val owner: String, val name: String)
}
