package com.ritense.authzenk

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.logging.Logger

class EvaluationContext(
    val httpClient: HttpClient,
    val host: String,
    val objectMapper: ObjectMapper
)

typealias Properties = Map<String, Any?>
typealias Context = Map<String, Any?>

data class Subject(
    val type: String,
    val id: String,
    val properties: Properties? = null
)

data class Action(
    val name: String,
    val properties: Properties? = null
)

data class Resource(
    val type: String,
    val id: String,
    val properties: Properties? = null
)

data class AccessEvaluationApiRequest(
    val subject: Subject,
    val action: Action,
    val resource: Resource,
    val context: Context? = null
)

data class AccessEvaluationsApiRequest(
    val subject: Subject? = null,
    val action: Action? = null,
    val resource: Resource? = null,
    val evaluations: List<PartialEvaluation> = listOf()
)

data class PartialEvaluation(
    val subject: Subject? = null,
    val action: Action? = null,
    val resource: Resource? = null,
)

typealias AccessEvaluationApiResponse = Decision

data class Decision(
    val decision: Boolean,
    val context: Context
)

data class AccessEvaluationsApiResponse(
    val evaluations: List<AccessEvaluationApiResponse> = listOf()
)

class Client(
    private val context: EvaluationContext
) {
    val evaluationApi = AccessEvaluationApi(context)
    val evaluationsApi = AccessEvaluationsApi(context)
    val searchApi = SearchApi(context)


}

@JsonIgnoreProperties(ignoreUnknown = true)
data class MetadataObject(
    @field:JsonProperty("policy_decision_point")
    val policyDecisionPoint: String,
    @field:JsonProperty("access_evaluation_endpoint")
    val accessEvaluationEndpoint: String,
    @field:JsonProperty("access_evaluations_endpoint")
    val accessEvaluationsEndpoint: String? = null,
    @field:JsonProperty("search_subject_endpoint")
    val searchSubjectEndpoint: String? = null,
    @field:JsonProperty("search_resource_endpoint")
    val searchResourceEndpoint: String? = null,
    @field:JsonProperty("search_action_endpoint")
    val searchActionEndpoint: String? = null,
    @field:JsonProperty("capabilities")
    val capabilities: List<String>? = null,
    @field:JsonProperty("signed_metadata")
    val signedMetadata: String? = null
)

class AccessEvaluationApi(val context: EvaluationContext) {

    fun evaluation(evaluation: AccessEvaluationApiRequest): AccessEvaluationApiResponse {
        Logger.getLogger("AccessEvaluationApi").info(context.objectMapper.writeValueAsString(evaluation))

        return this.context.httpClient.send(
            HttpRequest.newBuilder(URI.create("${context.host}/evaluation"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.objectMapper.writeValueAsString(evaluation)))
                .build()
        ) { ofJson<AccessEvaluationApiResponse>(context.objectMapper) }
            .body()
    }

}

class AccessEvaluationsApi(val context: EvaluationContext) {

    fun evaluations(evaluations: AccessEvaluationsApiRequest): AccessEvaluationsApiResponse {
        Logger.getLogger("AccessEvaluationsApi").info(context.objectMapper.writeValueAsString(evaluations))

        return this.context.httpClient.send(
            HttpRequest.newBuilder(URI.create("${context.host}/evaluations"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.objectMapper.writeValueAsString(evaluations)))
                .build()
        ) { ofJson<AccessEvaluationsApiResponse>(context.objectMapper) }
            .body()
    }

}

data class PageRequest(
    val token: String? = null,
    val limit: Int? = null,
    val properties: Properties? = null
)

data class PageResponse(
    @JsonProperty("next_token")
    val nextToken: String? = null,
    val count: Int? = null,
    val total: Int? = null,
    val properties: Properties? = null
)

data class PartialSubject(
    val type: String,
)

data class PartialResource(
    val type: String,
)

data class SubjectSearchApiRequest (
    val subject: PartialSubject,
    val action: Action,
    val resource: Resource,
    val context: Context? = null,
    val page: PageRequest? = null
)

data class ActionSearchApiRequest (
    val subject: Subject,
    val resource: Resource,
    val context: Context? = null,
    val page: PageRequest? = null
)

data class ResourceSearchApiRequest (
    val subject: Subject,
    val action: Action,
    val resource: PartialResource,
    val context: Context? = null,
    val page: PageRequest? = null
)

data class SearchResult(
    val results: List<Any>,
    val page: PageResponse? = null,
    val context: Context? = null,
)

class SearchApi(val context: EvaluationContext) {

    fun searchSubject(search: SubjectSearchApiRequest): SearchResult {
        Logger.getLogger("SearchApiRequest").info(context.objectMapper.writeValueAsString(search))

        return this.context.httpClient.send(
            HttpRequest.newBuilder(URI.create("${context.host}/search/subject"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.objectMapper.writeValueAsString(search)))
                .build()
        ) { ofJson<SearchResult>(context.objectMapper) }
            .body()
    }

    fun searchAction(search: ActionSearchApiRequest): SearchResult {
        Logger.getLogger("SearchApiRequest").info(context.objectMapper.writeValueAsString(search))

        return this.context.httpClient.send(
            HttpRequest.newBuilder(URI.create("${context.host}/search/action"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.objectMapper.writeValueAsString(search)))
                .build()
        ) { ofJson<SearchResult>(context.objectMapper) }
            .body()
    }

    fun searchResource(search: ResourceSearchApiRequest): SearchResult {
        Logger.getLogger("SearchApiRequest").info(context.objectMapper.writeValueAsString(search))

        return this.context.httpClient.send(
            HttpRequest.newBuilder(URI.create("${context.host}/search/resource"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(context.objectMapper.writeValueAsString(search)))
                .build()
        ) { ofJson<SearchResult>(context.objectMapper) }
            .body()
    }

}

inline fun <reified T> ofJson(objectMapper: ObjectMapper): HttpResponse.BodySubscriber<T> {
    return HttpResponse.BodySubscribers.mapping(HttpResponse.BodySubscribers.ofByteArray()) {
        Logger.getLogger("ofJson").log(java.util.logging.Level.INFO) {
            String(it)
        }
        objectMapper.readValue(it, T::class.java)
    }
}

