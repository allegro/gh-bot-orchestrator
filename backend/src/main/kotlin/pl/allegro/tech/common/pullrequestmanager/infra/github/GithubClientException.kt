package pl.allegro.tech.common.pullrequestmanager.infra.github

class GithubClientException(context: String, cause: Throwable?) : RuntimeException(context, cause) {
    constructor(context: String) : this(context, null)
}

class GithubNonRetriableException(context: String, cause: Throwable?) : RuntimeException(context, cause) {
    constructor(context: String) : this(context, null)
}
