package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PhotoCamera
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.ProduceCategory
import com.example.ui.screens.BazaarHomeScreen
import com.example.ui.screens.CustomerWelcomeFlow
import com.example.ui.screens.CartAndOrderScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.MarathiChatScreen
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
    CART(
        labelRes = R.string.nav_cart,
        selectedIcon = Icons.Filled.ShoppingBag,
        unselectedIcon = Icons.Outlined.ShoppingBag,
        tag = "nav_tab_cart"
    ),
    ASSISTANT(
        labelRes = R.string.nav_assistant,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        tag = "nav_tab_assistant"
    ),
    STUDIO(
        labelRes = R.string.nav_studio,
        selectedIcon = Icons.Filled.PhotoCamera,
        unselectedIcon = Icons.Outlined.PhotoCamera,
        tag = "nav_tab_studio"
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
        // Preload Razorpay checkout resources before the customer starts payment.
        Checkout.preload(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnnapurnaTheme {
                CustomerWelcomeFlow {
                    AnnapurnaRootApp()
                }
            }
        }
    }
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
        when (entryPrefs.getString("selected_category", "")) {
            "vegetables" -> storeViewModel.selectCategory(ProduceCategory.VEGETABLE)
            "fruits" -> storeViewModel.selectCategory(ProduceCategory.FRUIT)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(AnnapurnaTab.BAZAAR) }

    val produceItems by storeViewModel.filteredProduce.collectAsStateWithLifecycle()
    val selectedCategory by storeViewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by storeViewModel.searchQuery.collectAsStateWithLifecycle()
    val cartItems by storeViewModel.cartItems.collectAsStateWithLifecycle()
    val selectedDetailItem by storeViewModel.selectedProduceDetail.collectAsStateWithLifecycle()
    val formState by storeViewModel.formState.collectAsStateWithLifecycle()
    val recentOrders by storeViewModel.recentOrders.collectAsStateWithLifecycle()
    val confirmedOrder by storeViewModel.confirmedOrder.collectAsStateWithLifecycle()
    val trackingStatus by storeViewModel.trackingStatus.collectAsStateWithLifecycle()
    val trackingLocation by storeViewModel.trackingLocation.collectAsStateWithLifecycle()

    val chatState by aiViewModel.chatState.collectAsStateWithLifecycle()
    val selectedImageSize by aiViewModel.selectedImageSize.collectAsStateWithLifecycle()
    val selectedAspectRatio by aiViewModel.selectedAspectRatio.collectAsStateWithLifecycle()
    val studioPrompt by aiViewModel.studioPrompt.collectAsStateWithLifecycle()
    val studioState by aiViewModel.studioState.collectAsStateWithLifecycle()

    val totalCartItems = remember(cartItems) { cartItems.sumOf { it.quantity } }
    var languageMenuExpanded by remember { mutableStateOf(false) }
    val currentLanguage = context.resources.configuration.locales[0].language
    val languageLabel = when (currentLanguage) { "hi" -> "हिन्दी"; "en" -> "English"; else -> "मराठी" }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(7.dp)
                                        .size(22.dp)
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
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AnnapurnaTab.entries.forEach { tab ->
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
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
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
                        AnnapurnaTab.entries.forEach { tab ->
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
                                onDecreaseQty = storeViewModel::decreaseQuantity,
                                onOpenDetail = storeViewModel::openProduceDetail,
                                onGoToCart = { currentTab = AnnapurnaTab.CART },
                                onAskRecipeInChat = { produce ->
                                    currentTab = AnnapurnaTab.ASSISTANT
                                    aiViewModel.sendChatMessage(
                                        promptOverride = "${produce.marathiName} वापरून एखादी सोपी आणि चविष्ट महाराष्ट्रीयन पाककृती सांगा.",
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
                                onSelectModelTier = aiViewModel::selectModelTier,
                                onInputChange = aiViewModel::updateChatInput,
                                onSendMessage = { overridePrompt ->
                                    aiViewModel.sendChatMessage(
                                        promptOverride = overridePrompt,
                                        cartSummary = storeViewModel.buildCartSummaryForAi()
                                    )
                                },
                                onResetChat = aiViewModel::resetConversation,
                                onBackToBazaar = { currentTab = AnnapurnaTab.BAZAAR },
                                modifier = contentModifier
                            )
                        }

                        AnnapurnaTab.STUDIO -> {
                            ImageStudioScreen(
                                selectedSize = selectedImageSize,
                                selectedRatio = selectedAspectRatio,
                                prompt = studioPrompt,
                                studioState = studioState,
                                onSelectSize = aiViewModel::selectStudioImageSize,
                                onSelectRatio = aiViewModel::selectStudioAspectRatio,
                                onPromptChange = aiViewModel::updateStudioPrompt,
                                onGenerateClick = aiViewModel::generateHighQualityImage,
                                onBackToBazaar = { currentTab = AnnapurnaTab.BAZAAR },
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
