package pl.allegro.tech.github.botorchestrator

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import pl.allegro.tech.github.botorchestrator.PullRequestFiltersFixture.jsonPath
import pl.allegro.tech.github.botorchestrator.PullRequestFiltersFixture.matchAll
import pl.allegro.tech.github.botorchestrator.PullRequestFiltersFixture.matchAny
import pl.allegro.tech.github.botorchestrator.PullRequestFixture.pullRequest
import pl.allegro.tech.github.botorchestrator.domain.filtering.MatchEverything

class PullRequestFiltersSpec : FunSpec() {
    init {
        val pullRequest = pullRequest()

        context("examples of filters matching event") {

            withData(
                MatchEverything,
                matchAny { jsonPath("$.repository.name", "some-repository") },
                matchAny { jsonPath("$.repository.name", ".*repository") },
                matchAny { jsonPath("$.pull_request.merged", "false") },
                matchAny {
                    jsonPath("$.repository.name", "repository")
                    jsonPath("$.repository.name", "some-repository")
                },
                matchAll {
                    jsonPath("$.action", "opened|synchronize")
                    jsonPath("$.pull_request.user.login", "some-user")
                }
            ) { filter ->
                val result = filter.match(pullRequest)
                result shouldBe true
            }
        }

        context("examples of filters not matching event") {

            withData(
                matchAny { jsonPath("$.repository.name", "repository") },
                matchAll {
                    jsonPath("$.repository.name", "repository")
                    jsonPath("$.repository.name", "some-repository")
                },
                matchAny { jsonPath("$.pull_request.user.login", "^(?!some-user).*\$") },
            ) { filter ->
                val result = filter.match(pullRequest)
                result shouldBe false
            }
        }

        test("should treat event as not matched when given path does not exist") {
            // given
            val filter = matchAny { jsonPath("$.repository.non-exising-path", "some-value") }

            // when
            val result = filter.match(pullRequest)

            // then
            result shouldBe false
        }

        test("should parse topics array") {
            // given
            val filter = matchAny { jsonPath("$.repository.topics", "source") }

            // when
            val result = filter.match(pullRequest)

            // then
            result shouldBe true
        }
    }
}
