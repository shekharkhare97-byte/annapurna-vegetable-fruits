package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.Location
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.OrderEntity
import com.example.data.repository.CartLineItem
import com.example.data.model.WeightPricing
import com.example.ui.viewmodel.CustomerFormState
import com.example.util.MarathiFormatters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@SuppressLint("MissingPermission")
@Composable
fun CartAndOrderScreen(
    cartItems: List<CartLineItem>,
    formState: CustomerFormState,
    recentOrders: List<OrderEntity>,
    confirmedOrder: OrderEntity?,
    trackingStatus: String,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: (Double?, Double?, Int, Double?, String, String) -> Unit,
    onDismissReceipt: () -> Unit,
    onContinueShopping: () -> Unit,
    onReorder: (OrderEntity) -> Unit,
    onBackToBazaar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToBazaar)

    val context = LocalContext.current
    var showGpsOffDialog by remember { mutableStateOf(false) }
    var deliveryCharge by rememberSaveable { mutableStateOf<Int?>(null) }
    var deliveryDistanceKm by rememberSaveable { mutableStateOf<Double?>(null) }
    var deliveryLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var deliveryLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var deliveryMessage by rememberSaveable { mutableStateOf("डिलिव्हरी चार्ज आपोआप मोजत आहे...") }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var paymentMethod by rememberSaveable { mutableStateOf("COD") }
    var transactionId by rememberSaveable { mutableStateOf("") }
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    fun calculateDeliveryFromLocation(location: Location) {
        deliveryLatitude = location.latitude; deliveryLongitude = location.longitude
        deliveryCharge = null; deliveryDistanceKm = null; deliveryMessage = "डिलिव्हरी अंतर मोजत आहे..."
        coroutineScope.launch {
            val shop = withContext(Dispatchers.IO) { runCatching {
                if (!Geocoder.isPresent()) null else {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale("en", "IN")).getFromLocationName("Surya co op HSG society, road boargad, Hari Har Colony, Mhasrul Gaon, Nashik, Maharashtra 422004, India", 1)?.firstOrNull()
                }
            }.getOrNull() }
            if (shop == null) { deliveryMessage = "दुकानाचे लोकेशन सापडले नाही. इंटरनेट सुरू करून पुन्हा प्रयत्न करा."; return@launch }
            val meters = FloatArray(1)
            Location.distanceBetween(shop.latitude, shop.longitude, location.latitude, location.longitude, meters)
            val km = meters[0] / 1000.0; deliveryDistanceKm = km
            val chargeableKm = kotlin.math.ceil(km).toInt().coerceAtLeast(1)
            deliveryCharge = chargeableKm * 10
            deliveryMessage = String.format(Locale.US, "अंतर %.2f km • डिलिव्हरी ₹%d (प्रति किमी ₹10)", km, deliveryCharge)
        }
    }
    fun fetchGpsForDelivery() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc -> if (loc != null) calculateDeliveryFromLocation(loc) else deliveryMessage = "लोकेशन मिळाले नाही. GPS सुरू करून पुन्हा प्रयत्न करा." }
            .addOnFailureListener { deliveryMessage = "लोकेशन मिळवता आले नाही. पुन्हा प्रयत्न करा." }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchGpsForDelivery() else deliveryMessage = "डिलिव्हरी चार्ज मोजण्यासाठी GPS परवानगी आवश्यक आहे."
    }

    fun requestDeliveryLocation() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        if (!enabled) { showGpsOffDialog = true; return }
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine || coarse) fetchGpsForDelivery() else locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(formState.deliveryAddress) {
        val typedAddress = formState.deliveryAddress.trim()
        if (typedAddress.length >= 5) {
            deliveryCharge = null
            deliveryDistanceKm = null
            deliveryMessage = "पत्ता तपासून डिलिव्हरी चार्ज मोजत आहे..."
            kotlinx.coroutines.delay(900)
            val destination = withContext(Dispatchers.IO) {
                runCatching {
                    if (!Geocoder.isPresent()) null else {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale("en", "IN"))
                            .getFromLocationName("$typedAddress, Nashik, Maharashtra, India", 1)
                            ?.firstOrNull()
                    }
                }.getOrNull()
            }
            if (destination == null) {
                deliveryMessage = "हा पत्ता सापडला नाही. पूर्ण पत्ता व Satpur, Nashik लिहा."
                deliveryLatitude = null
                deliveryLongitude = null
            } else {
                deliveryLatitude = destination.latitude
                deliveryLongitude = destination.longitude
                val shop = withContext(Dispatchers.IO) {
                    runCatching {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale("en", "IN")).getFromLocationName(
                            "Surya co op HSG society, Road Borgad, Hari Har Colony, Mhasrul Gaon, Nashik, Maharashtra 422004, India",
                            1
                        )?.firstOrNull()
                    }.getOrNull()
                }
                if (shop == null) {
                    deliveryMessage = "दुकानाचे लोकेशन सापडले नाही. कृपया पुन्हा प्रयत्न करा."
                    deliveryLatitude = null
                    deliveryLongitude = null
                } else {
                    val meters = FloatArray(1)
                    Location.distanceBetween(shop.latitude, shop.longitude, destination.latitude, destination.longitude, meters)
                    val km = meters[0] / 1000.0
                    deliveryDistanceKm = km
                    deliveryCharge = kotlin.math.ceil(km).toInt().coerceAtLeast(1) * 10
                    deliveryMessage = String.format(Locale.US, "पत्त्यापासून दुकानापर्यंत अंदाजे %.2f km • डिलिव्हरी ₹%d (प्रति किमी ₹10)", km, deliveryCharge)
                }
            }
        } else {
            deliveryCharge = null
            deliveryDistanceKm = null
            deliveryLatitude = null
            deliveryLongitude = null
            deliveryMessage = "पूर्ण डिलिव्हरी पत्ता लिहा; त्यानंतर चार्ज आपोआप मोजला जाईल."
        }
    }

    val itemsTotal = remember(cartItems) { cartItems.sumOf { it.lineTotalRupees } }
    val grandTotal = itemsTotal + (deliveryCharge ?: 0)

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier
            .fillMaxSize()
            .testTag("cart_screen_list")
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        androidx.compose.foundation.layout.Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(25.dp)
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.cart_header_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "तुमच्या आवडीचा ताजा शेतमाल",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (cartItems.isNotEmpty()) {
                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.testTag("clear_cart_button")
                    ) {
                        Text(
                            text = stringResource(R.string.cart_clear_all),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        if (cartItems.isEmpty()) {
            item {
                EmptyCartCard(onBrowseBazaar = onBackToBazaar)
            }
        } else {
            items(cartItems, key = { it.produce.id }) { line ->
                CartLineCard(
                    line = line,
                    onIncrease = { onIncreaseQty(line.produce.id) },
                    onDecrease = { onDecreaseQty(line.produce.id) },
                    onRemove = { onRemoveItem(line.produce.id) }
                )
            }

            item {
                BillSummaryCard(itemsTotal = itemsTotal, deliveryCharge = deliveryCharge, grandTotal = grandTotal)
            }
        }

        // Customer Name, Mobile Number & Place Order Section
        item {
            CustomerOrderFormCard(
                formState = formState,
                grandTotal = grandTotal,
                deliveryCharge = deliveryCharge,
                deliveryMessage = deliveryMessage,
                isCartEmpty = cartItems.isEmpty(),
                onCustomerNameChange = onCustomerNameChange,
                onMobileNumberChange = onMobileNumberChange,
                onAddressChange = onAddressChange,
                paymentMethod = paymentMethod,
                onPaymentMethodChange = { paymentMethod = it },
                transactionId = transactionId,
                onTransactionIdChange = { transactionId = it },
                onSubmitOrder = { if (deliveryLatitude != null && deliveryLongitude != null && deliveryCharge != null && deliveryDistanceKm != null) onSubmitOrder(deliveryLatitude, deliveryLongitude, deliveryCharge!!, deliveryDistanceKm, paymentMethod, transactionId.trim()) }
            )
        }

        if (trackingStatus.isNotBlank()) {
            item {
                val statusLabel = when (trackingStatus) {
                    "new" -> "नवीन ऑर्डर • दुकानाकडून पुष्टी बाकी"
                    "accepted" -> "ऑर्डर स्वीकारली • तयारी सुरू"
                    "completed" -> "ऑर्डर पूर्ण झाली"
                    "cancelled" -> "ऑर्डर रद्द झाली"
                    else -> "ऑर्डरची स्थिती सध्या मिळत नाही"
                }
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("live_order_tracking_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(42.dp)) {
                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ऑर्डर ट्रॅकिंग", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("तुमच्या ऑर्डरची सद्यस्थिती", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = if (trackingStatus == "cancelled") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (trackingStatus == "cancelled") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = when (trackingStatus) {
                                        "new" -> "नवीन"
                                        "accepted" -> "स्वीकारली"
                                        "completed" -> "पूर्ण"
                                        "cancelled" -> "रद्द"
                                        else -> "अपडेट"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Text(statusLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Admin कडून स्थिती बदलताच येथे अपडेट दिसेल.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (recentOrders.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.recent_orders_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(recentOrders, key = { it.orderId }) { order ->
                PastOrderCard(
                    order = order,
                    onShare = {
                        MarathiFormatters.shareOrderReceipt(
                            context = context,
                            receiptText = formatOrderShareReceipt(order)
                        )
                    },
                    onReorder = { onReorder(order) }
                )
            }
        }
    }

    if (showGpsOffDialog) {
        AlertDialog(
            onDismissRequest = { showGpsOffDialog = false },
            title = { Text("GPS Location बंद आहे") },
            text = { Text("डिलिव्हरी चार्ज मोजण्यासाठी कृपया GPS / Location सुरू करा.") },
            confirmButton = {
                TextButton(onClick = {
                    showGpsOffDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Location सुरू करा") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGpsOffDialog = false
                    deliveryMessage = "डिलिव्हरी चार्ज मोजण्यासाठी GPS आवश्यक आहे."
                }) { Text("ठीक आहे") }
            }
        )
    }

    if (confirmedOrder != null) {
        OrderConfirmedDialog(
            order = confirmedOrder,
            onShareReceipt = {
                MarathiFormatters.shareOrderReceipt(
                    context = context,
                    receiptText = formatOrderShareReceipt(confirmedOrder)
                )
            },
            onDismiss = onDismissReceipt,
            onContinueShopping = onContinueShopping
        )
    }
}

@Composable
private fun EmptyCartCard(onBrowseBazaar: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.cart_empty_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.cart_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onBrowseBazaar,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("browse_bazaar_button")
            ) {
                Icon(Icons.Default.Storefront, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.btn_browse_bazaar))
            }
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLineItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("cart_line_${line.produce.id}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (line.produce.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = line.produce.imageRes),
                        contentDescription = line.produce.marathiName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.medium)
                    )
                } else {
                    AsyncImage(
                        model = line.produce.imageUrl,
                        contentDescription = line.produce.marathiName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.medium)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = line.produce.marathiName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (line.produce.isPieceBased)
                            "₹${MarathiFormatters.toMarathiNumber(line.produce.priceRupees)} / ${line.produce.unitMarathi.removePrefix("१ ")} • ${MarathiFormatters.toMarathiNumber(line.quantity)} ${line.produce.unitMarathi.removePrefix("१ ")}"
                        else
                            "₹${MarathiFormatters.toMarathiNumber(line.produce.priceRupees)} / किलो • ${WeightPricing.formatWeightMarathi(line.weightGrams)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("cart_remove_${line.produce.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_remove_item),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("एकूण किंमत", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${MarathiFormatters.toMarathiNumber(line.lineTotalRupees)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDecrease,
                            modifier = Modifier.size(38.dp).testTag("cart_minus_${line.produce.id}")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.cd_decrease_qty), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = MarathiFormatters.toMarathiNumber(line.quantity),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = onIncrease,
                            modifier = Modifier.size(38.dp).testTag("cart_plus_${line.produce.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_increase_qty), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BillSummaryCard(itemsTotal: Int, deliveryCharge: Int?, grandTotal: Int) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("bill_summary_card")
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.bill_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "तुमच्या ऑर्डरचा तपशील",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bill_items_total), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹${MarathiFormatters.toMarathiNumber(itemsTotal)}", fontWeight = FontWeight.SemiBold)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bill_delivery_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = deliveryCharge?.let { "₹${MarathiFormatters.toMarathiNumber(it)}" } ?: "चार्ज मोजायचा आहे",
                    color = if (deliveryCharge == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.bill_grand_total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (deliveryCharge == null) "चार्ज मोजल्यावर" else "₹${MarathiFormatters.toMarathiNumber(grandTotal)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerOrderFormCard(
    formState: CustomerFormState,
    paymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    transactionId: String,
    onTransactionIdChange: (String) -> Unit,
    grandTotal: Int,
    deliveryCharge: Int?,
    deliveryMessage: String,
    isCartEmpty: Boolean,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_form_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(46.dp)) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = stringResource(R.string.customer_section_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(text = stringResource(R.string.customer_section_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (formState.cartErrorMarathi != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formState.cartErrorMarathi,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            OutlinedTextField(
                value = formState.customerName,
                onValueChange = onCustomerNameChange,
                singleLine = true,
                label = { Text(stringResource(R.string.label_customer_name)) },
                placeholder = { Text(stringResource(R.string.placeholder_customer_name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = formState.nameErrorMarathi != null,
                supportingText = formState.nameErrorMarathi?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("customer_name_input")
            )

            OutlinedTextField(
                value = formState.mobileNumber,
                onValueChange = onMobileNumberChange,
                singleLine = true,
                label = { Text(stringResource(R.string.label_mobile_number)) },
                placeholder = { Text(stringResource(R.string.placeholder_mobile_number)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                isError = formState.mobileErrorMarathi != null,
                supportingText = formState.mobileErrorMarathi?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mobile_number_input")
            )

            OutlinedTextField(
                value = formState.deliveryAddress,
                onValueChange = onAddressChange,
                maxLines = 2,
                label = { Text(stringResource(R.string.label_delivery_address)) },
                placeholder = { Text(stringResource(R.string.placeholder_delivery_address)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_address_input")
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (deliveryCharge == null) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("डिलिव्हरी माहिती", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        deliveryMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (deliveryCharge == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text("पेमेंट पद्धत", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paymentMethod == "COD", onClick = { onPaymentMethodChange("COD") })
                Text("Cash on Delivery")
                Spacer(modifier = Modifier.width(8.dp))
                RadioButton(selected = paymentMethod == "UPI", onClick = { onPaymentMethodChange("UPI") })
                Text("UPI")
            }
            if (paymentMethod == "UPI") {
                val context = LocalContext.current
                Text("UPI ID: paytm.s3wcxb1@pty", fontWeight = FontWeight.SemiBold)
                Text("पेमेंट केल्यानंतर Transaction ID / UTR भरा. Admin तपासल्यानंतरच Paid होईल.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    enabled = deliveryCharge != null,
                    onClick = {
                        val uri = Uri.parse("upi://pay?pa=paytm.s3wcxb1@pty&pn=Annapurna%20Vegetables%20and%20Fruits&am=$grandTotal&cu=INR")
                        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), "UPI पेमेंट करा")) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("UPI App उघडा") }
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = onTransactionIdChange,
                    singleLine = true,
                    label = { Text("Transaction ID / UTR") },
                    placeholder = { Text("पेमेंटनंतर UTR टाका") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val orderButtonLabel = if (!isCartEmpty && grandTotal > 0) {
                "${stringResource(R.string.btn_place_order)} • ₹${MarathiFormatters.toMarathiNumber(grandTotal)}"
            } else {
                stringResource(R.string.btn_place_order)
            }

            Button(
                onClick = onSubmitOrder,
                enabled = !isCartEmpty && !formState.isSubmitting && deliveryCharge != null,
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("place_order_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = orderButtonLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PastOrderCard(
    order: OrderEntity,
    onShare: () -> Unit,
    onReorder: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("past_order_card_${order.orderId}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(
                            R.string.order_id_format,
                            MarathiFormatters.toMarathiNumber(order.orderId.toInt())
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = MarathiFormatters.formatTimestampMarathi(order.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            text = stringResource(R.string.order_status_confirmed),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = order.customerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = MarathiFormatters.toMarathiDigits(order.mobileNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (order.deliveryAddress.isNotBlank()) {
                        Text(
                            text = order.deliveryAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = order.itemsSummaryMarathi,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("एकूण बिल", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.btn_share_receipt),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Button(
                        onClick = onReorder,
                        shape = MaterialTheme.shapes.medium,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.btn_reorder))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderConfirmedDialog(
    order: OrderEntity,
    onShareReceipt: () -> Unit,
    onDismiss: () -> Unit,
    onContinueShopping: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(68.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = stringResource(R.string.order_success_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.order_success_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.order_id_format,
                                MarathiFormatters.toMarathiNumber(order.orderId.toInt())
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("ग्राहक: ${order.customerName}", style = MaterialTheme.typography.bodySmall)
                        Text("मोबाईल: ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}", style = MaterialTheme.typography.bodySmall)
                        if (order.deliveryAddress.isNotBlank()) {
                            Text(
                                text = order.deliveryAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = order.itemsSummaryMarathi,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Text(
                            "एकूण बिल",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onShareReceipt,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.testTag("share_receipt_button")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_share_receipt))
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onContinueShopping,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().testTag("continue_shopping_after_order_button")
                ) {
                    Text("आणखी खरेदी करा")
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally).testTag("order_receipt_done_button")
                ) {
                    Text(stringResource(R.string.btn_done))
                }
            }
        }
    )
}

private fun formatOrderShareReceipt(order: OrderEntity): String {
    val addressLine = if (order.deliveryAddress.isNotBlank()) {
        "\nपत्ता: ${order.deliveryAddress}"
    } else ""
    return """
        🌿 अन्नपूर्णा भाजीपाला आणि फळे 🌿
        ऑर्डर पावती (#${MarathiFormatters.toMarathiNumber(order.orderId.toInt())})
        ---------------------------
        ग्राहक: ${order.customerName}
        मोबाईल: ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}$addressLine
        ---------------------------
        ${order.itemsSummaryMarathi}
        ---------------------------
        एकूण देय रक्कम: ₹${MarathiFormatters.toMarathiNumber(order.totalAmountRupees)}
        धन्यवाद! ताजा शेतमाल लवकरच घरपोच केला जाईल.
    """.trimIndent()
}
