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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
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
    val prefs = remember { context.getSharedPreferences("annapurna_customer_entry", Context.MODE_PRIVATE) }
    var completed by remember { mutableStateOf(prefs.getBoolean("onboarding_complete", false)) }
    var phone by remember { mutableStateOf(prefs.getString("customer_mobile", "") ?: "") }
    var stage by remember { mutableStateOf(if (completed) "done" else "phone") }
    var error by remember { mutableStateOf<String?>(null) }

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
                    "अन्नपूर्णा Vegetables and Fruits",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = AnnapurnaGreen,
                    textAlign = TextAlign.Center
                )
                when (stage) {
                    "phone" -> {
                        Text("पुढे जाण्यासाठी मोबाइल नंबर टाका", fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter(Char::isDigit).takeLast(10); error = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("मोबाइल नंबर") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            placeholder = { Text("१० अंकी मोबाइल नंबर") }
                        )
                        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Button(
                            onClick = {
                                val normalized = phone.filter(Char::isDigit).takeLast(10)
                                if (!normalized.matches(Regex("^[6-9][0-9]{9}$"))) {
                                    error = "योग्य १० अंकी मोबाइल नंबर टाका"
                                } else {
                                    prefs.edit().putString("customer_mobile", normalized).apply()
                                    phone = normalized
                                    stage = "splash"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AnnapurnaGreen)
                        ) { Text("पुढे जा", fontWeight = FontWeight.Bold) }
                        Text("OTP किंवा पासवर्डची गरज नाही.", color = Color.DarkGray, style = MaterialTheme.typography.bodySmall)
                    }
                    "splash" -> {
                        Spacer(Modifier.height(8.dp))
                        Text("ताज्या भाज्या आणि फळे आता घरपोच!", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    }
                    "category" -> {
                        Text("तुम्हाला काय खरेदी करायचे आहे?", fontWeight = FontWeight.Bold)
                        CategoryChoice("🥬", "Vegetables", "ताज्या भाज्या") {
                            finishEntry(prefs, "vegetables") { completed = true; stage = "done" }
                        }
                        CategoryChoice("🍎", "Fruits", "ताजी फळे") {
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
