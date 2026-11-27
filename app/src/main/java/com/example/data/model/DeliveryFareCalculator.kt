package com.example.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

data class DeliveryFare(
    val distanceKm: Double,
    val chargeRupees: Int
)

/**
 * Calculates delivery distance on the road network (not straight-line GPS distance).
 * Uses the OpenStreetMap OSRM public routing endpoint; checkout fails safely if no
 * route can be obtained, rather than silently charging from an inaccurate distance.
 */
object DeliveryFareCalculator {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    fun fromDistanceKm(distanceKm: Double): DeliveryFare {
        require(distanceKm.isFinite() && distanceKm > 0.0)
        return DeliveryFare(
            distanceKm = distanceKm,
            chargeRupees = ceil(distanceKm * 10.0).toInt()
        )
    }

    suspend fun calculate(
        shopLatitude: Double,
        shopLongitude: Double,
        customerLatitude: Double,
        customerLongitude: Double
    ): DeliveryFare = withContext(Dispatchers.IO) {
        require(shopLatitude.isFinite() && shopLatitude in -90.0..90.0)
        require(customerLatitude.isFinite() && customerLatitude in -90.0..90.0)
        require(shopLongitude.isFinite() && shopLongitude in -180.0..180.0)
        require(customerLongitude.isFinite() && customerLongitude in -180.0..180.0)

        val url = "https://router.project-osrm.org/route/v1/driving/" +
            "$shopLongitude,$shopLatitude;$customerLongitude,$customerLatitude?overview=false"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "AnnapurnaCustomerApp/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("रस्त्याचे अंतर मिळाले नाही. पुन्हा प्रयत्न करा.")
            val body = response.body?.string() ?: throw IOException("रस्त्याचे अंतर मिळाले नाही. पुन्हा प्रयत्न करा.")
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") throw IOException("या पत्त्यासाठी रस्ता सापडला नाही.")
            val route = json.optJSONArray("routes")?.optJSONObject(0)
                ?: throw IOException("या पत्त्यासाठी रस्ता सापडला नाही.")
            val meters = route.optDouble("distance", Double.NaN)
            if (!meters.isFinite() || meters <= 0.0) throw IOException("रस्त्याचे अंतर योग्य मिळाले नाही.")
            fromDistanceKm(meters / 1000.0)
        }
    }
}
