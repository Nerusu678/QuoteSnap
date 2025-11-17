package uk.ac.tees.mad.quotesnap.data.models.quote

data class Quote(
    val content: String = "",
    val author: String = ""
)

// API Response
data class QuotableResponse(
    val _id: String,
    val content: String,
    val author: String,
    val tags: List<String>
) {
    fun toQuote() = Quote(
        content = content,
        author = author
    )
}