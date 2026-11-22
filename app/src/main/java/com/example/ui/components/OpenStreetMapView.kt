package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OpenStreetMapView(
    latitude: Double,
    longitude: Double,
    markerTitle: String,
    markerSnippet: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val mapHolder = remember { arrayOfNulls<MapView>(1) }

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
                    controller.setZoom(16.0)
                    controller.setCenter(GeoPoint(latitude, longitude))
                    mapHolder[0] = this
                    onResume()
                }
            },
            update = { map ->
                val point = GeoPoint(latitude, longitude)
                map.overlays.clear()
                map.controller.setCenter(point)
                val marker = Marker(map).apply {
                    position = point
                    title = markerTitle
                    snippet = markerSnippet
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                map.invalidate()
            }
        )
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
