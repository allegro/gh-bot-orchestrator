package pl.allegro.tech.github.botorchestrator.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import pl.allegro.tech.github.botorchestrator.domain.UrlProvider

@Component
class ApplicationPropertiesUrlProvider : UrlProvider {

    @Value("\${app.base-url}")
    private lateinit var baseUrl: String

    override fun baseUrl(): String {
        return baseUrl
    }
}
