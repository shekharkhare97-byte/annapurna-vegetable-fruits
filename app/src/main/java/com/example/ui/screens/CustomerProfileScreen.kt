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
import com.example.ui.components.OpenStreetMapView
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
                Surface(shape = CircleShape, color = Color(0xFFE8F5E9)) {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(18.dp))
                }
                Column {
                    Text(formState.customerName.ifBlank { tr("अन्नपूर्णा ग्राहक", "अन्नपूर्णा ग्राहक", "Annapurna Customer") }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(formState.mobileNumber.ifBlank { tr("मोबाईल नंबर जोडा", "मोबाइल नंबर जोड़ें", "Add mobile number") }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  " + tr("माझी माहिती", "मेरी जानकारी", "My details"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        androidx.compose.material3.TextButton(onClick = { editing = !editing }) {
                            Text(if (editing) tr("बंद करा", "बंद करें", "Close") else tr("बदला", "बदलें", "Edit"))
                        }
                    }
                    if (editing || formState.customerName.isBlank()) {
                        OutlinedTextField(value = formState.customerName, onValueChange = onNameChange, label = { Text(tr("पूर्ण नाव", "पूरा नाम", "Full name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.mobileNumber, onValueChange = onMobileChange, label = { Text(tr("मोबाईल नंबर", "मोबाइल नंबर", "Mobile number")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = formState.deliveryAddress, onValueChange = onAddressChange, label = { Text(tr("डिलिव्हरी पत्ता", "डिलीवरी पता", "Delivery address")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(onClick = onSaveProfile, enabled = !formState.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                            Text(if (formState.isSubmitting) tr("जतन होत आहे...", "सेव हो रहा है...", "Saving...") else tr("माहिती जतन करा", "जानकारी सेव करें", "Save details"))
                        }
                        formState.cartErrorMarathi?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    } else {
                        Text(formState.customerName)
                        Text(formState.mobileNumber, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                            Text("  " + formState.deliveryAddress.ifBlank { tr("पत्ता जोडलेला नाही", "पता नहीं जोड़ा गया", "No address added") })
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF6EA))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.LocationOn, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  " + tr("लाईव्ह ऑर्डर ट्रॅकिंग", "लाइव ऑर्डर ट्रैकिंग", "Live order tracking"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            listOf(tr("ऑर्डर", "ऑर्डर", "Order"), tr("तयारी", "तैयारी", "Preparing"), tr("पिकअप", "पिकअप", "Pickup"), tr("डिलिव्हरी", "डिलीवरी", "Delivery")).forEachIndexed { index, label ->
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
                            Text(tr("ऑर्डर प्रगती", "ऑर्डर की प्रगति", "Order progress"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            Text("${(trackingProgress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        when (trackingStatus) {
                            "new" -> tr("ऑर्डर मिळाली आहे. दुकानाकडून पुष्टीची प्रतीक्षा आहे.", "ऑर्डर मिल गई है। दुकान की पुष्टि का इंतज़ार है।", "Order received. Waiting for shop confirmation.")
                            "accepted" -> tr("दुकानाने ऑर्डर स्वीकारली आहे.", "दुकान ने ऑर्डर स्वीकार कर ली है।", "The shop has accepted your order.")
                            "preparing" -> tr("तुमची ऑर्डर निवडून पॅक केली जात आहे.", "आपका ऑर्डर चुना और पैक किया जा रहा है।", "Your order is being picked and packed.")
                            "ready_for_pickup" -> tr("ऑर्डर तयार आहे. डिलिव्हरी पार्टनर येत आहे.", "ऑर्डर तैयार है। डिलीवरी पार्टनर आ रहा है।", "Your order is ready. The delivery partner is on the way.")
                            "claimed_pending_shop_otp" -> tr("डिलिव्हरी पार्टनर दुकानात पोहोचला आहे. दुकानाकडून पिकअप OTP ची पुष्टी बाकी आहे.", "डिलीवरी पार्टनर दुकान पहुँच गया है। दुकान से पिकअप OTP की पुष्टि बाकी है।", "The delivery partner has reached the shop. Shop confirmation of the pickup OTP is pending.")
                            "picked_up" -> tr("डिलिव्हरी पार्टनरने दुकानातून तुमची ऑर्डर घेतली आहे.", "डिलीवरी पार्टनर ने दुकान से आपका ऑर्डर ले लिया है।", "The delivery partner has picked up your order from the shop.")
                            "out_for_delivery" -> tr("तुमची ऑर्डर डिलिव्हरीसाठी निघाली आहे.", "आपका ऑर्डर डिलीवरी के लिए निकल गया है।", "Your order is out for delivery.")
                            "completed" -> tr("ऑर्डर यशस्वीपणे पोहोचवली आहे.", "ऑर्डर सफलतापूर्वक पहुँचा दिया गया है।", "Your order has been delivered successfully.")
                            "cancelled" -> tr("ही ऑर्डर रद्द झाली आहे.", "यह ऑर्डर रद्द कर दिया गया है।", "This order has been cancelled.")
                            "unavailable" -> tr("ट्रॅकिंग सध्या उपलब्ध नाही. इंटरनेट तपासा.", "ट्रैकिंग अभी उपलब्ध नहीं है। इंटरनेट जाँचें।", "Tracking is currently unavailable. Check your internet connection.")
                            else -> tr("ऑर्डर केल्यानंतर तिचे अपडेट येथे दिसतील.", "ऑर्डर करने के बाद इसके अपडेट यहाँ दिखेंगे।", "Order updates will appear here after you place an order.")
                        }
                    )
                    if (trackingLocation.isNotBlank()) {
                        val coordinates = trackingLocation.split(",")
                        Text(tr("डिलिव्हरी पार्टनरचे Live लोकेशन उपलब्ध आहे.", "डिलीवरी पार्टनर की Live Location उपलब्ध है।", "Delivery partner live location is available."), fontWeight = FontWeight.SemiBold)
                        trackingUpdatedAtMillis?.let { updatedAt ->
                            val timeLabel = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(updatedAt))
                            Text("${tr("शेवटचे लोकेशन अपडेट", "आखिरी लोकेशन अपडेट", "Last location update")}: $timeLabel", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (coordinates.size == 2) {
                            val latitude = coordinates.getOrNull(0)?.toDoubleOrNull()
                            val longitude = coordinates.getOrNull(1)?.toDoubleOrNull()
                            if (latitude != null && longitude != null) {
                                OpenStreetMapView(
                                    latitude = latitude,
                                    longitude = longitude,
                                    markerTitle = tr("डिलिव्हरी पार्टनर", "डिलीवरी पार्टनर", "Delivery partner"),
                                    markerSnippet = tr("तुमच्या ऑर्डरचे Live लोकेशन", "आपके ऑर्डर की Live Location", "Live location of your order"),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(230.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                )
                                Button(onClick = {
                                    val mapUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(tr("मोठा नकाशा उघडा", "बड़ा नक्शा खोलें", "Open larger map"))
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
                Text("  " + tr("माझ्या ऑर्डर्स", "मेरे ऑर्डर", "My orders"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = onOpenOrders) { Text(tr("सर्व पहा", "सभी देखें", "View all")) }
            }
        }
        if (recentOrders.isEmpty()) {
            item { Text(tr("अजून कोणतीही ऑर्डर नाही. तुमची पहिली खरेदी सुरू करा!", "अभी कोई ऑर्डर नहीं है। अपनी पहली खरीदारी शुरू करें!", "No orders yet. Start your first purchase!")) }
        } else {
            items(recentOrders.take(5)) { order ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${tr("ऑर्डर #", "ऑर्डर #", "Order #")}${order.orderId}", fontWeight = FontWeight.Bold)
                        Text(localizedOrderItemsSummary(order, java.util.Locale.getDefault().language), maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹${order.totalAmountRupeesDecimal}  •  ${java.text.DateFormat.getDateInstance().format(java.util.Date(order.createdAtMillis))}", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
        item {
            val referralCode = remember(formState.mobileNumber) {
                val seed = formState.mobileNumber.filter { it.isDigit() }.ifBlank { "ANNAPURNA" }
                "ANN" + Integer.toUnsignedString(seed.hashCode(), 36).uppercase(java.util.Locale.ROOT).take(7)
            }
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF6EA))
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CardGiftcard, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  " + tr("मित्रांना आमंत्रित करा आणि मोफत डिलिव्हरी मिळवा", "दोस्तों को आमंत्रित करें और मुफ्त डिलीवरी पाएँ", "Refer friends and earn free delivery"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text(tr("तुमचा रेफरल कोड", "आपका रेफरल कोड", "Your referral code"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                        Text(referralCode, modifier = Modifier.fillMaxWidth().padding(14.dp), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color(0xFF176B3A))
                    }
                    Button(
                        onClick = {
                            val message = tr(
                                "Annapurna Vegetables and Fruits ॲप वापरा! माझा रेफरल कोड $referralCode वापरा.",
                                "Annapurna Vegetables and Fruits ऐप इस्तेमाल करें! मेरा रेफरल कोड $referralCode इस्तेमाल करें।",
                                "Try Annapurna Vegetables and Fruits! Use my referral code $referralCode."
                            )
                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            }, tr("मित्रांना शेअर करा", "दोस्तों के साथ शेयर करें", "Share with friends")))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr("रेफरल कोड शेअर करा", "रेफरल कोड शेयर करें", "Share referral code"))
                    }
                    HorizontalDivider()
                    Text(tr("रेफरल बक्षिसे", "रेफरल इनाम", "Referral rewards"), fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("1 यशस्वी मित्र", "1 सफल मित्र", "1 successful friend"))
                        Text(tr("1 डिलिव्हरी मोफत", "1 डिलीवरी मुफ्त", "1 free delivery"), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("3 यशस्वी मित्र", "3 सफल मित्र", "3 successful friends"))
                        Text(tr("5 डिलिव्हरी मोफत", "5 डिलीवरी मुफ्त", "5 free deliveries"), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(tr("10 यशस्वी मित्र", "10 सफल मित्र", "10 successful friends"))
                        Text(tr("7 दिवस डिलिव्हरी मोफत", "7 दिन डिलीवरी मुफ्त", "7 days free delivery"), fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        tr(
                            "मित्राने तुमचा कोड वापरून ॲप इन्स्टॉल व नोंदणी करावी आणि किमान 30 दिवस ॲप वापरावे. बनावट किंवा डुप्लिकेट खाते ग्राह्य धरले जाणार नाही. अटींची सर्व्हरवर पडताळणी झाल्यावरच रेफरल व बक्षीस मंजूर होईल.",
                            "मित्र को आपका कोड इस्तेमाल करके ऐप इंस्टॉल और रजिस्टर करना होगा तथा कम से कम 30 दिन ऐप इस्तेमाल करना होगा। नकली या डुप्लिकेट खाते मान्य नहीं होंगे। सर्वर सत्यापन के बाद ही रेफरल और इनाम मंजूर होगा।",
                            "Your friend must install the app, register with your code, and use it for at least 30 days. Fake or duplicate accounts do not qualify. Referrals and rewards are granted only after server-side verification."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  " + tr("माझी आवडती उत्पादने", "मेरे पसंदीदा उत्पाद", "My favorite products"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    val favorites = produceItems.filter { it.id in favoriteIds }
                    if (favorites.isEmpty()) {
                        Text(tr("अजून आवडते उत्पादन निवडलेले नाही. बाजारात ❤️ दाबून उत्पादने येथे सेव्ह करा.", "अभी कोई पसंदीदा उत्पाद नहीं चुना। बाज़ार में ❤️ दबाकर उत्पाद यहाँ सेव करें।", "No favorite products yet. Tap ❤️ in the market to save products here."))
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
                                }) { Text(tr("काढा", "हटाएँ", "Remove")) }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AddLocationAlt, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Text("  " + tr("माझे सेव्ह केलेले पत्ते", "मेरे सेव किए गए पते", "My saved addresses"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    savedAddresses.forEach { address ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(address, style = MaterialTheme.typography.bodyMedium)
                                Text(tr("ऑर्डरसाठी हा पत्ता वापरा", "ऑर्डर के लिए इस पते का उपयोग करें", "Use this address for orders"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(onClick = { onAddressChange(address) }) { Text(tr("निवडा", "चुनें", "Select")) }
                        }
                        HorizontalDivider()
                    }
                    OutlinedTextField(value = addressDraft, onValueChange = { addressDraft = it }, label = { Text(tr("नवीन घर / ऑफिस पत्ता", "नया घर / ऑफिस का पता", "New home / office address")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
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
                    ) { Text(tr("पत्ता सेव्ह करा", "पता सेव करें", "Save address")) }
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.FavoriteBorder, tr("आवडती उत्पादने", "पसंदीदा उत्पाद", "Favorite products"), tr("आवडते पदार्थ लवकर शोधा", "पसंदीदा चीज़ें जल्दी खोजें", "Find favorite items quickly"))
                    HorizontalDivider()
                    ProfileShortcut(Icons.Outlined.CardGiftcard, tr("कूपन आणि ऑफर्स", "कूपन और ऑफ़र", "Coupons and offers"), tr("सध्या उपलब्ध ऑफर्स कार्टमध्ये तपासा", "उपलब्ध ऑफ़र कार्ट में देखें", "Check available offers in your cart"))
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.HelpOutline, tint = MaterialTheme.colorScheme.primary, contentDescription = null)
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(tr("मदत आणि ग्राहक सेवा", "मदद और ग्राहक सेवा", "Help and customer service"), fontWeight = FontWeight.SemiBold)
                            Text(tr("ऑर्डर, पेमेंट आणि डिलिव्हरीविषयी सामान्य मदत", "ऑर्डर, भुगतान और डिलीवरी से जुड़ी मदद", "Help with orders, payments and delivery"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showHelp = true }) { Text(tr("मदत", "मदद", "Help")) }
                    }
                    HorizontalDivider()
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text(tr("अन्नपूर्णा ग्राहक मदत", "अन्नपूर्णा ग्राहक सहायता", "Annapurna Customer Help")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(tr("SOS / पोलीस: 112", "SOS / पुलिस: 112", "SOS / Police: 112"))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))) }) { Text(tr("SOS • Police 112 वर कॉल करा", "SOS • Police 112 पर कॉल करें", "Call SOS • Police 112")) }
                    Text(tr("ग्राहक मदत: 8530011324", "ग्राहक सहायता: 8530011324", "Customer Help: 8530011324"))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:8530011324"))) }) { Text(tr("ग्राहक मदत केंद्राला कॉल करा", "ग्राहक सहायता केंद्र पर कॉल करें", "Call Customer Help")) }
                    Text(tr("ईमेल: annpurnavegitablesandfruits@gmail.com", "ईमेल: annpurnavegitablesandfruits@gmail.com", "Email: annpurnavegitablesandfruits@gmail.com"))
                    TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:annpurnavegitablesandfruits@gmail.com"))) }) { Text(tr("ईमेल पाठवा", "ईमेल भेजें", "Send email")) }
                }
            },
            confirmButton = { TextButton(onClick = { showHelp = false }) { Text(tr("ठीक आहे", "ठीक है", "OK")) } }
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

@Composable
private fun tr(mr: String, hi: String, en: String): String {
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
    return when (language) { "hi" -> hi; "en" -> en; else -> mr }
}
