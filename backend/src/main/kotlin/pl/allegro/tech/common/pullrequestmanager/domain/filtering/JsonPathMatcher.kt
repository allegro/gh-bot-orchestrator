package pl.allegro.tech.common.pullrequestmanager.domain.filtering

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import pl.allegro.tech.common.pullrequestmanager.domain.PullRequest
import pl.allegro.tech.common.pullrequestmanager.infra.github.PullRequestFileDto

class JsonPathMatcher(val path: String, val pattern: Regex) : PullRequestMatcher {

    constructor(path: String, pattern: String) : this(path, pattern.toRegex(RegexOption.DOT_MATCHES_ALL))

    override fun match(pullRequest: PullRequest, pullRequestFiles: Sequence<PullRequestFileDto>): Boolean {
        try {
            return when (val jsonNode: Any? = JsonPath.read(pullRequest.originalEventString, path)) {
                is String -> handleString(jsonNode)
                is Boolean -> handleString(jsonNode.toString())
                is List<*> -> handleArray(jsonNode as List<String>)
                null -> handleString("")
                else -> {
                    logger.warn { "Expected String, Boolean, List<String> or null but got ${jsonNode?.javaClass} for path '$path'" }
                    false
                }
            }
        } catch (e: PathNotFoundException) {
            logger.warn { "Path $path which should match ${pattern.pattern} not found in event -> treating as not matched!" }
        }

        return false
    }

    private fun handleArray(array: List<String>): Boolean {
        return array.any(::handleString)
    }

    private fun handleString(extractedValue: String): Boolean {
        val result = pattern.matches(extractedValue)
        logger.debug { "$path has value $extractedValue. Result of matching with ${pattern.pattern}: $result" }
        return result
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
