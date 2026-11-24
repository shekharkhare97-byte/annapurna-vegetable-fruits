package com.example.ui.components

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.util.concurrent.TimeUnit

private data class DeliveryRoute(
    val points: List<GeoPoint>,
    val distanceKm: Double,
    val durationMinutes: Int
)

private val routeHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()
}

private suspend fun loadDeliveryRoute(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double
): DeliveryRoute? = withContext(Dispatchers.IO) {
    val url = "https://router.project-osrm.org/route/v1/driving/" +
        startLongitude + "," + startLatitude + ";" + endLongitude + "," + endLatitude +
        "?overview=full&geometries=geojson"
    val request = Request.Builder().url(url).header("User-Agent", "AnnapurnaCustomerApp/1.0").build()
    runCatching {
        routeHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            val body = response.body?.string() ?: return@use null
            val route = JSONObject(body).optJSONArray("routes")?.optJSONObject(0) ?: return@use null
            val coordinates = route.optJSONObject("geometry")?.optJSONArray("coordinates") ?: return@use null
            val points = buildList {
                for (index in 0 until coordinates.length()) {
                    val coordinate = coordinates.optJSONArray(index) ?: continue
                    val lon = coordinate.optDouble(0, Double.NaN)
                    val lat = coordinate.optDouble(1, Double.NaN)
                    if (lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) {
                        add(GeoPoint(lat, lon))
                    }
                }
            }
            if (points.size < 2) return@use null
            DeliveryRoute(
                points = points,
                distanceKm = route.optDouble("distance", 0.0) / 1000.0,
                durationMinutes = (route.optDouble("duration", 0.0) / 60.0).toInt().coerceAtLeast(1)
            )
        }
    }.getOrNull()
}

@Composable
fun OpenStreetMapView(
    latitude: Double,
    longitude: Double,
    markerTitle: String,
    markerSnippet: String,
    modifier: Modifier = Modifier,
    destinationLatitude: Double? = null,
    destinationLongitude: Double? = null
) {
    val context = LocalContext.current
    val mapHolder = remember { arrayOfNulls<MapView>(1) }
    var deliveryRoute by remember { mutableStateOf<DeliveryRoute?>(null) }
    var lastRouteFetchAt by remember { mutableStateOf(0L) }

    LaunchedEffect(latitude, longitude, destinationLatitude, destinationLongitude) {
        if (destinationLatitude != null && destinationLongitude != null &&
            destinationLatitude in -90.0..90.0 && destinationLongitude in -180.0..180.0
        ) {
            val now = System.currentTimeMillis()
            if (lastRouteFetchAt == 0L || now - lastRouteFetchAt >= 30_000L || deliveryRoute == null) {
                lastRouteFetchAt = now
                deliveryRoute = loadDeliveryRoute(latitude, longitude, destinationLatitude, destinationLongitude)
            }
        } else {
            deliveryRoute = null
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapHolder[0]?.let { map ->
                map.onPause()
                map.onDetach()
                mapHolder[0] = null
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext: Context ->
                Configuration.getInstance().userAgentValue = viewContext.packageName
                MapView(viewContext).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    setBuiltInZoomControls(false)
                    controller.setZoom(15.5)
                    controller.setCenter(GeoPoint(latitude, longitude))
                    mapHolder[0] = this
                    onResume()
                }
            },
            update = { map ->
                val driverPoint = GeoPoint(latitude, longitude)
                map.overlays.clear()
                deliveryRoute?.let { route ->
                    val line = Polyline().apply {
                        setPoints(route.points)
                        outlinePaint.color = Color.rgb(31, 145, 77)
                        outlinePaint.strokeWidth = 9f
                    }
                    map.overlays.add(line)
                }
                val driverMarker = Marker(map).apply {
                    position = driverPoint
                    title = markerTitle
                    snippet = markerSnippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(driverMarker)
                if (destinationLatitude != null && destinationLongitude != null) {
                    val customerMarker = Marker(map).apply {
                        position = GeoPoint(destinationLatitude, destinationLongitude)
                        title = "Delivery address"
                        snippet = "Your location"
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    map.overlays.add(customerMarker)
                }
                map.controller.setCenter(driverPoint)
                map.invalidate()
            }
        )
        if (deliveryRoute != null) {
            Surface(
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.large,
                shadowElevation = 4.dp
            ) {
                Text(
                    text = "मार्ग " + String.format("%.1f", deliveryRoute!!.distanceKm) +
                        " km  •  अंदाजे " + deliveryRoute!!.durationMinutes + " मिनिटे",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        Text(
            text = "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}
