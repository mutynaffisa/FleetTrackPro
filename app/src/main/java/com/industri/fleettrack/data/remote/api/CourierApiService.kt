package com.industri.fleettrack.data.remote.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class DeliveryDto(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("tracking_number") val trackingNumber: String,
    @SerializedName("recipient_name") val recipientName: String,
    @SerializedName("recipient_phone") val recipientPhone: String,
    @SerializedName("destination_address") val destinationAddress: String,
    @SerializedName("cod_amount") val codAmount: Double,
    @SerializedName("status") val status: String
)

interface CourierApiService {
    @GET("api/v1/courier/manifest/today")
    suspend fun getTodayManifest(): Response<List<DeliveryDto>>

    @POST("api/v1/courier/deliveries/{order_id}/status")
    suspend fun updateDeliveryStatus(
        @Path("order_id") orderId: String,
        @Query("status") status: String
    ): Response<Map<String, Any>>

    companion object {
        private const val BASE_URL = "https://mock-api.fleettrack.id/"
        fun create(): CourierApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CourierApiService::class.java)
        }
    }
}