package com.example.ui.components

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
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
import androidx.compose.ui.platform.LocalConfiguration
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


private fun createAnnapurnaRiderIcon(context: Context): Drawable {
    val size = 144
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    fun fill(color: Int) { paint.color = color; paint.style = Paint.Style.FILL }
    fun stroke(color: Int, width: Float) { paint.color = color; paint.style = Paint.Style.STROKE; paint.strokeWidth = width; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND }
    // White circular badge with Annapurna green border.
    fill(Color.WHITE); canvas.drawCircle(72f, 72f, 66f, paint)
    stroke(Color.rgb(18, 101, 47), 7f); canvas.drawCircle(72f, 72f, 63f, paint)
    // Scooter wheels and body.
    fill(Color.rgb(25, 35, 31)); canvas.drawCircle(39f, 103f, 15f, paint); canvas.drawCircle(105f, 103f, 15f, paint)
    fill(Color.LTGRAY); canvas.drawCircle(39f, 103f, 6f, paint); canvas.drawCircle(105f, 103f, 6f, paint)
    fill(Color.rgb(31, 145, 77))
    val scooter = Path().apply { moveTo(34f, 91f); lineTo(48f, 79f); lineTo(83f, 81f); lineTo(105f, 93f); lineTo(98f, 100f); lineTo(56f, 100f); close() }
    canvas.drawPath(scooter, paint)
    stroke(Color.rgb(25, 35, 31), 5f); canvas.drawLine(88f, 79f, 98f, 62f, paint); canvas.drawLine(98f, 62f, 111f, 62f, paint)
    // Annapurna-branded delivery box behind the rider.
    fill(Color.rgb(0, 105, 55)); canvas.drawRoundRect(RectF(87f, 38f, 128f, 77f), 7f, 7f, paint)
    stroke(Color.rgb(190, 235, 92), 3f); canvas.drawRoundRect(RectF(89f, 40f, 126f, 75f), 5f, 5f, paint)
    paint.style = Paint.Style.FILL
    paint.color = Color.WHITE
    paint.textSize = 7f
    paint.typeface = android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.BOLD)
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("ANNAPURNA", 107f, 58f, paint)
    paint.textSize = 5f
    canvas.drawText("FRESH", 107f, 66f, paint)
    // Rider legs and dark trousers.
    fill(Color.rgb(30, 52, 82)); canvas.drawRoundRect(RectF(62f, 67f, 91f, 91f), 8f, 8f, paint)
    // Green Annapurna polo torso.
    fill(Color.rgb(0, 105, 55)); val torso = Path().apply { moveTo(53f, 38f); lineTo(83f, 39f); lineTo(95f, 67f); lineTo(82f, 77f); lineTo(54f, 68f); lineTo(45f, 53f); close() }; canvas.drawPath(torso, paint)
    stroke(Color.rgb(190, 235, 92), 3f); canvas.drawLine(68f, 41f, 69f, 61f, paint)
    // Arms reaching the handlebar.
    stroke(Color.rgb(188, 130, 91), 8f); canvas.drawLine(83f, 49f, 101f, 62f, paint); canvas.drawLine(55f, 50f, 42f, 65f, paint)
    // Head and green delivery cap/helmet.
    fill(Color.rgb(188, 130, 91)); canvas.drawCircle(68f, 27f, 15f, paint)
    fill(Color.rgb(0, 105, 55)); canvas.drawArc(RectF(51f, 8f, 84f, 39f), 180f, 180f, true, paint); canvas.drawRoundRect(RectF(64f, 22f, 88f, 28f), 4f, 4f, paint)
    // Small bright-green shirt mark.
    fill(Color.rgb(190, 235, 92)); canvas.drawCircle(78f, 49f, 4f, paint)
    return BitmapDrawable(context.resources, bitmap).apply { setBounds(0, 0, size, size) }
}

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
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"
    fun localized(mr: String, hi: String, en: String) = when (language) {
        "hi" -> hi
        "en" -> en
        else -> mr
    }
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
                    icon = createAnnapurnaRiderIcon(map.context)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                }
                map.overlays.add(driverMarker)
                if (destinationLatitude != null && destinationLongitude != null) {
                    val customerMarker = Marker(map).apply {
                        position = GeoPoint(destinationLatitude, destinationLongitude)
                        title = localized("डिलिव्हरीचा पत्ता", "डिलीवरी का पता", "Delivery address")
                        snippet = localized("तुमचे ठिकाण", "आपकी लोकेशन", "Your location")
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
                    text = localized("मार्ग ", "रास्ता ", "Route ") + String.format(java.util.Locale.US, "%.1f", deliveryRoute!!.distanceKm) +
                        " km  •  " + localized("अंदाजे ", "लगभग ", "approx. ") + deliveryRoute!!.durationMinutes + localized(" मिनिटे", " मिनट", " min"),
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
