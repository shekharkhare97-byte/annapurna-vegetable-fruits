package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun LiveOffersHeroBanner(onOfferClick: () -> Unit) {
    val offers = remember { mutableStateListOf<Map<String, Any>>() }
    DisposableEffect(Unit) {
        val listener = FirebaseFirestore.getInstance()
            .collection("customerOffers")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    offers.clear()
                    offers.addAll(snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { mapOf("offerId" to doc.id) + it }
                    }.sortedByDescending {
                        (it["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    })
                }
            }
        onDispose { listener.remove() }
    }

    val pagerState = rememberPagerState(pageCount = { maxOf(1, offers.size) })
    LaunchedEffect(offers.size) {
        if (offers.size > 1) {
            while (isActive) {
                delay(3500)
                pagerState.animateScrollToPage((pagerState.currentPage + 1) % offers.size)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        pageSpacing = 0.dp,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.fillMaxWidth().height(196.dp)
    ) { page ->
        val offer = offers.getOrNull(page)
        val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
        val title = when (language) {
            "hi" -> offer?.get("titleHi")?.toString().orEmpty().ifBlank { offer?.get("title")?.toString().orEmpty() }
            "en" -> offer?.get("titleEn")?.toString().orEmpty().ifBlank { offer?.get("title")?.toString().orEmpty() }
            else -> offer?.get("titleMr")?.toString().orEmpty().ifBlank { offer?.get("title")?.toString().orEmpty() }
        }
        val description = when (language) {
            "hi" -> offer?.get("descriptionHi")?.toString().orEmpty().ifBlank { offer?.get("description")?.toString().orEmpty() }
            "en" -> offer?.get("descriptionEn")?.toString().orEmpty().ifBlank { offer?.get("description")?.toString().orEmpty() }
            else -> offer?.get("descriptionMr")?.toString().orEmpty().ifBlank { offer?.get("description")?.toString().orEmpty() }
        }
        val imageUrl = offer?.get("imageUrl")?.toString().orEmpty()
        val fallback = offer == null
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF238B45), Color(0xFF075B2A))))
                .clickable { onOfferClick() }
        ) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(model = imageUrl, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xD9003518), Color(0x33003518), Color(0x99000000)))))
            Column(
                modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.88f).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Surface(color = Color(0xFFFFD54F), shape = RoundedCornerShape(50)) {
                    Text(
                        if (fallback) "🌿  LIVE OFFERS" else "🔥  LIVE OFFER",
                        color = Color(0xFF3E2723), style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(
                    if (fallback) "आजच्या खास ऑफर्स लवकरच!" else title.ifBlank { "आजची खास ऑफर" },
                    color = Color.White, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (fallback) "ताज्या भाज्या आणि फळांवर खास बचत" else description,
                    color = Color.White, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }
            if (offers.size > 1) {
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    repeat(offers.size.coerceAtMost(6)) { index ->
                        Box(Modifier.size(if (pagerState.currentPage == index) 18.dp else 6.dp, 6.dp)
                            .clip(RoundedCornerShape(50)).background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.55f)))
                    }
                }
            }
        }
    }
}
