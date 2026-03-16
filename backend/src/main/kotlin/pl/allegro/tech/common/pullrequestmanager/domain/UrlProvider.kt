package pl.allegro.tech.common.pullrequestmanager.domain

interface UrlProvider {

    abstract fun baseUrl(): String
}
