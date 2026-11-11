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
import com.example.data.payment.RazorpayPaymentGateway
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
import kotlin.math.roundToLong
import org.json.JSONObject

@SuppressLint("MissingPermission")
@Composable
fun CartAndOrderScreen(
    cartItems: List<CartLineItem>,
    formState: CustomerFormState,
    recentOrders: List<OrderEntity>,
    confirmedOrder: OrderEntity?,
    trackingStatus: String,
    trackingLocation: String,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: (Double?, Double?, Int, Double?, String, String, Int) -> Unit,
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
    var deliveryMessage by rememberSaveable { mutableStateOf(context.getString(R.string.delivery_auto_calculating)) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var paymentMethod by rememberSaveable { mutableStateOf("COD") }
    var paymentInProgress by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var deliveryTipOption by rememberSaveable { mutableStateOf("0") }
    var customTipText by rememberSaveable { mutableStateOf("") }
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    fun calculateDeliveryFromLocation(location: Location) {
        deliveryLatitude = location.latitude; deliveryLongitude = location.longitude
        deliveryCharge = null; deliveryDistanceKm = null; deliveryMessage = context.getString(R.string.delivery_calculating_distance)
        coroutineScope.launch {
            val shop = withContext(Dispatchers.IO) { runCatching {
                if (!Geocoder.isPresent()) null else {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale("en", "IN")).getFromLocationName("Surya co op HSG society, road boargad, Hari Har Colony, Mhasrul Gaon, Nashik, Maharashtra 422004, India", 1)?.firstOrNull()
                }
            }.getOrNull() }
            if (shop == null) { deliveryMessage = context.getString(R.string.delivery_shop_location_missing); return@launch }
            val meters = FloatArray(1)
            Location.distanceBetween(shop.latitude, shop.longitude, location.latitude, location.longitude, meters)
            val km = meters[0] / 1000.0; deliveryDistanceKm = km
            val chargeableKm = kotlin.math.ceil(km).toInt().coerceAtLeast(1)
            deliveryCharge = chargeableKm * 10
            deliveryMessage = context.getString(R.string.delivery_charge_added)
        }
    }
    fun fetchGpsForDelivery() {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc -> if (loc != null) calculateDeliveryFromLocation(loc) else deliveryMessage = context.getString(R.string.delivery_location_missing) }
            .addOnFailureListener { deliveryMessage = context.getString(R.string.delivery_location_failed) }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) fetchGpsForDelivery() else deliveryMessage = context.getString(R.string.gps_enable_message)
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
            deliveryMessage = context.getString(R.string.delivery_address_checking)
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
                deliveryMessage = context.getString(R.string.delivery_address_not_found)
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
                    deliveryMessage = context.getString(R.string.delivery_shop_retry)
                    deliveryLatitude = null
                    deliveryLongitude = null
                } else {
                    val meters = FloatArray(1)
                    Location.distanceBetween(shop.latitude, shop.longitude, destination.latitude, destination.longitude, meters)
                    val km = meters[0] / 1000.0
                    deliveryDistanceKm = km
                    deliveryCharge = kotlin.math.ceil(km).toInt().coerceAtLeast(1) * 10
                    deliveryMessage = "डिलिव्हरी चार्ज बिलात आपोआप जोडला आहे."
                }
            }
        } else {
            deliveryCharge = null
            deliveryDistanceKm = null
            deliveryLatitude = null
            deliveryLongitude = null
            deliveryMessage = context.getString(R.string.delivery_enter_full_address)
        }
    }

    val itemsTotal = remember(cartItems) { cartItems.sumOf { it.lineTotalRupees } }
    val deliveryTipAmount = when (deliveryTipOption) {
        "20" -> 20
        "30" -> 30
        "50" -> 50
        "CUSTOM" -> customTipText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        else -> 0
    }
    val grandTotal = itemsTotal + (deliveryCharge ?: 0).toDouble() + deliveryTipAmount.toDouble()

    fun startRazorpayPayment() {
        val activity = context as? com.example.MainActivity
        if (activity == null) {
            paymentError = context.getString(R.string.payment_reopen_app)
            return
        }
        if (formState.customerName.trim().length < 2 ||
            !MarathiFormatters.isValidIndianMobile(formState.mobileNumber) ||
            formState.deliveryAddress.trim().length < 8 ||
            deliveryLatitude == null || deliveryLongitude == null ||
            deliveryCharge == null || deliveryDistanceKm == null
        ) {
            paymentError = context.getString(R.string.payment_fill_customer_details)
            return
        }
        paymentError = null
        paymentInProgress = true
        coroutineScope.launch {
            try {
                val amountPaise = (grandTotal * 100.0).roundToLong()
                val created = RazorpayPaymentGateway.createOrder(amountPaise)
                val options = JSONObject()
                    .put("key", created.keyId)
                    .put("order_id", created.orderId)
                    .put("amount", created.amountPaise)
                    .put("currency", created.currency)
                    .put("name", "Annapurna Vegetables and Fruits")
                    .put("description", "Fresh vegetables and fruits order")
                    .put("prefill", JSONObject()
                        .put("name", formState.customerName.trim())
                        .put("contact", MarathiFormatters.normalizePhoneDigits(formState.mobileNumber)))
                    .put("retry", JSONObject().put("enabled", true).put("max_count", 3))
                    .put("config", JSONObject().put("display", JSONObject().put("language", when (context.resources.configuration.locales[0].language) { "hi" -> "hi"; "en" -> "en"; else -> "mar" })))
                activity.startRazorpayCheckout(options) { paymentId, returnedOrderId, signature, error ->
                    if (error != null) {
                        paymentInProgress = false
                        paymentError = error
                    } else if (paymentId.isNullOrBlank() || signature.isNullOrBlank() || returnedOrderId != created.orderId) {
                        paymentInProgress = false
                        paymentError = context.getString(R.string.payment_details_invalid)
                    } else {
                        coroutineScope.launch {
                            try {
                                val verified = RazorpayPaymentGateway.verifyPayment(
                                    orderId = created.orderId,
                                    paymentId = paymentId,
                                    signature = signature,
                                    amountPaise = amountPaise
                                )
                                paymentInProgress = false
                                if (verified) {
                                    onSubmitOrder(
                                        deliveryLatitude, deliveryLongitude, deliveryCharge!!,
                                        deliveryDistanceKm, "RAZORPAY", paymentId, deliveryTipAmount
                                    )
                                } else {
                                    paymentError = context.getString(R.string.payment_server_verification_failed)
                                }
                            } catch (verificationError: Exception) {
                                paymentInProgress = false
                                paymentError = context.getString(R.string.payment_verification_failed)
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                paymentInProgress = false
                paymentError = error.message ?: context.getString(R.string.payment_start_failed)
            }
        }
    }

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
                            text = stringResource(R.string.cart_fresh_subtitle),
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
                BillSummaryCard(itemsTotal = itemsTotal, deliveryCharge = deliveryCharge, deliveryTip = deliveryTipAmount, grandTotal = grandTotal)
            }
        }

        // Let customers use GPS when address geocoding is inaccurate or unavailable.
        item {
            TextButton(
                onClick = { requestDeliveryLocation() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.use_current_location))
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
                tipOption = deliveryTipOption,
                onTipOptionChange = { deliveryTipOption = it },
                customTipText = customTipText,
                onCustomTipChange = { customTipText = it.filter(Char::isDigit).take(5) },
                paymentError = paymentError,
                paymentInProgress = paymentInProgress,
                onSubmitOrder = {
                    if (paymentMethod == "RAZORPAY") {
                        startRazorpayPayment()
                    } else if (deliveryLatitude != null && deliveryLongitude != null && deliveryCharge != null && deliveryDistanceKm != null) {
                        onSubmitOrder(deliveryLatitude, deliveryLongitude, deliveryCharge!!, deliveryDistanceKm, paymentMethod, "", deliveryTipAmount)
                    }
                }
            )
        }

        if (trackingStatus.isNotBlank()) {
            item {
                val statusLabel = when (trackingStatus) {
                    "new" -> stringResource(R.string.status_new_long)
                    "accepted" -> stringResource(R.string.status_accepted_long)
                    "preparing" -> stringResource(R.string.status_preparing_long)
                    "out_for_delivery" -> stringResource(R.string.status_out_long)
                    "completed" -> stringResource(R.string.status_completed_long)
                    "cancelled" -> stringResource(R.string.status_cancelled_long)
                    "unavailable" -> stringResource(R.string.status_unavailable_long)
                    else -> stringResource(R.string.status_updating_long)
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
                                Text(stringResource(R.string.order_tracking_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.order_tracking_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                color = if (trackingStatus == "cancelled") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                contentColor = if (trackingStatus == "cancelled") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = when (trackingStatus) {
                                        "new" -> stringResource(R.string.status_new_short)
                                        "accepted" -> stringResource(R.string.status_accepted_short)
                                        "preparing" -> stringResource(R.string.status_preparing_short)
                                        "out_for_delivery" -> stringResource(R.string.status_out_short)
                                        "completed" -> stringResource(R.string.status_completed_short)
                                        "cancelled" -> stringResource(R.string.status_cancelled_short)
                                        else -> stringResource(R.string.status_updating_short)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (trackingLocation.isNotBlank()) {
                            val parts = trackingLocation.split(",")
                            if (parts.size == 2) {
                                Text(stringResource(R.string.tracking_live_location_available), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                TextButton(onClick = {
                                    val uri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=${parts[0]},${parts[1]}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                }) { Text(stringResource(R.string.tracking_open_google_maps)) }
                            }
                        }
                        val progress = when (trackingStatus) {
                            "new" -> 0
                            "accepted" -> 1
                            "preparing" -> 2
                            "out_for_delivery" -> 3
                            "completed" -> 4
                            else -> -1
                        }
                        if (trackingStatus != "cancelled") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    stringResource(R.string.status_order_received),
                                    stringResource(R.string.status_order_accepted),
                                    stringResource(R.string.status_preparing),
                                    stringResource(R.string.status_out_for_delivery),
                                    stringResource(R.string.status_completed)
                                ).forEachIndexed { index, label ->
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = if (progress >= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                                if (progress >= index) {
                                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(19.dp))
                                                } else {
                                                    Text((index + 1).toString(), style = MaterialTheme.typography.labelMedium)
                                                }
                                            }
                                        }
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (progress >= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (progress >= index) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        } else {
                            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.status_cancelled_message), color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Text(statusLabel, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.status_admin_update), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text(stringResource(R.string.gps_location_off)) },
            text = { Text(stringResource(R.string.gps_enable_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showGpsOffDialog = false
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text(stringResource(R.string.gps_enable_action)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showGpsOffDialog = false
                    deliveryMessage = context.getString(R.string.gps_enable_message)
                }) { Text(stringResource(R.string.action_ok)) }
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
                            "₹${MarathiFormatters.formatRupees(line.produce.priceRupees)} / ${line.produce.unitMarathi.removePrefix("१ ")} • ${MarathiFormatters.toMarathiNumber(line.quantity)} ${line.produce.unitMarathi.removePrefix("१ ")}"
                        else
                            "₹${MarathiFormatters.formatRupees(line.produce.priceRupees)} / किलो • ${WeightPricing.formatWeightMarathi(line.weightGrams)}",
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
                    Text(stringResource(R.string.line_total_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${MarathiFormatters.formatRupees(line.lineTotalRupees)}",
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
private fun BillSummaryCard(itemsTotal: Double, deliveryCharge: Int?, deliveryTip: Int, grandTotal: Double) {
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
                        text = stringResource(R.string.order_details_title),
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
                Text("₹${MarathiFormatters.formatRupees(itemsTotal)}", fontWeight = FontWeight.SemiBold)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.bill_delivery_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = deliveryCharge?.let { "₹${MarathiFormatters.toMarathiNumber(it)}" } ?: stringResource(R.string.bill_address_calculating),
                    color = if (deliveryCharge == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (deliveryTip > 0) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.delivery_tip_title), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹${MarathiFormatters.toMarathiNumber(deliveryTip)}", fontWeight = FontWeight.SemiBold)
                }
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
                        text = if (deliveryCharge == null) stringResource(R.string.bill_waiting_charge) else "₹${MarathiFormatters.formatRupees(grandTotal)}",
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
    tipOption: String,
    onTipOptionChange: (String) -> Unit,
    customTipText: String,
    onCustomTipChange: (String) -> Unit,
    grandTotal: Double,
    deliveryCharge: Int?,
    deliveryMessage: String,
    paymentError: String?,
    paymentInProgress: Boolean,
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
                isError = formState.addressErrorMarathi != null,
                supportingText = formState.addressErrorMarathi?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
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

            Text(stringResource(R.string.delivery_tip_question), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.delivery_tip_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                listOf("0", "20", "30", "50").forEach { option ->
                    val label = if (option == "0") stringResource(R.string.tip_none) else "₹$option"
                    OutlinedButton(
                        onClick = { onTipOptionChange(option) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        colors = if (tipOption == option) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()
                    ) { Text(label, fontWeight = if (tipOption == option) FontWeight.Bold else FontWeight.Normal) }
                }
            }
            OutlinedButton(
                onClick = { onTipOptionChange("CUSTOM") },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                colors = if (tipOption == "CUSTOM") ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()
            ) { Text(stringResource(R.string.tip_custom_amount), fontWeight = if (tipOption == "CUSTOM") FontWeight.Bold else FontWeight.Normal) }
            if (tipOption == "CUSTOM") {
                OutlinedTextField(
                    value = customTipText,
                    onValueChange = onCustomTipChange,
                    singleLine = true,
                    label = { Text(stringResource(R.string.tip_amount_label)) },
                    placeholder = { Text(stringResource(R.string.tip_amount_example)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text(stringResource(R.string.payment_method_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paymentMethod == "COD", onClick = { onPaymentMethodChange("COD") })
                Text(stringResource(R.string.payment_cod))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paymentMethod == "RAZORPAY", onClick = { onPaymentMethodChange("RAZORPAY") })
                Text(stringResource(R.string.payment_razorpay))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = paymentMethod == "UPI", onClick = { onPaymentMethodChange("UPI") })
                Text(stringResource(R.string.payment_upi))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(stringResource(R.string.payment_card), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {}, enabled = false)
                Text(stringResource(R.string.payment_net_banking), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.coming_soon), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                if (paymentMethod == "RAZORPAY") stringResource(R.string.payment_secure_hint) else stringResource(R.string.payment_gateway_pending),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (paymentError != null) {
                Text(paymentError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (paymentMethod == "UPI") {
                val context = LocalContext.current
                Text("UPI ID: paytm.s3wcxb1@pty", fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.upi_payment_instruction),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    enabled = deliveryCharge != null,
                    onClick = {
                        val uri = Uri.parse("upi://pay?pa=paytm.s3wcxb1@pty&pn=Annapurna%20Vegetables%20and%20Fruits&am=$grandTotal&cu=INR")
                        runCatching { context.startActivity(Intent.createChooser(Intent(Intent.ACTION_VIEW, uri), context.getString(R.string.upi_open_title))) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.open_upi_app)) }
            }

            val orderButtonLabel = if (!isCartEmpty && grandTotal > 0) {
                "${stringResource(R.string.btn_place_order)} • ₹${MarathiFormatters.formatRupees(grandTotal)}"
            } else {
                stringResource(R.string.btn_place_order)
            }

            Button(
                onClick = onSubmitOrder,
                enabled = !isCartEmpty && !formState.isSubmitting && !paymentInProgress && deliveryCharge != null && (tipOption != "CUSTOM" || (customTipText.toIntOrNull() ?: -1) >= 0),
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
                    Text(stringResource(R.string.total_bill_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "₹${MarathiFormatters.formatRupees(order.totalAmountRupeesDecimal)}",
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
                        Text("${stringResource(R.string.customer_name_prefix)} ${order.customerName}", style = MaterialTheme.typography.bodySmall)
                        Text("${stringResource(R.string.mobile_prefix)} ${MarathiFormatters.toMarathiDigits(order.mobileNumber)}", style = MaterialTheme.typography.bodySmall)
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
                            stringResource(R.string.total_bill_label),
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
                    Text(stringResource(R.string.continue_shopping))
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
        एकूण देय रक्कम: ₹${MarathiFormatters.formatRupees(order.totalAmountRupeesDecimal)}
        धन्यवाद! ताजा शेतमाल लवकरच घरपोच केला जाईल.
    """.trimIndent()
}
