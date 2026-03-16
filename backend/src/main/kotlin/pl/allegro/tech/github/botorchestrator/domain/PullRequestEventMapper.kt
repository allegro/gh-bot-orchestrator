package pl.allegro.tech.github.botorchestrator.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import io.github.oshai.kotlinlogging.KotlinLogging

class PullRequestEventMapper(private val objectMapper: ObjectMapper, val paths: List<String>) {

    fun map(pullRequest: PullRequest): String {
        val json = pullRequest.originalEventString
        if (paths.any { it == "$" }) {
            return json
        }

        val rootNode: ObjectNode = objectMapper.readTree("{}") as ObjectNode
        paths.forEach { path -> addDataToNode(json, path, rootNode) }
        return objectMapper.writeValueAsString(rootNode)
    }

    private fun addDataToNode(json: String?, path: String, rootNode: ObjectNode) {
        try {
            val value = JsonPath.read<Any>(json, path)
            val segments = path.removePrefix("$").split(".")

            var currentNode = rootNode

            segments.forEachIndexed { index, segment ->
                if (index == segments.lastIndex) {
                    currentNode.putPOJO(segment, value)
                } else {
                    currentNode = currentNode.withObject(segment)
                }
            }
        } catch (e: JsonPathException) {
            logger.warn { "Non existing path found during mapping: $path" }
        }
    }

    companion object {

        private val logger = KotlinLogging.logger { }
    }
}
