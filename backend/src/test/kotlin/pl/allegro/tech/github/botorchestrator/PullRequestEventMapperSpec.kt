package pl.allegro.tech.github.botorchestrator

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import org.springframework.util.ClassUtils
import pl.allegro.tech.github.botorchestrator.PullRequestEventFixture.pullRequestEvent
import pl.allegro.tech.github.botorchestrator.PullRequestFixture.pullRequest
import pl.allegro.tech.github.botorchestrator.config.ApplicationConfig
import pl.allegro.tech.github.botorchestrator.domain.PullRequest
import pl.allegro.tech.github.botorchestrator.domain.PullRequestEventMapper

class PullRequestEventMapperSpec : FunSpec() {
    init {

        context("should pick appropriate fields") {
            withData(
                TestData(pullRequest(), listOf("$"), WHOLE_EVENT),
                TestData(pullRequest(), listOf("$.pull_request"), ONLY_PR_PART),
                TestData(pullRequest(), listOf("$.pull_request.base.ref", "$.repository.owner.login"), SINGLE_FIELDS_IN_PR_AND_REPO),
                TestData(
                    pullRequest(),
                    listOf("$.pull_request.base.ref", "$.pull_request.base.sha", "$.pull_request.base.repo.full_name"),
                    SUBSET_OF_PULL_REQUEST_FIELD
                )
            ) { (pullRequest, paths, expected) ->
                val eventMapper = PullRequestEventMapper(objectMapper, paths)
                eventMapper.map(pullRequest) shouldEqualJson expected
            }
        }

        test("should ignore not existing path") {
            // given
            val pullRequest = pullRequest()
            val eventMapper = PullRequestEventMapper(objectMapper, listOf("$.pull_request.base.ref", "$.repository.owner.login", "$.non-existing-path"))

            // when
            val result = eventMapper.map(pullRequest)

            // then
            result shouldEqualJson SINGLE_FIELDS_IN_PR_AND_REPO
        }
    }

    companion object {

        private val objectMapper = ApplicationConfig().objectMapper()
        private val WHOLE_EVENT = objectMapper.writeValueAsString(pullRequestEvent().data)
        private val ONLY_PR_PART = getResourceAsText("only_pr_part.json")!!
        private val SINGLE_FIELDS_IN_PR_AND_REPO = getResourceAsText("small_subset.json")!!
        private val SUBSET_OF_PULL_REQUEST_FIELD = getResourceAsText("subset_of_pull_request_field.json")!!
    }
}

private data class TestData(val event: PullRequest, val paths: List<String>, val expected: String)

private fun getResourceAsText(path: String): String? = ClassUtils.getDefaultClassLoader()?.getResource(path)?.readText()
