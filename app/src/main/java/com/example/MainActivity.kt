package com.example

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.appCheck
import com.google.firebase.initialize
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ProduceCategory
import com.example.ui.screens.BazaarHomeScreen
import com.example.ui.screens.CartAndOrderScreen
import com.example.ui.screens.MarathiChatScreen
import com.example.ui.screens.CustomerProfileScreen
import com.example.ui.screens.CustomerOrdersScreen
import com.example.ui.screens.LiveOffersScreen
import com.example.ui.screens.NewArrivalsScreen
import com.example.ui.theme.AnnapurnaTheme
import com.example.ui.viewmodel.AiFeaturesViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.MarathiFormatters
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import org.json.JSONObject

enum class AnnapurnaTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    BAZAAR(
        labelRes = R.string.nav_bazaar,
        selectedIcon = Icons.Filled.Storefront,
        unselectedIcon = Icons.Outlined.Storefront,
        tag = "nav_tab_bazaar"
    ),
    NEW_ARRIVALS(
        labelRes = R.string.nav_new_arrivals,
        selectedIcon = Icons.Filled.Eco,
        unselectedIcon = Icons.Filled.Eco,
        tag = "nav_tab_new_arrivals"
    ),
    OFFERS(
        labelRes = R.string.nav_offers,
        selectedIcon = Icons.Filled.Eco,
        unselectedIcon = Icons.Filled.Eco,
        tag = "nav_tab_offers"
    ),
    CATEGORIES(
        labelRes = R.string.nav_categories,
        selectedIcon = Icons.Filled.Category,
        unselectedIcon = Icons.Outlined.Category,
        tag = "nav_tab_categories"
    ),
    CART(
        labelRes = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        tag = "nav_tab_cart"
    ),
    ORDERS(
        labelRes = R.string.nav_orders,
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong,
        tag = "nav_tab_orders"
    ),
    ASSISTANT(
        labelRes = R.string.nav_assistant,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        tag = "nav_tab_assistant"
    ),
    PROFILE(
        labelRes = R.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        tag = "nav_tab_profile"
    )
}

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private var paymentResultCallback: ((String?, String?, String?, String?) -> Unit)? = null

    fun startRazorpayCheckout(
        options: JSONObject,
        callback: (String?, String?, String?, String?) -> Unit
    ) {
        paymentResultCallback = callback
        try {
            val checkout = Checkout()
            // Set the server-provided public Key ID explicitly before opening checkout.
            checkout.setKeyID(options.getString("key"))
            checkout.open(this, options)
        } catch (error: Exception) {
            paymentResultCallback = null
            callback(null, null, null, error.message ?: "पेमेंट सुरू करता आले नाही.")
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        val callback = paymentResultCallback
        paymentResultCallback = null
        callback?.invoke(razorpayPaymentId, paymentData?.orderId, paymentData?.signature, null)
    }

    override fun onPaymentError(errorCode: Int, response: String?, paymentData: PaymentData?) {
        val callback = paymentResultCallback
        paymentResultCallback = null
        callback?.invoke(null, null, null, "Razorpay error code: $errorCode. " + (response ?: "पेमेंट रद्द झाले किंवा अयशस्वी झाले."))
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val language = newBase.getSharedPreferences("annapurna_settings", MODE_PRIVATE)
            .getString("app_language", "mr") ?: "mr"
        val locale = java.util.Locale(language)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    fun changeLanguage(language: String) {
        getSharedPreferences("annapurna_settings", MODE_PRIVATE).edit()
            .putString("app_language", language).apply()
        recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Firebase App Check before any Firebase AI Logic requests.
        Firebase.initialize(context = this)
        Firebase.appCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )
        // Preload Razorpay checkout resources before the customer starts payment.
        Checkout.preload(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnnapurnaTheme {
                CustomerStartupPermissions {
                    AnnapurnaRootApp()
                }
            }
        }
    }
}


/** Requests the permissions used by the customer app on its first launch. */
@Composable
private fun CustomerStartupPermissions(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("annapurna_startup_permissions", android.content.Context.MODE_PRIVATE)
    }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // GPS is requested at startup; order submission independently requires precise GPS permission.
        preferences.edit().putBoolean("requested_once", true).apply()
    }

    LaunchedEffect(Unit) {
        if (!preferences.getBoolean("requested_once", false)) {
            val requested = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= 33) {
                requested += Manifest.permission.POST_NOTIFICATIONS
            }
            val missing = requested.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isEmpty()) {
                preferences.edit().putBoolean("requested_once", true).apply()
            } else {
                launcher.launch(missing.toTypedArray())
            }
        }
    }

    content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnapurnaRootApp() {
    val context = LocalContext.current
    val storeViewModel: StoreViewModel = viewModel(
        factory = StoreViewModel.provideFactory(context)
    )
    val aiViewModel: AiFeaturesViewModel = viewModel()

    LaunchedEffect(Unit) {
        val entryPrefs = context.getSharedPreferences("annapurna_customer_entry", android.content.Context.MODE_PRIVATE)
        entryPrefs.getString("customer_mobile", null)?.let(storeViewModel::updateMobileNumber)
    }

    var currentTab by rememberSaveable { mutableStateOf(AnnapurnaTab.BAZAAR) }
    var assistantStartAction by rememberSaveable { mutableStateOf<String?>(null) }

    val produceItems by storeViewModel.filteredProduce.collectAsStateWithLifecycle()
    val allProducts by storeViewModel.allProducts.collectAsStateWithLifecycle()
    val selectedCategory by storeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val cartItems by storeViewModel.cartItems.collectAsStateWithLifecycle()
    val selectedDetailItem by storeViewModel.selectedProduceDetail.collectAsStateWithLifecycle()
    val formState by storeViewModel.formState.collectAsStateWithLifecycle()
    val recentOrders by storeViewModel.recentOrders.collectAsStateWithLifecycle()
    val confirmedOrder by storeViewModel.confirmedOrder.collectAsStateWithLifecycle()
    val trackingStatus by storeViewModel.trackingStatus.collectAsStateWithLifecycle()
    val trackingLocation by storeViewModel.trackingLocation.collectAsStateWithLifecycle()
    val trackingUpdatedAtMillis by storeViewModel.trackingUpdatedAtMillis.collectAsStateWithLifecycle()
    val customerDestination by storeViewModel.customerDestination.collectAsStateWithLifecycle()
    val trackingPartnerAccepted by storeViewModel.trackingPartnerAccepted.collectAsStateWithLifecycle()
    val trackingPartnerName by storeViewModel.trackingPartnerName.collectAsStateWithLifecycle()

    val chatState by aiViewModel.chatState.collectAsStateWithLifecycle()

    val totalCartItems = remember(cartItems) { cartItems.sumOf { it.quantity } }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    var showMoreRequestDialog by rememberSaveable { mutableStateOf(false) }
    var moreRequestName by rememberSaveable { mutableStateOf("") }
    var moreRequestQuantity by rememberSaveable { mutableStateOf("") }
    var moreRequestNote by rememberSaveable { mutableStateOf("") }
    var moreRequestSending by remember { mutableStateOf(false) }
    var moreRequestFeedback by remember { mutableStateOf<String?>(null) }
    val currentLanguage = context.resources.configuration.locales[0].language
    val languageLabel = when (currentLanguage) { "hi" -> "हिन्दी"; "en" -> "English"; else -> "मराठी" }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (currentTab != AnnapurnaTab.BAZAAR) {
                    TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_launcher_logo),
                                    contentDescription = stringResource(R.string.brand_name),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.store_title_marathi),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.store_subtitle_marathi),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.86f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    actions = {
                        Box {
                            FilledTonalButton(
                                onClick = { languageMenuExpanded = true },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.padding(end = 4.dp)
                            ) { Text(languageLabel, fontWeight = FontWeight.SemiBold) }
                            DropdownMenu(
                                expanded = languageMenuExpanded,
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                listOf("mr" to "मराठी", "hi" to "हिन्दी", "en" to "English").forEach { (code, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            languageMenuExpanded = false
                                            (context as? MainActivity)?.changeLanguage(code)
                                        }
                                    )
                                }
                            }
                        }
                        if (currentTab != AnnapurnaTab.CART) {
                            FilledTonalButton(
                                onClick = { currentTab = AnnapurnaTab.CART },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.18f),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .testTag("topbar_cart_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBag,
                                    contentDescription = stringResource(R.string.nav_cart),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = MarathiFormatters.toMarathiNumber(totalCartItems),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                    )
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 8.dp
                    ) {
                        AnnapurnaTab.entries.filter { it != AnnapurnaTab.ASSISTANT && it != AnnapurnaTab.OFFERS && it != AnnapurnaTab.NEW_ARRIVALS }.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithCartBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = totalCartItems
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AnnapurnaTab.entries.filter { it != AnnapurnaTab.ASSISTANT }.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithCartBadge(
                                        tab = tab,
                                        selected = selected,
                                        cartCount = totalCartItems
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                ) {
                    val contentModifier = Modifier
                        .widthIn(max = 960.dp)
                        .fillMaxSize()

                    when (currentTab) {
                        AnnapurnaTab.NEW_ARRIVALS -> {
                            NewArrivalsScreen(products = allProducts, onAddToCart = { id, grams -> storeViewModel.addToCart(id, grams) }, onBack = { currentTab = AnnapurnaTab.BAZAAR }, modifier = contentModifier)
                        }
                        AnnapurnaTab.OFFERS -> {
                            LiveOffersScreen(onBack = { currentTab = AnnapurnaTab.BAZAAR }, modifier = contentModifier)
                        }
                        AnnapurnaTab.CATEGORIES -> {
                            data class CategoryTile(
                                val emoji: String,
                                val mr: String,
                                val hi: String,
                                val en: String,
                                val category: ProduceCategory?
                            )
                            val categoryTiles = listOf(
                                CategoryTile("🥬", "भाज्या", "सब्ज़ियाँ", "Vegetables", ProduceCategory.VEGETABLE),
                                CategoryTile("🍎", "फळे", "फल", "Fruits", ProduceCategory.FRUIT),
                                CategoryTile("🌿", "पालेभाज्या", "हरी पत्तेदार", "Leafy Greens", ProduceCategory.LEAFY_GREENS),
                                CategoryTile("🎁", "ऑफर्स", "ऑफ़र", "Offers", ProduceCategory.ALL),
                                CategoryTile("🌱", "सेंद्रिय", "ऑर्गेनिक", "Organic", ProduceCategory.ORGANIC),
                                CategoryTile("🧺", "दैनंदिन गरजा", "दैनिक ज़रूरतें", "Daily Needs", ProduceCategory.DAILY_NEEDS),
                                CategoryTile("📦", "कॉम्बो पॅक्स", "कॉम्बो पैक", "Combo Packs", ProduceCategory.COMBO_PACKS),
                                CategoryTile("＋", "अधिक", "और", "More", null)
                            )
                            Column(
                                modifier = contentModifier.padding(16.dp),
                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    color = Color(0xFFE8F5E9),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Text(
                                            when (currentLanguage) { "hi" -> "ताज़ा और चुनिंदा"; "en" -> "Fresh & Handpicked"; else -> "ताजे आणि निवडक" },
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF12652F)
                                        )
                                        Text(
                                            when (currentLanguage) { "hi" -> "अपनी पसंद की श्रेणी चुनें"; "en" -> "Choose a category you love"; else -> "तुमच्या आवडीची श्रेणी निवडा" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF52745B)
                                        )
                                    }
                                }
                                Text(
                                    when (currentLanguage) { "hi" -> "सभी श्रेणियाँ"; "en" -> "All Categories"; else -> "सर्व कॅटेगरी" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                                    columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                                    userScrollEnabled = false,
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(9.dp),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(9.dp),
                                    modifier = Modifier.height(190.dp)
                                ) {
                                    items(categoryTiles.size) { index ->
                                        val tile = categoryTiles[index]
                                        val label = when (currentLanguage) {
                                            "hi" -> tile.hi
                                            "en" -> tile.en
                                            else -> tile.mr
                                        }
                                        Surface(
                                            onClick = {
                                                when {
                                                    tile.en == "Offers" -> currentTab = AnnapurnaTab.OFFERS
                                                    tile.category == null -> showMoreRequestDialog = true
                                                    else -> {
                                                        storeViewModel.selectCategory(tile.category)
                                                        currentTab = AnnapurnaTab.BAZAAR
                                                    }
                                                }
                                            },
                                            color = Color(0xFFF3F8F2),
                                            shape = RoundedCornerShape(15.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE1ECE1)),
                                            modifier = Modifier.fillMaxWidth().height(88.dp)
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                                            ) {
                                                Text(tile.emoji, style = MaterialTheme.typography.titleLarge)
                                                Text(
                                                    label,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF12652F),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 2,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                FilledTonalButton(
                                    onClick = {
                                        storeViewModel.selectCategory(ProduceCategory.ALL)
                                        currentTab = AnnapurnaTab.BAZAAR
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(when (currentLanguage) { "hi" -> "🛍️  सभी उत्पाद देखें"; "en" -> "🛍️  Browse all products"; else -> "🛍️  सर्व उत्पादने पहा" })
                                }
                            }
                            if (showMoreRequestDialog) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { if (!moreRequestSending) showMoreRequestDialog = false },
                                    title = { Text(when (currentLanguage) { "hi" -> "नया उत्पाद मँगवाएँ"; "en" -> "Request an item"; else -> "नवीन वस्तू मागवा" }) },
                                    text = {
                                        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                                            androidx.compose.material3.OutlinedTextField(
                                                value = moreRequestName,
                                                onValueChange = { moreRequestName = it },
                                                label = { Text(when (currentLanguage) { "hi" -> "उत्पाद का नाम"; "en" -> "Item name"; else -> "वस्तूचे नाव" }) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            androidx.compose.material3.OutlinedTextField(
                                                value = moreRequestQuantity,
                                                onValueChange = { moreRequestQuantity = it },
                                                label = { Text(when (currentLanguage) { "hi" -> "मात्रा"; "en" -> "Quantity"; else -> "प्रमाण" }) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            androidx.compose.material3.OutlinedTextField(
                                                value = moreRequestNote,
                                                onValueChange = { moreRequestNote = it },
                                                label = { Text(when (currentLanguage) { "hi" -> "अतिरिक्त जानकारी (वैकल्पिक)"; "en" -> "Extra details (optional)"; else -> "अधिक माहिती (ऐच्छिक)" }) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            moreRequestFeedback?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                        }
                                    },
                                    confirmButton = {
                                        FilledTonalButton(
                                            enabled = moreRequestName.isNotBlank() && !moreRequestSending,
                                            onClick = {
                                                moreRequestSending = true
                                                val payload = hashMapOf<String, Any>(
                                                    "productName" to moreRequestName.trim(),
                                                    "quantity" to moreRequestQuantity.trim(),
                                                    "note" to moreRequestNote.trim(),
                                                    "status" to "pending",
                                                    "source" to "customer_app_categories",
                                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                    "createdAtMillis" to System.currentTimeMillis()
                                                )
                                                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                    .collection("productRequests").add(payload)
                                                    .addOnSuccessListener {
                                                        moreRequestSending = false
                                                        showMoreRequestDialog = false
                                                        moreRequestName = ""
                                                        moreRequestQuantity = ""
                                                        moreRequestNote = ""
                                                        moreRequestFeedback = null
                                                    }
                                                    .addOnFailureListener {
                                                        moreRequestSending = false
                                                        moreRequestFeedback = when (currentLanguage) {
                                                            "hi" -> "अनुरोध नहीं भेजा गया। फिर कोशिश करें।"
                                                            "en" -> "Request failed. Please try again."
                                                            else -> "विनंती पाठवता आली नाही. पुन्हा प्रयत्न करा."
                                                        }
                                                    }
                                            }
                                        ) {
                                            Text(if (moreRequestSending) "…" else when (currentLanguage) { "hi" -> "अनुरोध भेजें"; "en" -> "Send request"; else -> "विनंती पाठवा" })
                                        }
                                    },
                                    dismissButton = {
                                        androidx.compose.material3.TextButton(
                                            onClick = { if (!moreRequestSending) showMoreRequestDialog = false }
                                        ) {
                                            Text(when (currentLanguage) { "hi" -> "रद्द करें"; "en" -> "Cancel"; else -> "रद्द करा" })
                                        }
                                    }
                                )
                            }
                        }
                        AnnapurnaTab.ORDERS -> {
                            CustomerOrdersScreen(
                                orders = recentOrders,
                                modifier = contentModifier
                            )
                        }
                        AnnapurnaTab.BAZAAR -> {
                            BazaarHomeScreen(
                                produceItems = produceItems,
                                selectedCategory = selectedCategory,
                                searchQuery = searchQuery,
                                cartItems = cartItems,
                                selectedDetailItem = selectedDetailItem,
                                onSelectCategory = storeViewModel::selectCategory,
                                onSearchQueryChange = storeViewModel::updateSearchQuery,
                                onAddToCart = { id, grams -> storeViewModel.addToCart(id, grams) },
                                onDecreaseQty = storeViewModel::decreaseProductQuantity,
                                onOpenDetail = storeViewModel::openProduceDetail,
                                onGoToCart = { currentTab = AnnapurnaTab.CART },
                                onOpenAssistant = { assistantStartAction = null; currentTab = AnnapurnaTab.ASSISTANT },
                                onOpenVoiceSearch = { assistantStartAction = "voice"; currentTab = AnnapurnaTab.ASSISTANT },
                                onOpenPhotoSearch = { assistantStartAction = "photo"; currentTab = AnnapurnaTab.ASSISTANT },
                                onOpenOffers = { currentTab = AnnapurnaTab.OFFERS },
                                onOpenNewArrivals = { currentTab = AnnapurnaTab.NEW_ARRIVALS },
                                onOpenLanguageSettings = { languageMenuExpanded = true },
                                onOpenOrders = { currentTab = AnnapurnaTab.ORDERS },
                                onShareApp = {
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Annapurna Vegetables and Fruits — ताज्या भाज्या आणि फळे सहज मागवा!")
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Annapurna share"))
                                },
                                onAskRecipeInChat = { produce ->
                                    currentTab = AnnapurnaTab.ASSISTANT
                                    val recipePrompt = when (currentLanguage) {
                                        "hi" -> "${produce.marathiName} से बनने वाली एक आसान और स्वादिष्ट रेसिपी बताइए। सामग्री, मात्रा और बनाने की विधि चरणों में समझाइए।"
                                        "en" -> "Suggest an easy and tasty recipe using ${produce.marathiName}. Explain ingredients, quantities, and step-by-step instructions."
                                        else -> "${produce.marathiName} वापरून एखादी सोपी आणि चविष्ट महाराष्ट्रीयन पाककृती सांगा. साहित्य, प्रमाण आणि कृती क्रमाने समजावून सांगा."
                                    }
                                    aiViewModel.sendChatMessage(
                                        promptOverride = recipePrompt,
                                        cartSummary = storeViewModel.buildCartSummaryForAi()
                                    )
                                },
                                modifier = contentModifier
                            )
                        }

                        AnnapurnaTab.CART -> {
                            CartAndOrderScreen(
                                cartItems = cartItems,
                                formState = formState,
                                recentOrders = recentOrders,
                                confirmedOrder = confirmedOrder,
                                trackingStatus = trackingStatus,
                                trackingLocation = trackingLocation,
                                trackingUpdatedAtMillis = trackingUpdatedAtMillis,
                                customerDestination = customerDestination,
                                trackingPartnerAccepted = trackingPartnerAccepted,
                                trackingPartnerName = trackingPartnerName,
                                onIncreaseQty = storeViewModel::increaseQuantity,
                                onDecreaseQty = storeViewModel::decreaseQuantity,
                                onRemoveItem = storeViewModel::removeFromCart,
                                onClearCart = storeViewModel::clearCart,
                                onCustomerNameChange = storeViewModel::updateCustomerName,
                                onMobileNumberChange = storeViewModel::updateMobileNumber,
                                onAddressChange = storeViewModel::updateDeliveryAddress,
                                onSubmitOrder = { latitude, longitude, deliveryCharge, distanceKm, paymentMethod, transactionId, deliveryTip, paymentOrderId, paymentSignature -> storeViewModel.submitOrder(latitude, longitude, deliveryCharge, distanceKm, paymentMethod, transactionId, paymentOrderId, paymentSignature, deliveryTip) },
                                onDismissReceipt = storeViewModel::dismissOrderReceipt,
                                onContinueShopping = {
                                    storeViewModel.dismissOrderReceipt()
                                    currentTab = AnnapurnaTab.BAZAAR
                                },
                                onReorder = { pastOrder ->
                                    storeViewModel.reorderPastOrder(pastOrder)
                                },
                                onSubmitDeliveryReview = storeViewModel::submitDeliveryReview,
                                onBackToBazaar = { currentTab = AnnapurnaTab.BAZAAR },
                                modifier = contentModifier
                            )
                        }

                        AnnapurnaTab.ASSISTANT -> {
                            MarathiChatScreen(
                                chatState = chatState,
                                startAction = assistantStartAction,
                                onStartActionConsumed = { assistantStartAction = null },
                                onSelectModelTier = aiViewModel::selectModelTier,
                                onInputChange = aiViewModel::updateChatInput,
                                onSendMessage = { overridePrompt, imageBase64, mimeType ->
                                    aiViewModel.sendChatMessage(
                                        promptOverride = overridePrompt,
                                        cartSummary = storeViewModel.buildCartSummaryForAi(),
                                        imageBase64 = imageBase64,
                                        mimeType = mimeType
                                    )
                                },
                                onResetChat = aiViewModel::resetConversation,
                                onBackToBazaar = { currentTab = AnnapurnaTab.BAZAAR },
                                modifier = contentModifier
                            )
                        }

                        AnnapurnaTab.PROFILE -> {
                            CustomerProfileScreen(
                                formState = formState,
                                recentOrders = recentOrders,
                                produceItems = produceItems,
                                trackingStatus = trackingStatus,
                                trackingLocation = trackingLocation,
                                trackingUpdatedAtMillis = trackingUpdatedAtMillis,
                                onNameChange = storeViewModel::updateCustomerName,
                                onMobileChange = storeViewModel::updateMobileNumber,
                                onAddressChange = storeViewModel::updateDeliveryAddress,
                                onSaveProfile = storeViewModel::saveCustomerProfileForEntry,
                                onOpenOrders = { currentTab = AnnapurnaTab.CART },
                                modifier = contentModifier
                            )
                        }

                    }
                }
            }
        }
    }
}

@Composable
private fun TabIconWithCartBadge(
    tab: AnnapurnaTab,
    selected: Boolean,
    cartCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == AnnapurnaTab.CART && cartCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(MarathiFormatters.toMarathiNumber(cartCount))
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = stringResource(tab.labelRes)
            )
        }
    } else {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(tab.labelRes)
        )
    }
}
