package com.nrova.sdk.newsroom

import com.nrova.sdk.core.JsonValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class NewsroomStatus(public val wire: String) {
    @SerialName("draft") DRAFT("draft"),
    @SerialName("scheduled") SCHEDULED("scheduled"),
    @SerialName("published") PUBLISHED("published");
}

@Serializable
public data class NewsroomCategory(
    public val id: String,
    public val slug: String? = null,
    public val label: String? = null,
    public val color: String? = null,
    public val sortOrder: Int? = null,
    public val createdAt: String? = null,
)

@Serializable
public data class NewsroomCategoriesResponse(public val categories: List<NewsroomCategory>)

@Serializable
public data class NewsroomPost(
    public val id: String,
    public val slug: String,
    public val title: String,
    public val categoryId: String,
    public val tags: List<String>? = null,
    public val excerpt: String? = null,
    public val body: String? = null,
    public val status: NewsroomStatus,
    public val heroImageUrl: String? = null,
    public val createdBy: String? = null,
    public val publishedAt: String? = null,
    public val scheduledFor: String? = null,
    public val createdAt: String? = null,
    public val updatedAt: String? = null,
    public val document: JsonValue? = null,
)

@Serializable
public data class NewsroomPostsResponse(public val posts: List<NewsroomPost>)

@Serializable
public data class NewsroomPostResponse(public val post: NewsroomPost)

// Request bodies

@Serializable
public data class CreateNewsroomPostRequest(
    public val title: String,
    public val categoryId: String,
    public val slug: String? = null,
    public val body: String? = null,
    public val excerpt: String? = null,
    public val tags: List<String>? = null,
    public val heroImageUrl: String? = null,
    public val status: NewsroomStatus? = null,
    public val scheduledFor: String? = null,
)

@Serializable
public data class UpdateNewsroomPostRequest(
    public val title: String? = null,
    public val categoryId: String? = null,
    public val slug: String? = null,
    public val body: String? = null,
    public val excerpt: String? = null,
    public val tags: List<String>? = null,
    public val heroImageUrl: String? = null,
    public val status: NewsroomStatus? = null,
    public val scheduledFor: String? = null,
)
