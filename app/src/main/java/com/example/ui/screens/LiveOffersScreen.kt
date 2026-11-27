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
    fun tr(mr: String, hi: String, en: String) = when (language) { "hi" -> hi; "en" -> en; else -> mr }
    fun localized(item: Map<String, Any>, base: String): String {
        val suffix = when (language) { "hi" -> "Hi"; "en" -> "En"; else -> "Mr" }
        return (item["$base$suffix"]?.toString().orEmpty()).ifBlank { item[base]?.toString().orEmpty() }
    }
    Column(modifier = modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (selected != null) selected = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("मागे", "वापस", "Back"))
            }
            Text(if (selected == null) tr("थेट ऑफर्स", "लाइव ऑफ़र", "Live Offers") else localized(selected!!, "title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (selected != null) {
            val item = selected!!
            val image = item["imageUrl"]?.toString().orEmpty()
            if (image.isNotBlank()) AsyncImage(model = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(190.dp))
            Text(localized(item, "title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(localized(item, "description").ifBlank { tr("या ऑफरची अधिक माहिती उपलब्ध नाही.", "इस ऑफ़र की अधिक जानकारी उपलब्ध नहीं है।", "More information about this offer is unavailable.") })
            Text(tr("ऑफरची माहिती अन्नपूर्णाकडून प्रकाशित केली आहे.", "ऑफ़र की जानकारी अन्नपूर्णा द्वारा प्रकाशित की गई है।", "Offer details are published by Annapurna."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (loading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (offers.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🎁", style = MaterialTheme.typography.displaySmall)
                    Text(tr("सध्या कोणतीही थेट ऑफर उपलब्ध नाही.", "अभी कोई लाइव ऑफ़र उपलब्ध नहीं है।", "No live offers are available right now."), fontWeight = FontWeight.SemiBold)
                    Text(tr("नवीन ऑफर्स आल्यावर येथे दिसतील.", "नई ऑफ़र आने पर यहाँ दिखाई देंगी।", "New offers will appear here when available."), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(localized(offer, "title").ifBlank { tr("आजची खास ऑफर", "आज की खास ऑफ़र", "Today’s special offer") }, fontWeight = FontWeight.Bold)
                                Text(localized(offer, "description"), maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                Text(tr("ऑफर पहा  →", "ऑफ़र देखें  →", "View offer  →"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
