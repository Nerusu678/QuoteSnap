package uk.ac.tees.mad.quotesnap.data.models.quote

data class Quote(
    val id: String = "",
    val content: String = "",
    val author: String = "",
    val tags: List<String> = emptyList()
)

// API Response model
data class QuotableResponse(
    val _id: String,
    val content: String,
    val author: String,
    val tags: List<String>,
    val length: Int
) {
    fun toQuote() = Quote(
        id = _id,
        content = content,
        author = author,
        tags = tags
    )
}