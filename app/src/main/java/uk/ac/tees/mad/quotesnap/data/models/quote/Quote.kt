package uk.ac.tees.mad.quotesnap.data.models.quote



data class Quote(
    val content: String = "",
    val author: String = ""
)

// API Response data class
data class QuotableResponse(
    val q: String,
    val a: String,
    val h: String
) {
    fun toQuote() = Quote(
        content = q,
        author = a
    )
}

