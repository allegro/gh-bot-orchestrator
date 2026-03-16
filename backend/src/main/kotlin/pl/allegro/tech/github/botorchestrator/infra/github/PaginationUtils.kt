package pl.allegro.tech.github.botorchestrator.infra.github

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity

fun <T> paginate(baseUri: String, perPage: Int = 100, performRequest: (String) -> Result<ResponseEntity<List<T>>?>): Sequence<T> =
    sequence {
        var nextUri: String? = "${baseUri}?per_page=${perPage}"

        while (nextUri != null) {
            val response = performRequest(nextUri).getOrNull()

            if (response != null) {
                (response.body ?: emptyList()).forEach { yield(it) }
                nextUri = parseNextPageUrl(response.headers)
            }
        }
    }

private fun parseNextPageUrl(headers: HttpHeaders): String? {
    val linkHeader = headers.getFirst(HttpHeaders.LINK) ?: return null

    val links = linkHeader.split(',')
        .map { it.trim() }
        .associate {
            val parts = it.split(';')
            val url = parts[0].removePrefix("<").removeSuffix(">")
            val rel = parts[1].substringAfter("rel=\"").substringBefore("\"")
            rel to url
        }

    return links["next"]
}
