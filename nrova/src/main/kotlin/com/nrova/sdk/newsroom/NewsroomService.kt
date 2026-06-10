package com.nrova.sdk.newsroom

import com.nrova.sdk.core.HealthResponse
import com.nrova.sdk.core.HttpClient

/**
 * Client for the **Nrova Newsroom API**
 * (`https://us-central1-nrovallc.cloudfunctions.net/newsroomApi`).
 */
public class NewsroomService internal constructor(
    private val http: HttpClient,
    private val orgId: String,
) {
    private fun base(): String = "v1/organizations/$orgId/newsroom"

    public suspend fun health(): HealthResponse =
        http.get("health", HealthResponse.serializer(), authenticated = false)

    /** `GET …/newsroom/categories`. */
    public suspend fun listCategories(): List<NewsroomCategory> =
        http.get(
            path = "${base()}/categories",
            deserializer = NewsroomCategoriesResponse.serializer(),
        ).categories

    /** `GET …/newsroom/posts` with optional filters. */
    public suspend fun listPosts(
        status: NewsroomStatus? = null,
        categoryId: String? = null,
        tag: String? = null,
        limit: Int? = null,
        includeDocument: Boolean = false,
    ): List<NewsroomPost> =
        http.get(
            path = "${base()}/posts",
            deserializer = NewsroomPostsResponse.serializer(),
            query = mapOf(
                "status" to status?.wire,
                "categoryId" to categoryId,
                "tag" to tag,
                "limit" to limit?.toString(),
                "include" to if (includeDocument) "document" else null,
            ),
        ).posts

    /** `GET …/newsroom/posts/{idOrSlug}`. */
    public suspend fun getPost(
        idOrSlug: String,
        includeDocument: Boolean = false,
    ): NewsroomPost =
        http.get(
            path = "${base()}/posts/$idOrSlug",
            deserializer = NewsroomPostResponse.serializer(),
            query = mapOf("include" to if (includeDocument) "document" else null),
        ).post

    /** `POST …/newsroom/posts` — requires `newsroom:write`. */
    public suspend fun createPost(request: CreateNewsroomPostRequest): NewsroomPost =
        http.post(
            path = "${base()}/posts",
            bodySerializer = CreateNewsroomPostRequest.serializer(),
            body = request,
            deserializer = NewsroomPostResponse.serializer(),
        ).post

    /** `PATCH …/newsroom/posts/{postId}` — requires `newsroom:write`. */
    public suspend fun updatePost(
        postId: String,
        update: UpdateNewsroomPostRequest,
    ): NewsroomPost =
        http.patch(
            path = "${base()}/posts/$postId",
            bodySerializer = UpdateNewsroomPostRequest.serializer(),
            body = update,
            deserializer = NewsroomPostResponse.serializer(),
        ).post

    /** `DELETE …/newsroom/posts/{postId}` — requires `newsroom:write`. */
    public suspend fun deletePost(postId: String) {
        http.delete("${base()}/posts/$postId")
    }
}
