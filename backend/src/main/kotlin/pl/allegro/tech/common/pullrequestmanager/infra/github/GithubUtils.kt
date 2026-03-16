@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package pl.allegro.tech.common.pullrequestmanager.infra.github

import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestClient.RequestHeadersSpec
import org.springframework.web.client.toEntity

fun <T> RequestHeadersSpec<*>.toResult(targetClass: ParameterizedTypeReference<T>): Result<T?> =
    exchange { _, response ->
        if (response.statusCode.is2xxSuccessful) {
            val body = response.bodyTo(targetClass)
            Result.success(body)
        } else if (isNonRetriable(response.statusCode)) {
            val error = response.bodyTo(String::class.java)
            Result.failure(GithubNonRetriableException("${response.statusCode} $error"))
        } else {
            val error = response.bodyTo(String::class.java)
            Result.failure(GithubClientException("${response.statusCode} $error"))
        }
    }

inline fun <reified T : Any> RequestHeadersSpec<*>.toResultEntity(): Result<ResponseEntity<T>?> {
    val response = retrieve().toEntity<T>()

    return if (response.statusCode.is2xxSuccessful) {
        Result.success(response)
    } else if (isNonRetriable(response.statusCode)) {
        val error = response.body
        Result.failure(GithubNonRetriableException("${response.statusCode} $error"))
    } else {
        val error = response.body
        Result.failure(GithubClientException("${response.statusCode} $error"))
    }
}

fun isNonRetriable(statusCode: HttpStatusCode): Boolean =
    statusCode.value() == 422

inline fun <reified T> RequestHeadersSpec<*>.toResult(): Result<T?> =
    toResult(object : ParameterizedTypeReference<T>() {})

fun <T> Result<T>.mapError(transform: (throwable: Throwable) -> Throwable): Result<T> = exceptionOrNull()?.let {
    Result.failure(transform(it))
} ?: this
