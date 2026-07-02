package com.aegis.network

import com.aegis.BuildConfig
import com.aegis.network.dto.ThreatIntelDto
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

interface ApiService {

    @POST("api/v1/threats/report")
    suspend fun reportThreat(@Body threat: ThreatIntelDto.ThreatReport): ThreatIntelDto.ReportResponse

    @GET("api/v1/threats/blocklist")
    suspend fun getBlocklist(@Query("since") since: Long): ThreatIntelDto.BlocklistResponse

    @GET("api/v1/threats/phishing")
    suspend fun checkPhishingUrl(@Query("url") url: String): ThreatIntelDto.UrlCheckResponse

    @GET("api/v1/threats/recent")
    suspend fun getRecentThreats(@Query("limit") limit: Int = 50): ThreatIntelDto.RecentThreatsResponse

    @POST("api/v1/analytics/event")
    suspend fun sendAnalytics(@Body event: ThreatIntelDto.AnalyticsEvent): ThreatIntelDto.AnalyticsResponse

    @GET("api/v1/models/check-update")
    suspend fun checkModelUpdate(@Query("current_version") currentVersion: String): ThreatIntelDto.ModelUpdateResponse

    companion object {
        private const val DEFAULT_TIMEOUT = 30L

        fun create(baseUrl: String, certificatePinner: CertificatePinner? = null): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BODY
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            }

            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("User-Agent", "AEGIS/${BuildConfig.VERSION_NAME}")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }

            certificatePinner?.let { clientBuilder.certificatePinner(it) }

            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(clientBuilder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
