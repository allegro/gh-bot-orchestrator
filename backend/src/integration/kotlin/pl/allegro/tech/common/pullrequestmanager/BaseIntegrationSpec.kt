package pl.allegro.tech.common.pullrequestmanager

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.Options.DYNAMIC_PORT
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.spring.SpringExtension
import org.flywaydb.core.Flyway
import org.mockito.BDDMockito.given
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import pl.allegro.tech.common.pullrequestmanager.BaseIntegrationSpec.TestConfig
import pl.allegro.tech.common.pullrequestmanager.domain.Repository
import pl.allegro.tech.common.pullrequestmanager.domain.SlotContext
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsEditor
import pl.allegro.tech.common.pullrequestmanager.domain.workflows.WorkflowDefinitionsReader
import pl.allegro.tech.common.pullrequestmanager.infra.github.auth.JwtSigner

@SpringBootTest(
    classes = [TestConfig::class, AppRunner::class],
    properties = ["spring.flyway.clean-disabled=false"]
)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class BaseIntegrationSpec : FunSpec() {

    @Autowired
    private lateinit var flyway: Flyway

    @Autowired
    lateinit var workflowDefinitionsReader: WorkflowDefinitionsReader

    @Autowired
    lateinit var workflowDefinitionFixtures: WorkflowDefinitionFixtures

    init {
        extensions(SpringExtension)

        beforeAny {
            cleanUpDb()
            wireMock.resetAll()
        }
    }

    private fun cleanUpDb() {
        flyway.clean()
        flyway.migrate()
    }

    @Configuration
    class TestConfig {

        @Bean
        fun workflowDefinitionFixture(
            workflowDefinitionsReader: WorkflowDefinitionsReader,
            workflowDefinitionsEditor: WorkflowDefinitionsEditor
        ): WorkflowDefinitionFixtures =
            WorkflowDefinitionFixtures(workflowDefinitionsReader, workflowDefinitionsEditor)

        @Bean
        @Primary
        fun testJwtSigner(): JwtSigner {
            val jwtSigner = Mockito.mock(JwtSigner::class.java)
            given(jwtSigner.createNewSignedToken()).willReturn(SIGNED_TEMP_TOKEN)
            return jwtSigner
        }
    }

    companion object {

        const val SIGNED_TEMP_TOKEN = "temporary-token-from-test"
        const val ACCESS_TOKEN = "access-token-from-test"

        @JvmStatic
        private val wireMock =
            WireMockServer(DYNAMIC_PORT).also {
                it.start()
            }

        val githubApiMock = GithubApiMock(wireMock, ACCESS_TOKEN)

        @JvmStatic
        @DynamicPropertySource
        fun configureWiremockPorts(registry: DynamicPropertyRegistry) {
            registry.add("app.github.server.base-url") { "http://localhost:${wireMock.port()}" }
            registry.add("app.base-url") { "http://localhost:${wireMock.port()}" }
        }

    }
}
