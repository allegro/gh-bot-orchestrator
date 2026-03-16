package pl.allegro.tech.github.botorchestrator

import com.github.tomakehurst.wiremock.client.WireMock.badRequestEntity
import com.github.tomakehurst.wiremock.client.WireMock.ok
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.urlMatching
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

class CheckCallbacksEndpointIntSpec(val rest: MockMvc) : BaseIntegrationSpec() {
    init {
        beforeAny {
            githubApiMock.stubTokenResponse()
        }

        test("should return success status in happy path") {
            // given
            githubApiMock.stubFor(patch(urlMatching("/repos/.*/.*/check-runs/.*")).willReturn(ok()))

            // expect
            rest.post("/check-callbacks/some-org/some-repo/123456") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "status": "failure",
                        "detailsPage": {
                            "title": "some title",
                            "summary": "some summary",
                            "details": "some details",
                            "customUrl": "some url"
                        }
                    }
                """
            }.andExpect {
                status { is2xxSuccessful() }
            }
        }

        test("should fail with 4xx (not retriable) status code when client error") {
            // given
            githubApiMock.stubFor(patch(urlMatching("/repos/.*/.*/check-runs/.*")).willReturn(badRequestEntity()))

            // expect
            rest.post("/check-callbacks/some-org/some-repo/123456") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "status": "failure",
                        "detailsPage": {
                            "title": "some title",
                            "summary": "some summary",
                            "details": "some details",
                            "customUrl": "some url"
                        }
                    }
                """
            }.andExpect {
                status { is4xxClientError() }
            }

        }
    }
}
