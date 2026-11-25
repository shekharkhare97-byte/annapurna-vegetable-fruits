package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

/** Displays all active Admin offers in sequence on app launch. */
@Composable
fun CustomerOffersSplashOverlay() {
    val offers = remember { mutableStateListOf<Map<String, Any>>() }
    var loading by remember { mutableStateOf(true) }
    var dismissed by rememberSaveable { mutableStateOf(false) }
    var currentIndex by rememberSaveable { mutableStateOf(0) }
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"

    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance().collection("customerOffers")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    offers.clear()
                    offers.addAll(snapshot.documents.mapNotNull { document ->
                        document.data?.let { mapOf("offerId" to document.id) + it }
                    }.sortedByDescending { offer ->
                        when (val createdAt = offer["createdAt"]) {
                            is Timestamp -> createdAt.toDate().time
                            is Number -> createdAt.toLong()
                            else -> 0L
                        }
                    })
                    if (currentIndex >= offers.size) currentIndex = (offers.size - 1).coerceAtLeast(0)
                }
                loading = false
            }
        onDispose { registration.remove() }
    }

    LaunchedEffect(loading, dismissed, currentIndex, offers.size) {
        if (!loading && !dismissed && offers.isNotEmpty()) {
            delay(5000)
            if (currentIndex < offers.lastIndex) currentIndex += 1 else dismissed = true
        }
    }

    if (loading || dismissed || offers.isEmpty()) return

    val offer = offers.getOrNull(currentIndex) ?: return
    fun localized(base: String): String {
        val suffix = when (language) { "hi" -> "Hi"; "en" -> "En"; else -> "Mr" }
        return offer["$" + base + suffix]?.toString().orEmpty()
            .ifBlank { offer[base]?.toString().orEmpty() }
    }
    val title = localized("title").ifBlank {
        when (language) { "hi" -> "आज की खास ऑफ़र"; "en" -> "Special offer"; else -> "आजची खास ऑफर" }
    }
    val description = localized("description")
    val imageUrl = offer["imageUrl"]?.toString().orEmpty()
    val primary = Color(0xFF168447)

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF063B20), Color(0xFF0C6B37), Color(0xFF052C19)))
        ),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl.isNotBlank()) {
            AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0xB8002915)))
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ANNAPURNA VEGETABLES AND FRUITS", color = Color.White,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Surface(color = Color(0xFFFFD54F), shape = RoundedCornerShape(50)) {
                Text(
                    when (language) { "hi" -> "🎉  खास ऑफ़र"; "en" -> "🎉  SPECIAL OFFER"; else -> "🎉  खास ऑफर" },
                    color = Color(0xFF3E2723), fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                )
            }
            Spacer(Modifier.height(22.dp))
            if (imageUrl.isNotBlank()) {
                Surface(shape = RoundedCornerShape(26.dp), shadowElevation = 8.dp) {
                    AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(260.dp))
                }
                Spacer(Modifier.height(22.dp))
            }
            Text(title, color = Color.White, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            if (description.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(description, color = Color.White, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            Text("${currentIndex + 1} / ${offers.size}", color = Color.White,
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(18.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { if (currentIndex > 0) currentIndex -= 1 }, enabled = currentIndex > 0,
                    modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                    Text(when (language) { "hi" -> "पीछे"; "en" -> "Previous"; else -> "मागे" })
                }
                Button(onClick = { if (currentIndex < offers.lastIndex) currentIndex += 1 else dismissed = true },
                    modifier = Modifier.weight(1.25f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = primary)) {
                    Text(when (language) {
                        "hi" -> if (currentIndex < offers.lastIndex) "अगली ऑफ़र" else "शुरू करें"
                        "en" -> if (currentIndex < offers.lastIndex) "Next offer" else "Start shopping"
                        else -> if (currentIndex < offers.lastIndex) "पुढची ऑफर" else "खरेदी सुरू करा"
                    }, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { dismissed = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
                Text(when (language) { "hi" -> "छोड़ें"; "en" -> "Skip offers"; else -> "ऑफर्स वगळा" })
            }
        }
    }
}
