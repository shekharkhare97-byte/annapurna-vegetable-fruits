package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.ProduceCatalog
import com.example.data.model.ProduceCategory
import com.example.data.model.ProduceItem
import com.example.data.repository.CartLineItem
import com.example.util.MarathiFormatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BazaarHomeScreen(
    produceItems: List<ProduceItem>,
    selectedCategory: ProduceCategory,
    searchQuery: String,
    cartItems: List<CartLineItem>,
    selectedDetailItem: ProduceItem?,
    onSelectCategory: (ProduceCategory) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddToCart: (String, Int) -> Unit,
    onDecreaseQty: (String) -> Unit,
    onOpenDetail: (ProduceItem?) -> Unit,
    onGoToCart: () -> Unit,
    onAskRecipeInChat: (ProduceItem) -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenVoiceSearch: () -> Unit,
    onOpenPhotoSearch: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenNewArrivals: () -> Unit,
    onOpenLanguageSettings: () -> Unit,
    onOpenOrders: () -> Unit,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val qtyMap = remember(cartItems) {
        cartItems.groupBy { it.produce.id }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    }
    val totalCartQty = remember(cartItems) { cartItems.sumOf { it.quantity } }
    val totalCartAmount = remember(cartItems) { cartItems.sumOf { it.lineTotalRupees } }
    var weightDialogItem by remember { mutableStateOf<ProduceItem?>(null) }
    var showProductRequest by remember { mutableStateOf(false) }
    var requestedProductName by remember { mutableStateOf("") }
    var requestedProductQuantity by remember { mutableStateOf("") }
    var requestedProductNote by remember { mutableStateOf("") }
    var productRequestSending by remember { mutableStateOf(false) }
    var productRequestMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val favoritePrefs = remember { context.getSharedPreferences("annapurna_favorites", android.content.Context.MODE_PRIVATE) }
    val favoriteIds = remember { mutableStateListOf<String>().apply {
        addAll(favoritePrefs.getStringSet("ids", emptySet()) ?: emptySet())
    } }
    fun toggleFavorite(id: String) {
        if (id in favoriteIds) favoriteIds.remove(id) else favoriteIds.add(id)
        favoritePrefs.edit().putStringSet("ids", favoriteIds.toSet()).apply()
    }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    val visibleItems = remember(produceItems, favoriteIds.toList(), showFavoritesOnly) {
        if (showFavoritesOnly) produceItems.filter { it.id in favoriteIds } else produceItems
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = if (totalCartQty > 0) 104.dp else 28.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("produce_grid")
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                StoreBrandHeader()
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                SearchAndFilterBar(
                    searchQuery = searchQuery,
                    selectedCategory = selectedCategory,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectCategory = onSelectCategory
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveOffersHeroBanner(onOfferClick = onOpenOffers)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onSelectCategory = onSelectCategory,
                    onRequestProduct = { showProductRequest = true }
                )
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                AnnapurnaAiHomeCard(onClick = onOpenAssistant)
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                CustomerFeatureShortcuts(
                    onOffers = onOpenOffers,
                    onNewArrivals = onOpenNewArrivals,
                    onLanguage = onOpenLanguageSettings,
                    onVoiceSearch = onOpenVoiceSearch,
                    onPhotoSearch = onOpenPhotoSearch,
                    onRefer = onShareApp,
                    onWishlist = { showFavoritesOnly = !showFavoritesOnly },
                    onNotifications = onOpenOrders,
                    wishlistSelected = showFavoritesOnly
                )
            }

            if (selectedCategory != ProduceCategory.ALL || searchQuery.isNotBlank() || showFavoritesOnly) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (selectedCategory == ProduceCategory.ALL) stringResource(R.string.bazaar_today_title) else when (selectedCategory) { ProduceCategory.ALL -> stringResource(R.string.cat_all); ProduceCategory.VEGETABLE -> stringResource(R.string.cat_vegetables); ProduceCategory.FRUIT -> stringResource(R.string.cat_fruits) },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.bazaar_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = MarathiFormatters.toMarathiNumber(visibleItems.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            if (visibleItems.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyProduceState(onResetFilter = {
                        onSearchQueryChange("")
                        onSelectCategory(ProduceCategory.ALL)
                    })
                }
            } else {
                items(
                    items = visibleItems,
                    key = { it.id }
                ) { item ->
                    ProduceGridCard(
                        item = item,
                        quantityInCart = qtyMap[item.id] ?: 0,
                        onCardClick = { if (!item.isOutOfStock) onOpenDetail(item) },
                        isFavorite = item.id in favoriteIds,
                        onToggleFavorite = { toggleFavorite(item.id) },
                        onAdd = { weightDialogItem = item },
                        onDecrease = { onDecreaseQty(item.id) }
                    )
                }
            }
            }
        }


        AnimatedVisibility(
            visible = totalCartQty > 0,
            enter = slideInVertically(animationSpec = spring()) { it } + fadeIn(),
            exit = slideOutVertically(animationSpec = spring()) { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            FloatingCartBanner(
                totalItems = totalCartQty,
                totalRupees = totalCartAmount,
                onGoToCart = onGoToCart
            )
        }
    }

    val productRequestFailureText = customerText(
        "विनंती पाठवता आली नाही. कृपया पुन्हा प्रयत्न करा.",
        "अनुरोध नहीं भेजा जा सका। कृपया फिर कोशिश करें।",
        "Could not send request. Please try again."
    )
    if (showProductRequest) {
        AlertDialog(
            onDismissRequest = { if (!productRequestSending) showProductRequest = false },
            title = { Text(customerText("नवीन वस्तू मागवा", "नई चीज़ मँगवाएँ", "Request a new item")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = requestedProductName,
                        onValueChange = { requestedProductName = it },
                        label = { Text(customerText("वस्तूचे नाव", "वस्तु का नाम", "Item name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = requestedProductQuantity,
                        onValueChange = { requestedProductQuantity = it },
                        label = { Text(customerText("प्रमाण (उदा. २ किलो)", "मात्रा (जैसे २ किलो)", "Quantity (e.g. 2 kg)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = requestedProductNote,
                        onValueChange = { requestedProductNote = it },
                        label = { Text(customerText("अधिक माहिती (ऐच्छिक)", "अधिक जानकारी (वैकल्पिक)", "Extra details (optional)")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    productRequestMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            },
            confirmButton = {
                Button(
                    enabled = requestedProductName.isNotBlank() && !productRequestSending,
                    onClick = {
                        productRequestSending = true
                        val request = hashMapOf<String, Any>(
                            "productName" to requestedProductName.trim(),
                            "quantity" to requestedProductQuantity.trim(),
                            "note" to requestedProductNote.trim(),
                            "status" to "pending",
                            "source" to "customer_app",
                            "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                            "createdAtMillis" to System.currentTimeMillis()
                        )
                        FirebaseFirestore.getInstance().collection("productRequests").add(request)
                            .addOnSuccessListener {
                                productRequestSending = false
                                showProductRequest = false
                                requestedProductName = ""
                                requestedProductQuantity = ""
                                requestedProductNote = ""
                                productRequestMessage = null
                            }
                            .addOnFailureListener { error ->
                                productRequestSending = false
                                productRequestMessage = productRequestFailureText
                            }
                    }
                ) { Text(if (productRequestSending) customerText("पाठवत आहे…", "भेज रहे हैं…", "Sending…") else customerText("विनंती पाठवा", "अनुरोध भेजें", "Send request")) }
            },
            dismissButton = {
                TextButton(onClick = { showProductRequest = false }) { Text(customerText("रद्द करा", "रद्द करें", "Cancel")) }
            }
        )
    }

    if (weightDialogItem != null) {
        WeightSelectionDialog(
            item = weightDialogItem!!,
            onDismiss = { weightDialogItem = null },
            onConfirm = { grams ->
                onAddToCart(weightDialogItem!!.id, grams)
                weightDialogItem = null
            }
        )
    }

    if (selectedDetailItem != null) {
        Dialog(
            onDismissRequest = { onOpenDetail(null) },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(androidx.compose.foundation.rememberScrollState())) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(customerText("उत्पादनाची माहिती", "उत्पाद की जानकारी", "Product details"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onOpenDetail(null) }) { Icon(Icons.Default.Clear, contentDescription = customerText("बंद करा", "बंद करें", "Close")) }
                    }
                    ProduceDetailSheetContent(
                        item = selectedDetailItem,
                        isFavorite = selectedDetailItem.id in favoriteIds,
                        onToggleFavorite = { toggleFavorite(selectedDetailItem.id) },
                        onAdd = { weightDialogItem = selectedDetailItem }
                    )
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(customerText("इतर उत्पादने", "अन्य उत्पाद", "Other products"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(produceItems.filter { it.id != selectedDetailItem.id }.take(12), key = { it.id }) { other ->
                                Card(
                                    modifier = Modifier.width(142.dp).clickable { onOpenDetail(other) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (other.imageUrl.isNullOrBlank()) {
                                            Image(painter = painterResource(id = other.imageRes), contentDescription = other.displayName(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)))
                                        } else {
                                            AsyncImage(model = other.imageUrl, contentDescription = other.displayName(), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)))
                                        }
                                        Text(other.displayName(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("₹${MarathiFormatters.formatRupees(other.priceRupees)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnapurnaAiHomeCard(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color(0xFFEAF6EC),
        contentColor = Color(0xFF145C2C),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD5EAD8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = Color(0xFFD4EED9),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Annapurna AI",
                        tint = Color(0xFF14743A),
                        modifier = Modifier.size(27.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "Annapurna AI",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF145C2C)
                )
                Text(
                    text = customerText(
                        "भाजी-फळांची माहिती, पाककृती आणि शोधासाठी AI ला विचारा",
                        "सब्ज़ियों-फलों की जानकारी, रेसिपी और खोज के लिए AI से पूछें",
                        "Ask AI about produce, recipes and finding products"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF416B4D),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF14743A)
            )
        }
    }
}

@Composable
private fun CustomerFeatureShortcuts(
    onOffers: () -> Unit,
    onNewArrivals: () -> Unit,
    onLanguage: () -> Unit,
    onVoiceSearch: () -> Unit,
    onPhotoSearch: () -> Unit,
    onRefer: () -> Unit,
    onWishlist: () -> Unit,
    onNotifications: () -> Unit,
    wishlistSelected: Boolean
) {
    val tiles = listOf(
        Triple("🎁", customerText("Live Offers", "Live Offers", "Live Offers"), onOffers),
        Triple("🆕", customerText("नवीन आगमन", "नए उत्पाद", "New Arrivals"), onNewArrivals),
        Triple("🌐", customerText("भाषा", "भाषा", "Language"), onLanguage),
        Triple("🎙️", customerText("आवाज शोध", "आवाज़ से खोजें", "Voice Search"), onVoiceSearch),
        Triple("📷", customerText("फोटोने शोधा", "फोटो से खोजें", "Search by Photo"), onPhotoSearch),
        Triple("👥", customerText("मित्रांना सांगा", "दोस्तों को बताएँ", "Refer & Earn"), onRefer),
        Triple("❤️", customerText("आवडती यादी", "पसंदीदा सूची", "Wishlist"), onWishlist),
        Triple("🔔", customerText("ऑर्डर सूचना", "ऑर्डर सूचनाएँ", "Notifications"), onNotifications)
    )
    Column(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            customerText("तुमच्यासाठी खास सुविधा", "आपके लिए खास सुविधाएँ", "Quick features"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(176.dp)
        ) {
            items(tiles.size) { index ->
                val tile = tiles[index]
                Surface(
                    onClick = tile.third,
                    color = if (index == 6 && wishlistSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF1F8F2),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 7.dp)
                    ) {
                        Text(tile.first, style = MaterialTheme.typography.titleLarge)
                        Text(
                            tile.second,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StoreBrandHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(15.dp),
                    color = Color(0xFFE8F5E9),
                    modifier = Modifier.size(56.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_launcher_logo),
                        contentDescription = stringResource(R.string.brand_name),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = stringResource(R.string.brand_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF12652F)
                    )
                    Text(
                        text = stringResource(R.string.brand_tagline),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF52745B)
                    )
                }
            }
            Surface(
                color = Color(0xFFE8F5E9),
                shape = CircleShape
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Eco,
                        contentDescription = null,
                        tint = Color(0xFF16843B),
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = stringResource(R.string.brand_fresh_daily),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF12652F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Surface(
            color = Color(0xFFF0F8EF),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF16843B),
                    modifier = Modifier.size(21.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        customerText("डिलिव्हरी कुठे हवी आहे?", "डिलीवरी कहाँ चाहिए?", "Delivering to"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF52745B)
                    )
                    Text(
                        customerText("तुमचा पत्ता ऑर्डर करताना भरा", "ऑर्डर करते समय अपना पता भरें", "Add your address at checkout"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF174B29)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF16843B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HeroMarketBanner(onShopNow: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_market_banner")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(178.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner),
                contentDescription = stringResource(R.string.store_title_marathi),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = CircleShape
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.hero_badge_fresh),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(R.string.hero_headline),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.hero_subtext),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(9.dp))
                    Button(
                        onClick = onShopNow,
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF12652F)
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            customerText("आत्ताच खरेदी करा", "अभी खरीदें", "Shop Now"),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    selectedCategory: ProduceCategory,
    onSearchQueryChange: (String) -> Unit,
    onSelectCategory: (ProduceCategory) -> Unit
) {
    val allCount = ProduceCatalog.items.size
    val vegCount = ProduceCatalog.items.count { it.category == ProduceCategory.VEGETABLE }
    val fruitCount = ProduceCatalog.items.count { it.category == ProduceCategory.FRUIT }
    val suggestions = remember(searchQuery, ProduceCatalog.items) {
        if (searchQuery.isBlank()) emptyList()
        else ProduceCatalog.items.filter { item ->
            item.displayName().contains(searchQuery.trim(), ignoreCase = true) ||
                item.englishKeywords.contains(searchQuery.trim(), ignoreCase = true) ||
                item.englishName.contains(searchQuery.trim(), ignoreCase = true) ||
                item.hindiName.contains(searchQuery.trim(), ignoreCase = true)
        }.take(5)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.search_clear_desc)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFFFFF),
                unfocusedContainerColor = Color(0xFFFFFFFF),
                focusedBorderColor = Color(0xFF16843B),
                unfocusedBorderColor = Color(0xFFDCEBDD),
                cursorColor = Color(0xFF16843B),
                focusedLeadingIconColor = Color(0xFF16843B),
                unfocusedLeadingIconColor = Color(0xFF66836C)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .testTag("search_produce_input")
        )

        // Blinkit-style live suggestions appear while the customer types.
        if (suggestions.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = customerText("तुम्ही हे शोधत आहात", "आप यह खोज रहे हैं", "You are searching for"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                    suggestions.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSearchQueryChange(item.displayName()) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = item.displayName(),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }


    }
}


@Composable
private fun CategoryFilterRow(
    selectedCategory: ProduceCategory,
    onSelectCategory: (ProduceCategory) -> Unit,
    onRequestProduct: () -> Unit
) {
    data class HomeCategory(val emoji: String, val mr: String, val hi: String, val en: String, val category: ProduceCategory?, val request: Boolean = false)
    val categories = listOf(
        HomeCategory("🥬", "भाज्या", "सब्ज़ियाँ", "Vegetables", ProduceCategory.VEGETABLE),
        HomeCategory("🍎", "फळे", "फल", "Fruits", ProduceCategory.FRUIT),
        HomeCategory("🌿", "पालेभाज्या", "हरी पत्तेदार", "Leafy Greens", ProduceCategory.VEGETABLE),
        HomeCategory("🎁", "ऑफर्स", "ऑफर", "Offers", ProduceCategory.ALL),
        HomeCategory("🌱", "सेंद्रिय", "ऑर्गेनिक", "Organic", ProduceCategory.ALL),
        HomeCategory("🧺", "दैनंदिन गरजा", "रोज़मर्रा", "Daily Needs", ProduceCategory.ALL),
        HomeCategory("📦", "कॉम्बो पॅक", "कॉम्बो पैक", "Combo Packs", ProduceCategory.ALL),
        HomeCategory("＋", "अधिक", "और", "More", null, request = true)
    )
    val language = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0].language
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            customerText("कॅटेगरी निवडा", "श्रेणी चुनें", "Shop by category"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.height(174.dp)
        ) {
            items(categories.size) { index ->
                val item = categories[index]
                val label = when (language) {
                    "hi" -> item.hi
                    "en" -> item.en
                    else -> item.mr
                }
                val selected = item.category != null && selectedCategory == item.category &&
                    (item.category != ProduceCategory.ALL)
                Surface(
                    onClick = {
                        if (item.request) onRequestProduct()
                        else item.category?.let(onSelectCategory)
                    },
                    color = if (selected) Color(0xFFE1F2E4) else Color(0xFFF3F8F2),
                    shape = RoundedCornerShape(15.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selected) Color(0xFF16843B) else Color(0xFFE1ECE1)
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 8.dp)
                    ) {
                        Text(item.emoji, style = MaterialTheme.typography.titleLarge)
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = Color(0xFF315C3A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProduceGridCard(
    item: ProduceItem,
    quantityInCart: Int,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onCardClick: () -> Unit,
    onAdd: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick)
            .testTag("produce_card_${item.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.08f)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                if (item.imageUrl.isNullOrBlank()) {
                    Image(
                        painter = painterResource(id = item.imageRes),
                        contentDescription = item.displayName(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (item.isOutOfStock) 0.32f else 1f)
                    )
                } else {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.displayName(),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().alpha(if (item.isOutOfStock) 0.32f else 1f)
                    )
                }
                if (item.isOutOfStock) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.68f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text(
                            text = customerText("साठा संपला आहे", "स्टॉक खत्म है", "Out of Stock"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(38.dp)) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (isFavorite) customerText("आवडीतून काढा", "पसंदीदा से हटाएँ", "Remove from favorites") else customerText("आवडीत जोडा", "पसंदीदा में जोड़ें", "Add to favorites"),
                            tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = item.displayBadge(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = item.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.displayUnit(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "₹${MarathiFormatters.formatRupees(item.priceRupees)}",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (item.isPieceBased) customerText("/ नग किंवा जुडी", "/ नग या गट्ठा", "/ piece or bunch") else customerText("/ किलो", "/ किलो", "/ kg"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (item.isOutOfStock) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = customerText("साठा संपला आहे", "स्टॉक खत्म है", "Out of Stock"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (quantityInCart == 0) {
                    Button(
                        onClick = onAdd,
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("add_to_cart_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.btn_add_to_cart),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            FilledIconButton(
                                onClick = onDecrease,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("qty_minus_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.cd_decrease_qty)
                                )
                            }

                            Text(
                                text = MarathiFormatters.toMarathiNumber(quantityInCart),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )

                            FilledIconButton(
                                onClick = onAdd,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("qty_plus_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.cd_increase_qty)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingCartBanner(
    totalItems: Int,
    totalRupees: Double,
    onGoToCart: () -> Unit
) {
    Surface(
        onClick = onGoToCart,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("floating_cart_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(
                        R.string.cart_items_summary_format,
                        MarathiFormatters.toMarathiNumber(totalItems),
                        MarathiFormatters.formatRupees(totalRupees)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.floating_cart_cta),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyProduceState(onResetFilter: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.empty_search_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_search_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = onResetFilter,
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = stringResource(R.string.cat_all))
            }
        }
    }
}

@Composable
private fun ProduceDetailSheetContent(
    item: ProduceItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        if (item.imageUrl.isNullOrBlank()) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)
            )
        } else {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.displayName(),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.displayName(),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = item.displayUnit(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "₹${MarathiFormatters.formatRupees(item.priceRupees)}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (item.isPieceBased) stringResource(R.string.unit_per_piece_bundle) else stringResource(R.string.unit_per_kg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = item.displayDescription(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onAdd,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.btn_add_to_cart))
            }
            OutlinedButton(
                onClick = onToggleFavorite,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.weight(1f).height(50.dp)
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFE53935) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isFavorite) customerText("आवडत्या यादीत", "पसंदीदा में", "Wishlisted") else customerText("आवडतीत जोडा", "पसंदीदा में जोड़ें", "Add Favourite"))
            }
        }
    }
}


@Composable
private fun CustomerOffersStrip() {
    val offers = remember { mutableStateListOf<Map<String, Any>>() }
    DisposableEffect(Unit) {
        val registration = FirebaseFirestore.getInstance()
            .collection("customerOffers")
            .whereEqualTo("active", true)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    offers.clear()
                    offers.addAll(
                        snapshot.documents
                            .mapNotNull { document ->
                                document.data?.let { data -> mapOf("offerId" to document.id) + data }
                            }
                            .sortedByDescending { offer ->
                                (offer["createdAt"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                            }
                    )
                }
            }
        onDispose { registration.remove() }
    }

    if (offers.isNotEmpty()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        customerText("आजच्या खास Offers", "आज के खास ऑफ़र", "Today’s special offers"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        customerText("तुमच्यासाठी खास बचत आणि ताज्या ऑफर्स", "आपके लिए खास बचत और ताज़ा ऑफ़र", "Special savings and fresh offers for you"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        customerText("खास ऑफर्स", "खास ऑफ़र", "SPECIAL OFFERS"),
                        color = Color(0xFF16843B),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(
                    items = offers,
                    key = { offer ->
                        offer["offerId"]?.toString()
                            ?: offer["title"]?.toString().orEmpty()
                    }
                ) { offer ->
                    val imageUrl = offer["imageUrl"]?.toString().orEmpty()
                    val offerLanguage = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
                    val offerTitle = when (offerLanguage) {
                        "hi" -> offer["titleHi"]?.toString().orEmpty().ifBlank { offer["title"]?.toString().orEmpty() }
                        "en" -> offer["titleEn"]?.toString().orEmpty().ifBlank { offer["title"]?.toString().orEmpty() }
                        else -> offer["titleMr"]?.toString().orEmpty().ifBlank { offer["title"]?.toString().orEmpty() }
                    }
                    val offerDescription = when (offerLanguage) {
                        "hi" -> offer["descriptionHi"]?.toString().orEmpty().ifBlank { offer["description"]?.toString().orEmpty() }
                        "en" -> offer["descriptionEn"]?.toString().orEmpty().ifBlank { offer["description"]?.toString().orEmpty() }
                        else -> offer["descriptionMr"]?.toString().orEmpty().ifBlank { offer["description"]?.toString().orEmpty() }
                    }

                    Box(
                        modifier = Modifier
                            .width(284.dp)
                            .height(170.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF238B45), Color(0xFF075B2A))
                                )
                            )
                    ) {
                        if (imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = offerTitle,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color(0x99000000),
                                            Color(0xE6000000)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(15.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Surface(
                                color = Color(0xFFFFD54F),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    customerText("आजची खास ऑफर", "आज का खास ऑफ़र", "Today’s special offer"),
                                    color = Color(0xFF3E2723),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                            Text(
                                offerTitle,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                offerDescription,
                                color = Color.White.copy(alpha = 0.94f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun customerText(mr: String, hi: String, en: String): String {
    val language = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language ?: "mr"
    return when (language) { "hi" -> hi; "en" -> en; else -> mr }
}
