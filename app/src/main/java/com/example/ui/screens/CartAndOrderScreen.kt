package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.location.Location
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.tasks.Tasks
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.graphics.Color
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
import com.example.ui.components.OpenStreetMapView
import org.osmdroid.util.GeoPoint
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
    trackingUpdatedAtMillis: Long?,
    customerDestination: String,
    trackingPartnerAccepted: Boolean,
    trackingPartnerName: String,
    onIncreaseQty: (String) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearCart: () -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onMobileNumberChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onSubmitOrder: (Double?, Double?, Int, Double?, String, String, Int, String, String) -> Unit,
    onDismissReceipt: () -> Unit,
    onContinueShopping: () -> Unit,
    onReorder: (OrderEntity) -> Unit,
    onSubmitDeliveryReview: (Int, String, (Boolean) -> Unit) -> Unit,
    onBackToBazaar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToBazaar)

    val context = LocalContext.current
    var shopMapPosition by remember { mutableStateOf<GeoPoint?>(null) }
    LaunchedEffect(Unit) {
        shopMapPosition = withContext(Dispatchers.IO) {
            runCatching {
                val snapshot = Tasks.await(
                    FirebaseFirestore.getInstance().collection("shopSettings").document("main").get()
                )
                val latitude = snapshot.getDouble("latitude")
                val longitude = snapshot.getDouble("longitude")
                if (latitude != null && longitude != null &&
                    latitude.isFinite() && longitude.isFinite() &&
                    latitude in -90.0..90.0 && longitude in -180.0..180.0
                ) GeoPoint(latitude, longitude) else null
            }.getOrNull()
        }
    }
    var deliveryRating by rememberSaveable { mutableStateOf(5) }
    var deliveryReview by rememberSaveable { mutableStateOf("") }
    var reviewSent by rememberSaveable { mutableStateOf(false) }
    var reviewMessage by remember { mutableStateOf<String?>(null) }
    var showGpsOffDialog by remember { mutableStateOf(false) }
    var deliveryCharge by rememberSaveable { mutableStateOf<Int?>(null) }
    var deliveryDistanceKm by rememberSaveable { mutableStateOf<Double?>(null) }
    var deliveryLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var deliveryLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var gpsAddressSnapshot by rememberSaveable { mutableStateOf("") }
    var deliveryMessage by rememberSaveable { mutableStateOf(context.getString(R.string.delivery_auto_calculating)) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    var paymentMethod by rememberSaveable { mutableStateOf("COD") }
    var paymentInProgress by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var deliveryTipOption by rememberSaveable { mutableStateOf("0") }
    var customTipText by rememberSaveable { mutableStateOf("") }
    val fusedLocationClient = remember(context) { LocationServices.getFusedLocationProviderClient(context) }

    fun calculateDeliveryFromLocation(location: Location) {
        // Preserve the phone's actual GPS coordinates for this checkout address.
        // Short localities such as Satpur must not replace this with a guessed pin.
        gpsAddressSnapshot = formState.deliveryAddress.trim()
        deliveryLatitude = location.latitude; deliveryLongitude = location.longitude
        deliveryCharge = null; deliveryDistanceKm = null; deliveryMessage = context.getString(R.string.delivery_calculating_distance)
        coroutineScope.launch {
            val shop = withContext(Dispatchers.IO) { runCatching {
                val snapshot = Tasks.await(
                    FirebaseFirestore.getInstance().collection("shopSettings").document("main").get()
                )
                val latitude = snapshot.getDouble("latitude")
                val longitude = snapshot.getDouble("longitude")
                if (latitude != null && longitude != null &&
                    latitude.isFinite() && longitude.isFinite() &&
                    latitude in -90.0..90.0 && longitude in -180.0..180.0
                ) Location("annapurna_shop").apply {
                    this.latitude = latitude
                    this.longitude = longitude
                } else null
            }.getOrNull() }
            if (shop == null) { deliveryMessage = context.getString(R.string.delivery_shop_location_missing); return@launch }
            try {
                val fare = com.example.data.model.DeliveryFareCalculator.calculate(
                    shop.latitude, shop.longitude, location.latitude, location.longitude
                )
                deliveryDistanceKm = fare.distanceKm
                deliveryCharge = fare.chargeRupees
                val distanceLabel = MarathiFormatters.toMarathiDigits(String.format(Locale.US, "%.1f", fare.distanceKm))
                deliveryMessage = context.getString(R.string.delivery_charge_added_format, distanceLabel)
            } catch (error: Exception) {
                deliveryDistanceKm = null
                deliveryCharge = null
                deliveryMessage = error.message ?: context.getString(R.string.delivery_location_failed)
            }
        }
    }
    fun clearCustomerGps(message: String) {
        deliveryLatitude = null
        deliveryLongitude = null
        deliveryCharge = null
        deliveryDistanceKm = null
        gpsAddressSnapshot = ""
        deliveryMessage = message
    }

    fun fetchGpsForDelivery() {
        clearCustomerGps(context.getString(R.string.delivery_auto_calculating))
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) calculateDeliveryFromLocation(loc)
                else clearCustomerGps(context.getString(R.string.delivery_location_missing))
            }
            .addOnFailureListener { clearCustomerGps(context.getString(R.string.delivery_location_failed)) }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) fetchGpsForDelivery()
        else clearCustomerGps("अचूक GPS परवानगी आवश्यक आहे. परवानगी न दिल्यास ऑर्डर देता येणार नाही.")
    }

    fun requestDeliveryLocation() {
        clearCustomerGps("ग्राहकाचे अचूक GPS Location मिळवत आहोत…")
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        if (!enabled) { showGpsOffDialog = true; return }
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fine) fetchGpsForDelivery()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Order placement requires a fresh, precise device GPS fix. A typed address
    // is descriptive only and is never converted into substitute coordinates.
    LaunchedEffect(Unit) {
        requestDeliveryLocation()
    }

    val itemsTotal = remember(cartItems) { cartItems.sumOf { it.lineTotalRupees } }
    val deliveryTipAmount = when (deliveryTipOption) {
        "20" -> 20
        "30" -> 30
        "50" -> 50
        "CUSTOM" -> customTipText.toIntOrNull()?.coerceAtLeast(0) ?: 0
        else -> 0
    }
    // Orders above ₹199 get free delivery; otherwise charge ₹10 per started road-kilometre.
    val payableDeliveryCharge = if (itemsTotal > 199.0) 0 else deliveryCharge
    val grandTotal = itemsTotal + (payableDeliveryCharge ?: 0).toDouble() + deliveryTipAmount.toDouble()

    fun startRazorpayPayment() {
        val activity = context as? com.example.MainActivity
        if (activity == null) {
            paymentError = context.getString(R.string.payment_reopen_app)
            return
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            deliveryLatitude == null || deliveryLongitude == null ||
            formState.customerName.trim().length < 2 ||
            !MarathiFormatters.isValidIndianMobile(formState.mobileNumber) ||
            formState.deliveryAddress.trim().length < 8 ||
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
                    // Razorpay Standard Checkout supports English in its documented display-language option. Omit unsupported locale codes.
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
                                // Submit once to the trusted Worker. /submit-paid-order verifies the
                                // Razorpay signature, confirms/captures the payment, and saves the
                                // order + tracking record together. A separate /verify-payment call
                                // could capture money and then leave no order if the app closes
                                // before the second request.
                                onSubmitOrder(
                                    deliveryLatitude, deliveryLongitude, payableDeliveryCharge!!,
                                    deliveryDistanceKm, paymentMethod, paymentId, deliveryTipAmount, created.orderId, signature
                                )
                                paymentInProgress = false
                            } catch (submissionError: Exception) {
                                paymentInProgress = false
                                val detail = submissionError.message?.takeIf { it.isNotBlank() }
                                paymentError = if (detail != null) {
                                    context.getString(R.string.payment_verification_failed) + "\n" + detail
                                } else {
                                    context.getString(R.string.payment_verification_failed)
                                }
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
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
                        modifier = Modifier.size(56.dp)
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
            items(cartItems, key = { it.cartLineId }) { line ->
                CartLineCard(
                    line = line,
                    onIncrease = { onIncreaseQty(line.cartLineId) },
                    onDecrease = { onDecreaseQty(line.cartLineId) },
                    onRemove = { onRemoveItem(line.cartLineId) }
                )
            }

            item {
                BillSummaryCard(itemsTotal = itemsTotal, deliveryCharge = payableDeliveryCharge, deliveryTip = deliveryTipAmount, grandTotal = grandTotal)
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
                deliveryCharge = payableDeliveryCharge,
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
                    val preciseGpsGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    if (!preciseGpsGranted) {
                        clearCustomerGps("अचूक GPS परवानगी द्या. GPS शिवाय ऑर्डर स्वीकारली जाणार नाही.")
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else if (deliveryLatitude == null || deliveryLongitude == null || deliveryCharge == null || deliveryDistanceKm == null) {
                        requestDeliveryLocation()
                    } else if (paymentMethod != "COD") {
                        startRazorpayPayment()
                    } else {
                        onSubmitOrder(deliveryLatitude, deliveryLongitude, payableDeliveryCharge!!, deliveryDistanceKm, paymentMethod, "", deliveryTipAmount, "", "")
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
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                        if (trackingLocation.isBlank()) {
                            val shopPosition = shopMapPosition
                            if (shopPosition != null) {
                                Text(
                                    if (trackingPartnerAccepted)
                                        "🛵 ${trackingPartnerName.ifBlank { "Delivery Partner" }} " + customerText("दुकानातून Pickup करण्याच्या तयारीत आहे.", "दुकान से Pickup करने की तैयारी में है।", "is getting ready to pick up your order.")
                                    else
                                        customerText("🛵 तुमची ऑर्डर दुकानात तयार होत आहे. गाडी दुकानाजवळ आहे.", "🛵 आपका ऑर्डर दुकान में तैयार हो रहा है। वाहन दुकान के पास है।", "🛵 Your order is being prepared at the shop. The vehicle is nearby."),
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OpenStreetMapView(
                                    latitude = shopPosition.latitude,
                                    longitude = shopPosition.longitude,
                                    markerTitle = "Annapurna Shop",
                                    markerSnippet = "Shop location",
                                    modifier = Modifier.fillMaxWidth().height(220.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .testTag("customer_live_delivery_map")
                                )
                                Text(
                                    if (trackingPartnerAccepted)
                                        customerText("Pickup झाल्यावर गाडीचे Live Location येथे दिसेल.", "Pickup होने के बाद वाहन की Live Location यहाँ दिखेगी।", "The vehicle's live location will appear here after pickup.")
                                    else
                                        customerText("Delivery Partner ऑर्डर Pickup करेपर्यंत गाडी दुकानातच दिसेल.", "Delivery Partner के ऑर्डर Pickup करने तक वाहन दुकान पर ही दिखेगा।", "The vehicle will remain shown at the shop until the delivery partner picks up the order."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    customerText("दुकानाचे नकाशावरील Location मिळवत आहोत...", "दुकान का नक्शे वाला Location प्राप्त कर रहे हैं...", "Getting the shop's map location..."),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (trackingLocation.isNotBlank()) {
                            val parts = trackingLocation.split(",")
                            if (parts.size == 2) {
                                val driverLatitude = parts[0].toDoubleOrNull()
                                val driverLongitude = parts[1].toDoubleOrNull()
                                if (driverLatitude != null && driverLongitude != null &&
                                    driverLatitude in -90.0..90.0 && driverLongitude in -180.0..180.0
                                ) {
                                    Surface(
                                        color = Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(18.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFF075B32),
                                                shape = RoundedCornerShape(50),
                                                modifier = Modifier.size(54.dp)
                                            ) {
                                                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                                    Text("🛵", style = MaterialTheme.typography.headlineMedium)
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    trackingPartnerName.ifBlank { customerText("डिलिव्हरी पार्टनर", "डिलीवरी पार्टनर", "Delivery partner") },
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF075B32),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    customerText("Annapurna गणवेशात तुमची ऑर्डर घेऊन येत आहेत", "Annapurna यूनिफॉर्म में आपका ऑर्डर ला रहे हैं", "Your order is on the way in Annapurna uniform"),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color(0xFF315B40)
                                                )
                                            }
                                            Surface(color = Color(0xFF159447), shape = RoundedCornerShape(50)) {
                                                Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                            }
                                        }
                                    }
                                    Text(
                                        text = stringResource(R.string.tracking_live_location_available),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val destinationParts = customerDestination.split(",")
                                    val customerLat = destinationParts.getOrNull(0)?.toDoubleOrNull()
                                    val customerLon = destinationParts.getOrNull(1)?.toDoubleOrNull()
                                    OpenStreetMapView(
                                        latitude = driverLatitude,
                                        longitude = driverLongitude,
                                        markerTitle = trackingPartnerName.ifBlank { "Delivery Partner" },
                                        markerSnippet = "Live location",
                                        destinationLatitude = customerLat,
                                        destinationLongitude = customerLon,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(420.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .testTag("customer_live_delivery_map")
                                    )
                                    trackingUpdatedAtMillis?.let { updatedAt ->
                                        val updatedTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(updatedAt))
                                        Text("${customerText("शेवटचे लोकेशन अपडेट", "आखिरी लोकेशन अपडेट", "Last location update")}: $updatedTime", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (trackingPartnerAccepted && trackingLocation.isBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        "🛵 Delivery Partner: ${trackingPartnerName.ifBlank { "नाव मिळवत आहोत" }}",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Delivery Partner ने ऑर्डर स्वीकारली आहे",
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Live Location मिळवत आहोत. GPS मिळताच नकाशावर स्कूटर आपोआप दिसेल.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
                        val reviewThanksText = customerText("धन्यवाद! तुमचा अभिप्राय नोंदवला.", "धन्यवाद! आपकी प्रतिक्रिया दर्ज की गई।", "Thank you! Your feedback has been recorded.")
                        val reviewFailedText = customerText("अभिप्राय पाठवता आला नाही. पुन्हा प्रयत्न करा.", "प्रतिक्रिया भेजी नहीं जा सकी। फिर कोशिश करें।", "Could not send feedback. Please try again.")
                        val reviewSubmitText = customerText("रेटिंग आणि अभिप्राय पाठवा", "रेटिंग और समीक्षा भेजें", "Submit rating and review")
                        val reviewInputText = customerText("तुमचा अनुभव लिहा (ऐच्छिक)", "अपना अनुभव लिखें (वैकल्पिक)", "Write your experience (optional)")
                        if (trackingStatus == "completed") {
                            HorizontalDivider()
                            Text(customerText("डिलिव्हरी पार्टनरला रेटिंग द्या", "डिलीवरी पार्टनर को रेटिंग दें", "Rate your delivery partner"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (!reviewSent) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    (1..5).forEach { star ->
                                        TextButton(onClick = { deliveryRating = star }) {
                                            Text(if (star <= deliveryRating) "★" else "☆", color = if (star <= deliveryRating) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.headlineSmall)
                                        }
                                    }
                                }
                                OutlinedTextField(value = deliveryReview, onValueChange = { deliveryReview = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text(reviewInputText) }, minLines = 2, maxLines = 4)
                                Button(onClick = { onSubmitDeliveryReview(deliveryRating, deliveryReview) { success -> reviewSent = success; reviewMessage = if (success) reviewThanksText else reviewFailedText } }, modifier = Modifier.fillMaxWidth()) { Text(reviewSubmitText) }
                            } else {
                                Text(reviewMessage ?: reviewThanksText, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                            reviewMessage?.takeIf { !reviewSent }?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().testTag("cart_line_${line.cartLineId}")
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (line.produce.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = line.produce.imageRes),
                        contentDescription = line.produce.displayName(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp).clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    AsyncImage(
                        model = line.produce.imageUrl,
                        contentDescription = line.produce.displayName(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.medium)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = line.produce.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (line.produce.isPieceBased)
                            "₹${MarathiFormatters.formatRupees(line.produce.priceRupees)} / ${line.produce.displayUnit().removePrefix("१ ")} • ${MarathiFormatters.toMarathiNumber(line.quantity)} ${line.produce.displayUnit().removePrefix("१ ")}"
                        else
                            "₹${MarathiFormatters.formatRupees(line.produce.priceRupees)} / किलो • ${WeightPricing.formatWeightMarathi(line.weightGrams)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("cart_remove_${line.cartLineId}")
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
                            modifier = Modifier.size(38.dp).testTag("cart_minus_${line.cartLineId}")
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
                            modifier = Modifier.size(38.dp).testTag("cart_plus_${line.cartLineId}")
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("customer_order_form_card")
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(22.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(52.dp)) {
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
            // Standard Razorpay Checkout presents supported UPI apps (including PhonePe/Paytm when available on the device), cards and net banking inside its own payment screen. Do not imply that selecting a separate local radio option launches a specific PSP app.
            val paymentOptions = listOf(
                "COD" to "Cash on Delivery",
                "RAZORPAY" to "Online Payment (UPI / PhonePe / Paytm / Cards / Net Banking)"
            )
            paymentOptions.forEach { (code, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = paymentMethod == code,
                        onClick = { onPaymentMethodChange(code) }
                    )
                    Text(label)
                }
            }
            Text(
                if (paymentMethod == "COD") stringResource(R.string.payment_cod_hint)
                else stringResource(R.string.payment_secure_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (paymentError != null) {
                Text(paymentError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
    val celebration = rememberInfiniteTransition(label = "order-celebration")
    val sparkleScale by celebration.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
        label = "sparkle-scale"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        icon = {
            Text(
                text = "🎉 ✨ 🎊 🌟 🎆",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.graphicsLayer(scaleX = sparkleScale, scaleY = sparkleScale)
            )
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
                    text = customerText("ऑर्डर यशस्वीरीत्या नोंदवली! धन्यवाद! 🛍️", "ऑर्डर सफलतापूर्वक दर्ज हुआ! धन्यवाद! 🛍️", "Order Placed! Thanks for Shopping! 🛍️"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
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
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
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

@Composable
private fun customerText(mr: String, hi: String, en: String): String {
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
    return when (language) { "hi" -> hi; "en" -> en; else -> mr }
}
