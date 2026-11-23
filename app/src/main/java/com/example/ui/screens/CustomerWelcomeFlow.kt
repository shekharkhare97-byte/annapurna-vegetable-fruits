package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import kotlinx.coroutines.delay

private val AnnapurnaGreen = Color(0xFF168447)
private val AnnapurnaPale = Color(0xFFF1F8F2)

@Composable
fun CustomerWelcomeFlow(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val selectedLanguage = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
    val prefs = remember { context.getSharedPreferences("annapurna_customer_entry", Context.MODE_PRIVATE) }
    var completed by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }
    var stage by remember { mutableStateOf(if (completed) "done" else "splash") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* The customer may continue even if optional permissions are declined. */ }

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("permission_prompt_shown", false)) {
            prefs.edit().putBoolean("permission_prompt_shown", true).apply()
            val requested = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            if (Build.VERSION.SDK_INT >= 33) requested += Manifest.permission.POST_NOTIFICATIONS
            val missing = requested.filter {
                ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(stage) {
        if (stage == "splash") {
            delay(2000)
            stage = "category"
        }
    }

    if (stage == "done" || completed) {
        content()
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AnnapurnaPale).padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_launcher_logo),
                    contentDescription = "Annapurna Vegetables and Fruits",
                    modifier = Modifier.size(112.dp)
                )
                Text(
                    customerText(selectedLanguage, "अन्नपूर्णा भाजीपाला आणि फळे", "अन्नपूर्णा सब्ज़ियाँ और फल", "Annapurna Vegetables and Fruits"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AnnapurnaGreen,
                    textAlign = TextAlign.Center
                )
                when (stage) {
                    "splash" -> {
                        Spacer(Modifier.height(8.dp))
                        Text(customerText(selectedLanguage, "ताज्या भाज्या आणि फळे आता घरपोच!", "ताज़ी सब्ज़ियाँ और फल अब घर पर!", "Fresh vegetables and fruits delivered to your home!"), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    "category" -> {
                        Text(customerText(selectedLanguage, "तुम्हाला काय खरेदी करायचे आहे?", "आप क्या खरीदना चाहते हैं?", "What would you like to shop for?"), fontWeight = FontWeight.Bold)
                        CategoryChoice("🥬", customerText(selectedLanguage, "भाज्या", "सब्ज़ियाँ", "Vegetables"), customerText(selectedLanguage, "ताज्या भाज्या", "ताज़ी सब्ज़ियाँ", "Fresh vegetables")) {
                            finishEntry(prefs, "vegetables") { completed = true; stage = "done" }
                        }
                        CategoryChoice("🍎", customerText(selectedLanguage, "फळे", "फल", "Fruits"), customerText(selectedLanguage, "ताजी फळे", "ताज़े फल", "Fresh fruits")) {
                            finishEntry(prefs, "fruits") { completed = true; stage = "done" }
                        }
                    }
                }
            }
        }
    }
}

private fun finishEntry(prefs: android.content.SharedPreferences, category: String, onDone: () -> Unit) {
    prefs.edit().putString("selected_category", category).putBoolean("onboarding_complete", true).apply()
    onDone()
}

@Composable
private fun CategoryChoice(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F4EA))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineLarge)
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = AnnapurnaGreen)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun customerText(language: String, mr: String, hi: String, en: String): String {
    return when (language) { "hi" -> hi; "en" -> en; else -> mr }
}
