package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LiveOffersScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val offers = remember { mutableStateListOf<Map<String, Any>>() }
    var selected by remember { mutableStateOf<Map<String, Any>?>(null) }
    var loading by remember { mutableStateOf(true) }
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("customerOffers")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                loading = false
                if (error == null && snapshot != null) {
                    offers.clear()
                    offers.addAll(snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapOf("offerId" to doc.id) + it }
                    }.sortedByDescending { (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L })
                }
            }
        onDispose { registration.remove() }
    }
    fun localized(item: Map<String, Any>, base: String): String {
        val suffix = when (language) { "hi" -> "Hi"; "en" -> "En"; else -> "Mr" }
        return (item["$base$suffix"]?.toString().orEmpty()).ifBlank { item[base]?.toString().orEmpty() }
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selected != null) selected = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(if (selected == null) "Live Offers" else localized(selected!!, "title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (selected != null) {
            val item = selected!!
            val image = item["imageUrl"]?.toString().orEmpty()
            if (image.isNotBlank()) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(190.dp))
            Text(localized(item, "title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(localized(item, "description").ifBlank { "या ऑफरची अधिक माहिती उपलब्ध नाही." })
            Text("ऑफरची माहिती Annapurna कडून प्रकाशित केली आहे.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (offers.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁", style = MaterialTheme.typography.displaySmall)
                    Text("सध्या कोणतीही Live Offer उपलब्ध नाही.", fontWeight = FontWeight.SemiBold)
                    Text("नवीन ऑफर्स आल्यावर येथे दिसतील.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(offers) { offer ->
                    Card(Modifier.fillMaxWidth().clickable { selected = offer }) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            val image = offer["imageUrl"]?.toString().orEmpty()
                            if (image.isNotBlank()) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(92.dp))
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(localized(offer, "title").ifBlank { "आजची खास ऑफर" }, fontWeight = FontWeight.Bold)
                                Text(localized(offer, "description"), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Text("ऑफर पहा  →", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
