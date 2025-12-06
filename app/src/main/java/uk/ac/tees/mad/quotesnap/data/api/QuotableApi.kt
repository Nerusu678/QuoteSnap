package uk.ac.tees.mad.quotesnap.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import uk.ac.tees.mad.quotesnap.data.models.quote.QuotableResponse

//https://api.quotable.io/random?tags=motivational&maxLength=150


// end point to get the random quote with the tag
interface QuotableApi {

    @GET("random")
    suspend fun getRandomQuote(
        @Query("tags") tags: String = "motivational",
        @Query("maxLength") maxLength: Int = 150
    ): QuotableResponse

    companion object {
        const val BASE_URL = "https://api.quotable.io/"
    }
}