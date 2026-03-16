package pl.allegro.tech.common.pullrequestmanager.config

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import pl.allegro.tech.common.pullrequestmanager.domain.GithubClient
import pl.allegro.tech.common.pullrequestmanager.infra.github.GithubAuthProperties
import pl.allegro.tech.common.pullrequestmanager.infra.github.GithubAuthenticationClient
import pl.allegro.tech.common.pullrequestmanager.infra.github.GithubServerProperties
import pl.allegro.tech.common.pullrequestmanager.infra.github.RestGithubClient
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.DefaultTokenProvider
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.JwtSigner
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.TokenProvider
import java.net.http.HttpClient
import java.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlin.time.toJavaDuration

@Configuration
class ApplicationConfig {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
        .disable(FAIL_ON_UNKNOWN_PROPERTIES)
        .registerKotlinModule()
        .registerModules(JavaTimeModule())

    @Bean
    fun restClient(builder: RestClient.Builder): RestClient {
        val client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
        val requestFactory = JdkClientHttpRequestFactory(client)
        requestFactory.setReadTimeout(10.seconds.toJavaDuration())
        return builder
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun jwtSigner(githubAuthProperties: GithubAuthProperties, clock: Clock): JwtSigner =
        JwtSigner(githubAuthProperties.privateKey, githubAuthProperties.appId, clock)

    @Bean
    fun githubAuthClient(
        restClient: RestClient,
        jwtSigner: JwtSigner,
        githubServerProperties: GithubServerProperties
    ): GithubAuthenticationClient = GithubAuthenticationClient(githubServerProperties.baseUrl, jwtSigner, restClient)

    @Bean
    fun tokenProvider(
        githubAuthClient: GithubAuthenticationClient,
        githubAuthProperties: GithubAuthProperties
    ): TokenProvider = DefaultTokenProvider(githubAuthClient, TimeSource.Monotonic, githubAuthProperties)

    @Bean
    fun restGithubClient(
        restClient: RestClient,
        tokenProvider: TokenProvider,
        githubServerProperties: GithubServerProperties,
        objectMapper: ObjectMapper
    ): GithubClient = RestGithubClient(githubServerProperties.baseUrl, tokenProvider, restClient, objectMapper)
}
