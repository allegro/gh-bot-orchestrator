package pl.allegro.tech.github.botorchestrator

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

class WorkflowDefinitionsEndpointIntSpec(val rest: MockMvc) : BaseIntegrationSpec() {

    init {
        test("should create workflow definition with event matcher") {
            // when
            val result = rest.post("/api/workflows") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "enabled": true,
                        "repository": {
                            "name": "test-repo",
                            "owner": "test-owner"
                        },
                        "ref": "main",
                        "maxConcurrentRuns": 5,
                        "concurrencyGroup": "ONE_JOB_PER_REPOSITORY",
                        "filters": {
                            "matchingStrategy": "ALL",
                            "matchers": [
                                {
                                    "type": "event",
                                    "path": "$.pull_request.title",
                                    "regex": ".*test.*"
                                }
                            ]
                        },
                        "mapping": {
                            "fields": ["$.pull_request.number", "$.pull_request.head.sha"]
                        },
                        "check": {
                            "name": "Test Check",
                            "detailsPage": {
                                "title": "Test Title",
                                "summary": "Test Summary",
                                "details": "Test Details"
                            }
                        },
                        "workflowFilename": "test.yml"
                    }
                """
            }.andExpect {
                status { isCreated() }
                jsonPath("$.id") { exists() }
                jsonPath("$.enabled") { value(true) }
                jsonPath("$.repository.name") { value("test-repo") }
                jsonPath("$.repository.owner") { value("test-owner") }
                jsonPath("$.ref") { value("main") }
                jsonPath("$.maxConcurrentRuns") { value(5) }
                jsonPath("$.concurrencyGroup") { value("ONE_JOB_PER_REPOSITORY") }
                jsonPath("$.filters.matchingStrategy") { value("ALL") }
                jsonPath("$.filters.matchers[0].type") { value("event") }
                jsonPath("$.check.name") { value("Test Check") }
                jsonPath("$.workflowFilename") { value("test.yml") }
            }.andReturn()

            val responseContent = result.response.contentAsString
            val id = responseContent.substringAfter("\"id\":\"").substringBefore("\"")

            // then - verify it can be retrieved
            rest.get("/api/workflows").andExpect {
                status { isOk() }
                jsonPath("$[?(@.id=='$id')]") { exists() }
            }
        }

        test("should create workflow definition with file matcher") {
            // when
            rest.post("/api/workflows") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "enabled": true,
                        "repository": {
                            "name": "file-test-repo",
                            "owner": "file-test-owner"
                        },
                        "ref": "main",
                        "maxConcurrentRuns": 3,
                        "concurrencyGroup": "ONE_JOB_PER_PULL_REQUEST",
                        "filters": {
                            "matchingStrategy": "ANY",
                            "matchers": [
                                {
                                    "type": "file",
                                    "file": ".*\\.java",
                                    "statuses": ["added", "modified"]
                                }
                            ]
                        },
                        "mapping": {
                            "fields": ["$.pull_request.number"]
                        },
                        "check": {
                            "name": "File Check",
                            "detailsPage": {
                                "title": "File Title",
                                "summary": "File Summary"
                            }
                        },
                        "workflowFilename": "file.yml"
                    }
                """
            }.andExpect {
                status { isCreated() }
                jsonPath("$.filters.matchers[0].type") { value("file") }
            }
        }

        test("should create workflow definition with dependabot matcher") {
            // when
            rest.post("/api/workflows") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "enabled": true,
                        "repository": {
                            "name": "dependabot-test-repo",
                            "owner": "dependabot-test-owner"
                        },
                        "ref": "main",
                        "maxConcurrentRuns": 2,
                        "concurrencyGroup": "ONE_JOB_PER_REPOSITORY",
                        "filters": {
                            "matchingStrategy": "ALL",
                            "matchers": [
                                {
                                    "type": "dependabot",
                                    "artifactRegex": "spring-.*"
                                }
                            ]
                        },
                        "mapping": {
                            "fields": ["$.pull_request.number"]
                        },
                        "check": {
                            "name": "Dependabot Check",
                            "detailsPage": {
                                "title": "Dependabot Title",
                                "summary": "Dependabot Summary"
                            }
                        },
                        "workflowFilename": "dependabot.yml"
                    }
                """
            }.andExpect {
                status { isCreated() }
                jsonPath("$.filters.matchers[0].type") { value("dependabot") }
            }
        }

        test("should update workflow definition") {
            // given
            val createResult = rest.post("/api/workflows") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "enabled": true,
                        "repository": {
                            "name": "update-test-repo",
                            "owner": "update-test-owner"
                        },
                        "ref": "main",
                        "maxConcurrentRuns": 3,
                        "concurrencyGroup": "ONE_JOB_PER_PULL_REQUEST",
                        "filters": {
                            "matchingStrategy": "ANY",
                            "matchers": [
                                {
                                    "type": "event",
                                    "path": "$.action",
                                    "regex": "opened"
                                }
                            ]
                        },
                        "mapping": {
                            "fields": ["$.pull_request.head.sha"]
                        },
                        "check": {
                            "name": "Original Check",
                            "detailsPage": {
                                "title": "Original Title",
                                "summary": "Original Summary"
                            }
                        },
                        "workflowFilename": "original.yml"
                    }
                """
            }.andReturn()

            val responseContent = createResult.response.contentAsString
            val id = responseContent.substringAfter("\"id\":\"").substringBefore("\"")

            // when
            rest.put("/api/workflows/$id") {
                contentType = MediaType.APPLICATION_JSON
                content = """
                    {
                        "enabled": false,
                        "repository": {
                            "name": "updated-repo",
                            "owner": "updated-owner"
                        },
                        "ref": "develop",
                        "maxConcurrentRuns": 10,
                        "concurrencyGroup": "ONE_JOB_PER_REPOSITORY",
                        "filters": {
                            "matchingStrategy": "ALL",
                            "matchers": [
                                {
                                    "type": "event",
                                    "path": "$.action",
                                    "regex": "synchronize"
                                }
                            ]
                        },
                        "mapping": {
                            "fields": ["$.pull_request.number", "$.pull_request.title"]
                        },
                        "check": {
                            "name": "Updated Check",
                            "detailsPage": {
                                "title": "Updated Title",
                                "summary": "Updated Summary",
                                "details": "Updated Details"
                            }
                        },
                        "workflowFilename": "updated.yml"
                    }
                """
            }.andExpect {
                status { isOk() }
                jsonPath("$.id") { value(id) }
                jsonPath("$.enabled") { value(false) }
                jsonPath("$.repository.name") { value("updated-repo") }
                jsonPath("$.repository.owner") { value("updated-owner") }
                jsonPath("$.ref") { value("develop") }
                jsonPath("$.maxConcurrentRuns") { value(10) }
                jsonPath("$.concurrencyGroup") { value("ONE_JOB_PER_REPOSITORY") }
                jsonPath("$.filters.matchingStrategy") { value("ALL") }
                jsonPath("$.filters.matchers.length()") { value(1) }
                jsonPath("$.filters.matchers[0].regex") { value("synchronize") }
                jsonPath("$.mapping.fields.length()") { value(2) }
                jsonPath("$.mapping.fields[0]") { value("$.pull_request.number") }
                jsonPath("$.mapping.fields[1]") { value("$.pull_request.title") }
                jsonPath("$.check.name") { value("Updated Check") }
                jsonPath("$.check.detailsPage.details") { value("Updated Details") }
                jsonPath("$.workflowFilename") { value("updated.yml") }
            }
        }
    }
}
