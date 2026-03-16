package pl.allegro.tech.common.pullrequestmanager

import com.github.zafarkhaja.semver.Version
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import pl.allegro.tech.common.pullrequestmanager.api.PullRequestEvent.PullRequestEventPayload
import pl.allegro.tech.common.pullrequestmanager.domain.dependabot.VersionUpdate

class VersionUpdateSpec : FunSpec() {
    init {
        context("should parse versions from correct pull request body") {
            withData(
                "Bumps pl.allegro.tech.common:andamio-starter-dependencies from 6.2.0 to 7.1.1",
                "Bumps pl.allegro.tech.common:andamio-starter-dependencies from v6.2.0 to v7.1.1",
                "Bumps pl.allegro.tech.common:andamio-errors-core from 6.2.0 to 7.1.1",
                "Bumps pl.allegro.tech.common:andamio-starter-webmvc from 6.2.0 to 7.1.1",
                "Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "Dependencies update: Bumps pl.allegro.tech.common:andamio-starter-webmvc from 6.2.0 to 7.1.1",
                "JIRA-1234 | Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "JIRA-1234 Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "JIRA-1234(deps): Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "#42 | Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "#42 (deps): Bumps pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "Bumps andamio from 6.2.0 to 7.1.1"
            ) { body ->
                val pullRequest = pullRequest(bodyTemplate(body))
                val versionUpdates = VersionUpdate.from(pullRequest)

                versionUpdates shouldHaveSize 1
                val versionUpdate = versionUpdates[0]

                versionUpdate.from!! shouldBeEqual Version.of(6, 2)
                versionUpdate.to!! shouldBeEqual Version.of(7, 1, 1)
            }
        }

        context("should parse versions from correct batch pull request body") {
            withData(
                "Updates pl.allegro.tech.common:andamio-starter-dependencies from 6.2.0 to 7.1.1",
                "Bumps pl.allegro.tech.common:andamio-starter-dependencies from v6.2.0 to v7.1.1.",
                "Updates pl.allegro.tech.common:andamio-errors-core from 6.2.0 to 7.1.1",
                "Dependencies update: Updates pl.allegro.tech.common:andamio-starter-webmvc from 6.2.0 to 7.1.1",
                "JIRA-1234 | Updates pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "JIRA-1234 Updates pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "JIRA-1234(deps): Updates pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "#42 | Updates pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "#42 (deps): Updates pl.allegro.tech.common:andamio-request-rxjava2 from 6.2.0 to 7.1.1",
                "Updates andamio from 6.2.0 to 7.1.1"
            ) { body ->
                val pullRequest = pullRequest(batchUpdateBodyTemplate(body))
                val versionUpdates = VersionUpdate.from(pullRequest)

                versionUpdates.shouldNotBeEmpty()
                versionUpdates.firstOrNull { it.from == Version.of(6, 2) }.shouldNotBeNull()
                versionUpdates.firstOrNull { it.to == Version.of(7, 1, 1) }.shouldNotBeNull()
            }
        }

        context("should fail for unexpected pull request body") {
            withData(
                "Bumps pl.allegro.tech.common:andamio-errors-core.",
                "Bump pl.allegro.tech.common:andamio-starter-webmvc from xxx to yyy.",
                "pl.allegro.tech.common:andamio-starter-webmvc from 6.2.0 to 7.1.1"
            ) { body ->
                val pullRequest = pullRequest(bodyTemplate(body))
                val versionUpdate = VersionUpdate.from(pullRequest)

                versionUpdate.shouldBeEmpty()
            }
        }

        test("should support non-strict semver versioning") {
            val pullRequest = pullRequest(bodyTemplate("Bumps actions/checkout from 2 to 4."))
            val versionUpdates = VersionUpdate.from(pullRequest)

            versionUpdates shouldHaveSize 1
            val versionUpdate = versionUpdates[0]
            versionUpdate.artifact shouldBeEqual "actions/checkout"
            versionUpdate.from!! shouldBeEqual Version.of(2)
            versionUpdate.to!! shouldBeEqual Version.of(4)
        }

        context("should parse artifact from correct pull request body") {
            withData(
                "pl.allegro.tech.common:andamio-starter-dependencies",
                "@allegro/metrum-assets",
                "dependency"
            ) { artifact ->
                val pullRequest = pullRequest(bodyTemplate("Bumps $artifact from 1.0.0 to 2.0.0."))
                val versionUpdates = VersionUpdate.from(pullRequest)

                versionUpdates shouldHaveSize 1
                val versionUpdate = versionUpdates[0]
                versionUpdate.artifact shouldBeEqual artifact
            }
        }

        context("should parse artifact from correct batch pull request body") {
            withData(
                "pl.allegro.tech.common:andamio-starter-dependencies",
                "@allegro/metrum-assets",
                "dependency"
            ) { artifact ->
                val pullRequest = pullRequest(batchUpdateBodyTemplate("Updates $artifact from 1.0.0 to 2.0.0"))
                val versionUpdates = VersionUpdate.from(pullRequest)

                versionUpdates.firstOrNull { it.artifact == artifact }.shouldNotBeNull()
            }
        }

        test("should parse artifact and version from batch pull request body with links") {
            val artifact = "ch.qos.logback:logback-classic"
            val from = Version.of(1)
            val to = Version.of(1, 0, 1)
            val message = "Bumps [$artifact](https://user:pass@mysite.com/repo#main?param1=42&param2=2137) from $from to $to."
            val pullRequest = pullRequest(batchUpdateBodyTemplate(message))

            // when
            val versionUpdates = VersionUpdate.from(pullRequest)

            // then
            versionUpdates.shouldNotBeEmpty()
            versionUpdates.shouldContain(VersionUpdate(artifact, from, to))
        }

        test("should parse artifact and version from batch pull request body with ticks") {
            val artifact = "ch.qos.logback:logback-classic"
            val from = Version.of(1)
            val to = Version.of(1, 0, 1)
            val message = "Updates `$artifact` from $from to $to"
            val pullRequest = pullRequest(batchUpdateBodyTemplate(message))

            // when
            val versionUpdates = VersionUpdate.from(pullRequest)

            // then
            versionUpdates.shouldNotBeEmpty()
            versionUpdates.shouldContain(VersionUpdate(artifact, from, to))
        }
    }

    private fun pullRequest(pullRequestBody: String): PullRequestEventPayload = mockk<PullRequestEventPayload> {
        every { pullRequest.body } returns pullRequestBody
    }

    private fun bodyTemplate(content: String) = """
        $content

        Dependabot will resolve any conflicts with this PR as long as you don't alter it yourself. You can also trigger a rebase manually by commenting `@dependabot rebase`.

        Dependabot commands and options
    """.trimIndent()

    private fun batchUpdateBodyTemplate(content: String) = """
        Bumps `dependencies` from 3.6.4 to 7.3.0.

        Updates `pl.allegro.tech:another-dependency` from 1.0.0 to 1.0.1

        Commits
        * See full diff in [compare view](https://github.com/allegro-internal/andamio-starter/commits)

        $content

        Commits
        * See full diff in [compare view](https://github.com/allegro-internal/another-dependency)

        Updates `actions/checkout` from 4 to 42

        Dependabot will resolve any conflicts with this PR as long as you don't alter it yourself. You can also trigger a rebase manually by commenting `@dependabot rebase`.

        Dependabot commands and options
    """.trimIndent()
}
