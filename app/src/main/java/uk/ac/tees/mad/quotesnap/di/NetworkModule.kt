package uk.ac.tees.mad.quotesnap.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uk.ac.tees.mad.quotesnap.data.OcrApiService
import uk.ac.tees.mad.quotesnap.data.api.QuotableApi
import uk.ac.tees.mad.quotesnap.data.local.AppDatabase
import uk.ac.tees.mad.quotesnap.data.local.PosterDao
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Single OkHttpClient with BOTH logging AND SSL bypass
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return try {
            // Logging interceptor
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // SSL bypass for expired certificates
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)  // Add logging
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)  // Add SSL bypass
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    // Retrofit for OCR API
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OcrApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOcrApiService(retrofit: Retrofit): OcrApiService {
        return retrofit.create(OcrApiService::class.java)
    }

    // Retrofit for Quotable API (uses same OkHttpClient)
    @Provides
    @Singleton
    fun provideQuotableApi(okHttpClient: OkHttpClient): QuotableApi {
        return Retrofit.Builder()
            .baseUrl(QuotableApi.BASE_URL)
            .client(okHttpClient)  // Same client with SSL bypass
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuotableApi::class.java)
    }

    // Room Database
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "quotesnap_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePosterDao(database: AppDatabase): PosterDao {
        return database.posterDao()
    }

    // Firebase Firestore
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}