package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun NewArrivalsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val language = LocalConfiguration.current.locales[0]?.language ?: "mr"
    fun tr(mr: String, hi: String, en: String) = when (language) { "hi" -> hi; "en" -> en; else -> mr }
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("मागे", "वापस", "Back")) }
            Text(tr("नवीन आगमन", "नए उत्पाद", "New Arrivals"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🌱", style = MaterialTheme.typography.displaySmall)
                Text(tr("नवीन उत्पादने लवकरच उपलब्ध होतील.", "नए उत्पाद जल्द उपलब्ध होंगे।", "New products will be available soon."), fontWeight = FontWeight.SemiBold)
                Text(tr("नवीन भाज्या आणि फळे येथे दिसतील.", "नई सब्ज़ियाँ और फल यहाँ दिखाई देंगे।", "New vegetables and fruits will appear here."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
