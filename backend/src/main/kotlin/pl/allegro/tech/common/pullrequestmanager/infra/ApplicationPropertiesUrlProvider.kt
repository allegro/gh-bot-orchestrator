package pl.allegro.tech.common.pullrequestmanager.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.allegro.tech.common.pullrequestmanager.domain.UrlProvider

@Component
class ApplicationPropertiesUrlProvider : UrlProvider {

    @Value("\${app.base-url}")
    private lateinit var baseUrl: String

    override fun baseUrl(): String {
        return baseUrl
    }
}
