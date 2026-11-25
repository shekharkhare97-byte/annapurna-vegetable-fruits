package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ProduceItem

@Composable
fun NewArrivalsScreen(
    products: List<ProduceItem>,
    onAddToCart: (String, Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"
    fun tr(mr: String, hi: String, en: String) = when (language) { "hi" -> hi; "en" -> en; else -> mr }
    // Only Admin-tagged products belong here. Never label the whole catalogue as new.
    val arrivals = products.filter {
        it.catalogSection.trim().lowercase().replace('-', '_').replace(' ', '_') == "new_arrivals"
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("मागे", "वापस", "Back"))
            }
            Text(tr("नवीन आगमन", "नए उत्पाद", "New Arrivals"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (arrivals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🌱", style = MaterialTheme.typography.displaySmall)
                    Text(tr("नवीन उत्पादने सध्या उपलब्ध नाहीत.", "अभी नए उत्पाद उपलब्ध नहीं हैं।", "No new arrivals right now."), fontWeight = FontWeight.SemiBold)
                    Text(
                        tr("Admin मध्ये उत्पादनाचा विभाग “new_arrivals” निवडल्यानंतर ते येथे दिसेल.",
                           "Admin में उत्पाद का विभाग “new_arrivals” चुनने पर वे यहाँ दिखेंगे।",
                           "Products appear here when Admin assigns the “new_arrivals” catalog section."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(arrivals, key = { it.id }) { item ->
                    Card {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            if (!item.imageUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = item.displayName(),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxWidth().height(112.dp)
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(112.dp), contentAlignment = Alignment.Center) {
                                    Text("🥬", style = MaterialTheme.typography.displayMedium)
                                }
                            }
                            Text(item.displayName(), fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("₹" + item.priceRupees.toString() + " / " + item.displayUnit(), style = MaterialTheme.typography.bodyMedium)
                            Button(
                                onClick = { onAddToCart(item.id, 1000) },
                                enabled = !item.isOutOfStock,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (item.isOutOfStock) tr("संपला आहे", "माल खत्म", "Out of stock") else tr("कार्टमध्ये टाका", "कार्ट में जोड़ें", "Add"))
                            }
                        }
                    }
                }
            }
        }
    }
}
