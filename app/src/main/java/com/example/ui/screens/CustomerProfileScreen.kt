package com.example.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.data.local.OrderEntity
import com.example.data.model.ProduceItem
import com.example.ui.viewmodel.CustomerFormState

@Composable
fun CustomerProfileScreen(
    formState: CustomerFormState,
    recentOrders: List<OrderEntity>,
    produceItems: List<ProduceItem>,
    trackingStatus: String,
    trackingLocation: String,
    trackingUpdatedAtMillis: Long?,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSaveProfile: () -> Unit,
    onOpenOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val addressPrefs = remember { context.getSharedPreferences("annapurna_saved_addresses", android.content.Context.MODE_PRIVATE) }
    var savedAddresses by remember { mutableStateOf(addressPrefs.getStringSet("items", emptySet())?.toList().orEmpty()) }
    var addressDraft by remember { mutableStateOf("") }
    val favoritePrefs = remember { context.getSharedPreferences("annapurna_favorites", android.content.Context.MODE_PRIVATE) }
    var favoriteIds by remember { mutableStateOf(favoritePrefs.getStringSet("ids", emptySet())?.toSet().orEmpty()) }
    var showHelp by remember { mutableStateOf(false) }
    LaunchedEffect(formState.customerName, formState.mobileNumber, formState.deliveryAddress) {
        if (formState.customerName.isNotBlank() && formState.mobileNumber.isNotBlank()) editing = false
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp))
                }
                Column {
                    Text(formState.customerName.ifBlank { "अन्नपूर्णा ग्राहक" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(formState.mobileNumber.ifBlank { "मोबाईल नंबर जोडा" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  माझी माहिती", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { editing = !editing }) {
                            Text(if (editing) "बंद करा" else "बदला")
                        }
                    }
                    if (editing || formState.customerName.isBlank()) {
                        OutlinedTextField(value = formState.customerName, onValueChange = onNameChange, label = { Text("पूर्ण नाव") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.mobileNumber, onValueChange = onMobileChange, label = { Text("मोबाईल नंबर") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.deliveryAddress, onValueChange = onAddressChange, label = { Text("डिलिव्हरी पत्ता") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(onClick = onSaveProfile, enabled = !formState.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                            Text(if (formState.isSubmitting) "जतन होत आहे..." else "माहिती जतन करा")
                        }
                        formState.cartErrorMarathi?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    } else {
                        Text(formState.customerName)
                        Text(formState.mobileNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            Text("  " + formState.deliveryAddress.ifBlank { "पत्ता जोडलेला नाही" })
                        }
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  लाईव्ह ऑर्डर ट्रॅकिंग", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val trackingProgress = when (trackingStatus) {
                        "new" -> 0.15f
                        "accepted" -> 0.30f
                        "preparing" -> 0.45f
                        "ready_for_pickup", "claimed_pending_shop_otp" -> 0.60f
                        "picked_up", "out_for_delivery" -> 0.82f
                        "completed" -> 1f
                        else -> 0f
                    }
                    if (trackingProgress > 0f && trackingStatus != "cancelled" && trackingStatus != "unavailable") {
                        val activeStep = when (trackingStatus) {
                            "new" -> 0
                            "accepted", "preparing" -> 1
                            "ready_for_pickup", "claimed_pending_shop_otp", "picked_up" -> 2
                            "out_for_delivery", "completed" -> 3
                            else -> -1
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("ऑर्डर", "तयारी", "Pickup", "डिलिव्हरी").forEachIndexed { index, label ->
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        if (index <= activeStep) "●" else "○",
                                        color = if (index <= activeStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (index <= activeStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (index == activeStep) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = trackingProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("ऑर्डर प्रगती", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("${(trackingProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        when (trackingStatus) {
                            "new" -> "ऑर्डर मिळाली आहे. दुकानाकडून पुष्टीची प्रतीक्षा आहे."
                            "accepted" -> "दुकानाने ऑर्डर स्वीकारली आहे."
                            "preparing" -> "तुमची ऑर्डर निवडून पॅक केली जात आहे."
                            "ready_for_pickup" -> "ऑर्डर तयार आहे. डिलिव्हरी पार्टनर येत आहे."
                            "claimed_pending_shop_otp" -> "डिलिव्हरी पार्टनर दुकानात पोहोचला आहे. दुकानाकडून Pickup OTP पुष्टी बाकी आहे."
                            "picked_up" -> "डिलिव्हरी पार्टनरने दुकानातून तुमची ऑर्डर घेतली आहे."
                            "out_for_delivery" -> "तुमची ऑर्डर डिलिव्हरीसाठी निघाली आहे."
                            "completed" -> "ऑर्डर यशस्वीपणे पोहोचवली आहे."
                            "cancelled" -> "ही ऑर्डर रद्द झाली आहे."
                            "unavailable" -> "ट्रॅकिंग सध्या उपलब्ध नाही. इंटरनेट तपासा."
                            else -> "ऑर्डर केल्यानंतर तिचे अपडेट येथे दिसतील."
                        }
                    )
                    if (trackingLocation.isNotBlank()) {
                        val coordinates = trackingLocation.split(",")
                        Text("डिलिव्हरी पार्टनरचे Live लोकेशन उपलब्ध आहे.", fontWeight = FontWeight.SemiBold)
                        trackingUpdatedAtMillis?.let { updatedAt ->
                            val timeLabel = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(updatedAt))
                            Text("शेवटचे लोकेशन अपडेट: $timeLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (coordinates.size == 2) {
                            val latitude = coordinates.getOrNull(0)?.toDoubleOrNull()
                            val longitude = coordinates.getOrNull(1)?.toDoubleOrNull()
                            if (latitude != null && longitude != null) {
                                val deliveryPartnerPosition = LatLng(latitude, longitude)
                                val cameraPositionState = rememberCameraPositionState {
                                    position = CameraPosition.fromLatLngZoom(deliveryPartnerPosition, 15f)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    GoogleMap(
                                        modifier = Modifier.fillMaxSize(),
                                        cameraPositionState = cameraPositionState,
                                        uiSettings = MapUiSettings(zoomControlsEnabled = true)
                                    ) {
                                        Marker(
                                            state = MarkerState(position = deliveryPartnerPosition),
                                            title = "डिलिव्हरी पार्टनर",
                                            snippet = "तुमच्या ऑर्डरचे Live लोकेशन"
                                        )
                                    }
                                }
                                Button(onClick = {
                                    val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Google Maps मध्ये मोठा नकाशा उघडा")
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.ReceiptLong, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                Text("  माझ्या ऑर्डर्स", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onOpenOrders) { Text("सर्व पहा") }
            }
        }
        if (recentOrders.isEmpty()) {
            item { Text("अजून कोणतीही ऑर्डर नाही. तुमची पहिली खरेदी सुरू करा!") }
        } else {
            items(recentOrders.take(5)) { order ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ऑर्डर #${order.orderId}", fontWeight = FontWeight.Bold)
                        Text(order.itemsSummaryMarathi, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${order.totalAmountRupeesDecimal}  •  ${java.text.DateFormat.getDateInstance().format(java.util.Date(order.createdAtMillis))}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  माझी आवडती उत्पादने", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val favorites = produceItems.filter { it.id in favoriteIds }
                    if (favorites.isEmpty()) {
                        Text("अजून आवडते उत्पादन निवडलेले नाही. बाजारात ❤️ दाबून उत्पादने येथे सेव्ह करा.")
                    } else {
                        favorites.forEach { product ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(product.displayName(), fontWeight = FontWeight.SemiBold)
                                    Text("₹" + product.priceRupees.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                TextButton(onClick = {
                                    favoriteIds = favoriteIds - product.id
                                    favoritePrefs.edit().putStringSet("ids", favoriteIds).apply()
                                }) { Text("काढा") }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AddLocationAlt, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  माझे सेव्ह केलेले पत्ते", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    savedAddresses.forEach { address ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(address, style = MaterialTheme.typography.bodyMedium)
                                Text("ऑर्डरसाठी हा पत्ता वापरा", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onAddressChange(address) }) { Text("निवडा") }
                        }
                        HorizontalDivider()
                    }
                    OutlinedTextField(value = addressDraft, onValueChange = { addressDraft = it }, label = { Text("नवीन घर / ऑफिस पत्ता") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Button(
                        onClick = {
                            val value = addressDraft.trim()
                            if (value.isNotEmpty() && value !in savedAddresses) {
                                savedAddresses = (savedAddresses + value).takeLast(5)
                                addressPrefs.edit().putStringSet("items", savedAddresses.toSet()).apply()
                                addressDraft = ""
                            }
                        },
                        enabled = addressDraft.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("पत्ता सेव्ह करा") }
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.FavoriteBorder, "आवडती उत्पादने", "आवडते पदार्थ लवकर शोधा")
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.CardGiftcard, "कूपन आणि ऑफर्स", "सध्या उपलब्ध ऑफर्स कार्टमध्ये तपासा")
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.HelpOutline, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text("मदत आणि ग्राहक सेवा", fontWeight = FontWeight.SemiBold)
                            Text("ऑर्डर, पेमेंट आणि डिलिव्हरीविषयी सामान्य मदत", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showHelp = true }) { Text("मदत") }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("अन्नपूर्णा ग्राहक मदत") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ऑर्डरची स्थिती Profile मधील Live Order Tracking मध्ये पहा.")
                    Text("पेमेंट यशस्वी झाले पण ऑर्डर दिसत नसेल, तर थोडा वेळ थांबा आणि इंटरनेट तपासा.")
                    Text("डिलिव्हरी पत्ता बदलायचा असल्यास सेव्ह केलेल्या पत्त्यांमधून योग्य पत्ता निवडा.")
                    Text("तातडीच्या मदतीसाठी दुकानाच्या अधिकृत संपर्क क्रमांकावर संपर्क करा.")
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text("ठीक आहे") } }
        )
    }
}

@Composable
private fun ProfileShortcut(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
