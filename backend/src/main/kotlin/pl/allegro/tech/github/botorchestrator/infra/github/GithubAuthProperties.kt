package pl.allegro.tech.github.botorchestrator.infra.github

import org.springframework.boot.context.properties.ConfigurationProperties
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

typealias JavaDuration = java.time.Duration
typealias KotlinDuration = kotlin.time.Duration

@ConfigurationProperties("app.github.auth")
class GithubAuthProperties(
    val installationId: String,
    val privateKey: String,
    tokenTimeToLive: JavaDuration,
    val appId: String
) {

    val tokenTimeToLive: KotlinDuration = tokenTimeToLive.toKotlinDuration()

    constructor(installationId: String, privateKey: String, tokenTimeToLive: KotlinDuration, appId: String) :
        this(installationId, privateKey, tokenTimeToLive.toJavaDuration(), appId)
}
