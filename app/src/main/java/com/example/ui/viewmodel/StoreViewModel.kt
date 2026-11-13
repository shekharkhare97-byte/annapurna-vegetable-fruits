package com.example.ui.viewmodel

import android.content.Context
import com.example.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.AnnapurnaDatabase
import com.example.data.local.OrderEntity
import com.example.data.model.ProduceCatalog
import com.example.data.model.ProduceCategory
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import android.content.SharedPreferences
import com.example.data.model.ProduceItem
import com.example.data.repository.CartLineItem
import com.example.data.repository.StoreRepository
import com.example.util.MarathiFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerFormState(
    val customerName: String = "",
    val mobileNumber: String = "",
    val deliveryAddress: String = "",
    val nameErrorMarathi: String? = null,
    val mobileErrorMarathi: String? = null,
    val addressErrorMarathi: String? = null,
    val cartErrorMarathi: String? = null,
    val isSubmitting: Boolean = false
)

class StoreViewModel(
    private val repository: StoreRepository,
    context: Context
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ProduceCategory.ALL)
    private var productsListener: ListenerRegistration? = null
    private var trackingListener: ListenerRegistration? = null
    private val appContext = context.applicationContext
    private val trackingPrefs = appContext.getSharedPreferences("annapurna_order_tracking", Context.MODE_PRIVATE)
    private val _trackingStatus = MutableStateFlow("")
    val trackingStatus: StateFlow<String> = _trackingStatus.asStateFlow()
    private val _trackingLocation = MutableStateFlow("")
    val trackingLocation: StateFlow<String> = _trackingLocation.asStateFlow()
    val selectedCategory: StateFlow<ProduceCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProduceDetail = MutableStateFlow<ProduceItem?>(null)
    val selectedProduceDetail: StateFlow<ProduceItem?> = _selectedProduceDetail.asStateFlow()

    private val _adminProducts = MutableStateFlow<List<ProduceItem>>(emptyList())

    val filteredProduce: StateFlow<List<ProduceItem>> = combine(
        _selectedCategory,
        _searchQuery,
        _adminProducts
    ) { category, rawQuery, products ->
        val query = rawQuery.trim().lowercase()
        products.filter { item ->
            val matchesCategory = category == ProduceCategory.ALL || item.category == category
            val matchesQuery = query.isEmpty() ||
                item.marathiName.lowercase().contains(query) ||
                item.hindiName.lowercase().contains(query) ||
                item.englishName.lowercase().contains(query) ||
                item.englishKeywords.lowercase().contains(query) ||
                item.descriptionMarathi.lowercase().contains(query) ||
                item.hindiDescription.lowercase().contains(query) ||
                item.englishDescription.lowercase().contains(query)
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cartItems: StateFlow<List<CartLineItem>> = repository.cartLineItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentOrders: StateFlow<List<OrderEntity>> = repository.recentOrders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _formState = MutableStateFlow(CustomerFormState())
    val formState: StateFlow<CustomerFormState> = _formState.asStateFlow()
    private val _customerProfileLoaded = MutableStateFlow(false)
    val customerProfileLoaded: StateFlow<Boolean> = _customerProfileLoaded.asStateFlow()
    private val _customerProfileReady = MutableStateFlow(false)
    val customerProfileReady: StateFlow<Boolean> = _customerProfileReady.asStateFlow()

    private val _confirmedOrder = MutableStateFlow<OrderEntity?>(null)
    val confirmedOrder: StateFlow<OrderEntity?> = _confirmedOrder.asStateFlow()

    private var profileHydrated = false

    init {
        trackingPrefs.getString("latest_token", null)?.let(::listenToOrderStatus)
        productsListener = FirebaseFirestore.getInstance().collection("products")
            .whereEqualTo("isVisible", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val products = snapshot.documents.mapNotNull { doc ->
                    val imageUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() }
                    if (doc.getBoolean("isVisible") != true) return@mapNotNull null
                    val stock = doc.getDouble("stock") ?: (doc.getLong("stock")?.toDouble() ?: 0.0)
                    if (stock <= 0.0) return@mapNotNull null
                    val unitText = sequenceOf("unit", "stockUnit", "sellUnit", "unitMarathi", "measurementUnit")
                        .mapNotNull { doc.getString(it) }
                        .firstOrNull { it.isNotBlank() }
                        .orEmpty()
                    val normalizedUnit = unitText.trim().lowercase()
                    val isBundleBased = normalizedUnit.contains("bunch") ||
                        normalizedUnit.contains("bundle") || unitText.contains("जुडी") ||
                        unitText.contains("जोडी") || unitText.contains("गड्डी")
                    val isPieceBased = isBundleBased ||
                        normalizedUnit.contains("nos") || normalizedUnit.contains("piece") ||
                        normalizedUnit.contains("pcs") || unitText.contains("नग") || unitText.contains("पिस")
                    val rate = if (isPieceBased) {
                        doc.getDouble("pricePerPiece") ?: doc.getLong("pricePerPiece")?.toDouble()
                            ?: doc.getDouble("price") ?: doc.getLong("price")?.toDouble()
                            ?: doc.getDouble("pricePerKg") ?: doc.getLong("pricePerKg")?.toDouble()
                    } else {
                        doc.getDouble("pricePerKg") ?: doc.getLong("pricePerKg")?.toDouble()
                            ?: doc.getDouble("price") ?: doc.getLong("price")?.toDouble()
                    } ?: return@mapNotNull null
                    val name = sequenceOf("nameMr", "marathiName", "name")
                        .mapNotNull { doc.getString(it) }
                        .firstOrNull { it.isNotBlank() } ?: return@mapNotNull null
                    val hindiName = sequenceOf("nameHi", "hindiName")
                        .mapNotNull { doc.getString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
                    val englishName = sequenceOf("nameEn", "englishName")
                        .mapNotNull { doc.getString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
                    val descriptionMr = doc.getString("descriptionMr")?.takeIf { it.isNotBlank() } ?: name
                    val descriptionHi = doc.getString("descriptionHi")?.takeIf { it.isNotBlank() }.orEmpty()
                    val descriptionEn = doc.getString("descriptionEn")?.takeIf { it.isNotBlank() }.orEmpty()
                    val category = if (doc.getString("category") == "fruit") ProduceCategory.FRUIT else ProduceCategory.VEGETABLE
                    ProduceItem(
                        id = doc.id,
                        marathiName = name,
                        englishKeywords = listOf(name, hindiName, englishName).joinToString(" ").lowercase(),
                        category = category,
                        priceRupees = rate,
                        unitMarathi = when {
                            isBundleBased -> "१ जुडी"
                            isPieceBased -> "१ नग"
                            else -> "१ किलो"
                        },
                        badgeMarathi = "ताजा माल",
                        descriptionMarathi = descriptionMr,
                        imageRes = if (category == ProduceCategory.FRUIT) R.drawable.img_prod_mango else R.drawable.img_prod_tomato,
                        imageUrl = imageUrl,
                        isPieceBased = isPieceBased,
                        hindiName = hindiName,
                        englishName = englishName,
                        hindiDescription = descriptionHi,
                        englishDescription = descriptionEn
                    )
                }
                ProduceCatalog.replaceFromAdmin(products)
                _adminProducts.value = products
            }

        viewModelScope.launch {
            repository.customerProfile.collect { saved ->
                if (!profileHydrated) {
                    profileHydrated = true
                    if (saved != null) {
                        _formState.update { current ->
                            current.copy(
                                customerName = current.customerName.ifBlank { saved.customerName },
                                mobileNumber = current.mobileNumber.ifBlank { saved.mobileNumber },
                                deliveryAddress = current.deliveryAddress.ifBlank { saved.deliveryAddress }
                            )
                        }
                    }
                    _customerProfileLoaded.value = true
                }
            }
        }
    }

    override fun onCleared() {
        productsListener?.remove()
        trackingListener?.remove()
        super.onCleared()
    }

    fun selectCategory(category: ProduceCategory) {
        _selectedCategory.value = category
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openProduceDetail(item: ProduceItem?) {
        _selectedProduceDetail.value = item
    }

    fun addToCart(produceId: String, weightGrams: Int = 1000) {
        viewModelScope.launch {
            repository.incrementItem(produceId, weightGrams)
            _formState.update { it.copy(cartErrorMarathi = null) }
        }
    }

    fun increaseQuantity(produceId: String) {
        viewModelScope.launch { repository.incrementQuantity(produceId) }
    }

    fun decreaseQuantity(produceId: String) {
        viewModelScope.launch {
            repository.decrementItem(produceId)
        }
    }

    fun removeFromCart(produceId: String) {
        viewModelScope.launch {
            repository.removeItem(produceId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun updateCustomerName(name: String) {
        _formState.update {
            it.copy(customerName = name, nameErrorMarathi = null)
        }
    }

    fun updateMobileNumber(rawMobile: String) {
        val cleaned = MarathiFormatters.normalizePhoneDigits(rawMobile)
        _formState.update {
            it.copy(mobileNumber = cleaned, mobileErrorMarathi = null)
        }
    }

    fun updateDeliveryAddress(address: String) {
        _formState.update {
            it.copy(deliveryAddress = address, addressErrorMarathi = null)
        }
    }

    fun saveCustomerProfileForEntry() {
        val current = _formState.value
        val nameError = if (current.customerName.trim().length < 2) appContext.getString(R.string.error_invalid_name) else null
        val mobileError = if (!MarathiFormatters.isValidIndianMobile(current.mobileNumber)) appContext.getString(R.string.error_invalid_mobile) else null
        val addressError = if (current.deliveryAddress.trim().length < 8) appContext.getString(R.string.error_invalid_address) else null
        if (nameError != null || mobileError != null || addressError != null) {
            _formState.update { it.copy(nameErrorMarathi = nameError, mobileErrorMarathi = mobileError, addressErrorMarathi = addressError) }
            return
        }
        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, cartErrorMarathi = null) }
            try {
                val normalizedMobile = MarathiFormatters.normalizePhoneDigits(current.mobileNumber)
                repository.saveCustomerProfile(current.customerName.trim(), normalizedMobile, current.deliveryAddress.trim())
                _formState.update { it.copy(mobileNumber = normalizedMobile, isSubmitting = false) }
                _customerProfileReady.value = true
            } catch (error: Exception) {
                _formState.update { it.copy(isSubmitting = false, cartErrorMarathi = appContext.getString(R.string.profile_save_failed)) }
            }
        }
    }

    fun submitOrder(latitude: Double? = null, longitude: Double? = null, deliveryChargeRupees: Int = 0, deliveryDistanceKm: Double? = null, paymentMethod: String = "COD", transactionId: String = "", paymentOrderId: String = "", paymentSignature: String = "", deliveryTipRupees: Int = 0) {
        val currentCart = cartItems.value
        val currentForm = _formState.value

        val cartErr = if (currentCart.isEmpty()) {
            appContext.getString(R.string.error_empty_cart)
        } else null

        val nameErr = if (currentForm.customerName.trim().length < 2) {
            appContext.getString(R.string.error_invalid_name)
        } else null

        val mobileErr = if (!MarathiFormatters.isValidIndianMobile(currentForm.mobileNumber)) {
            appContext.getString(R.string.error_invalid_mobile)
        } else null
        val addressErr = if (currentForm.deliveryAddress.trim().length < 8) {
            appContext.getString(R.string.error_invalid_address)
        } else null

        if (cartErr != null || nameErr != null || mobileErr != null || addressErr != null) {
            _formState.update {
                it.copy(
                    cartErrorMarathi = cartErr,
                    nameErrorMarathi = nameErr,
                    mobileErrorMarathi = mobileErr,
                    addressErrorMarathi = addressErr
                )
            }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, cartErrorMarathi = null) }
            try {
                val normalizedMobile = MarathiFormatters.normalizePhoneDigits(currentForm.mobileNumber)
                val savedOrder = repository.placeOrder(
                    customerName = currentForm.customerName,
                    mobileNumber = normalizedMobile,
                    deliveryAddress = currentForm.deliveryAddress,
                    items = currentCart,
                    latitude = latitude,
                    longitude = longitude,
                    paymentMethod = paymentMethod,
                    transactionId = transactionId.trim(),
                    paymentOrderId = paymentOrderId.trim(),
                    paymentSignature = paymentSignature.trim(),
                    deliveryChargeRupees = deliveryChargeRupees,
                    deliveryDistanceKm = deliveryDistanceKm,
                    deliveryTipRupees = deliveryTipRupees
                )
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        cartErrorMarathi = null,
                        nameErrorMarathi = null,
                        mobileErrorMarathi = null,
                        addressErrorMarathi = null
                    )
                }
                repository.latestTrackingToken?.let { token ->
                    trackingPrefs.edit().putString("latest_token", token).apply()
                    listenToOrderStatus(token)
                }
                _confirmedOrder.value = savedOrder
            } catch (error: Exception) {
                if (error is kotlinx.coroutines.CancellationException) throw error
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        cartErrorMarathi = appContext.getString(R.string.order_save_failed)
                    )
                }
            }
        }
    }

    private fun listenToOrderStatus(token: String) {
        trackingListener?.remove()
        trackingListener = FirebaseFirestore.getInstance()
            .collection("orderTracking").document(token)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _trackingStatus.value = "unavailable"
                } else if (snapshot != null && snapshot.exists()) {
                    _trackingStatus.value = snapshot.getString("status") ?: "new"
                    val latitude = snapshot.getDouble("deliveryLatitude")
                    val longitude = snapshot.getDouble("deliveryLongitude")
                    _trackingLocation.value = if (snapshot.getBoolean("deliveryLocationSharing") == true && latitude != null && longitude != null) "$latitude,$longitude" else ""
                }
            }
    }

    fun submitDeliveryReview(rating: Int, review: String, onResult: (Boolean) -> Unit = {}) {
        val token = trackingPrefs.getString("latest_token", null)
        if (token.isNullOrBlank() || rating !in 1..5) {
            onResult(false)
            return
        }
        val firestore = FirebaseFirestore.getInstance()
        val trackingRef = firestore.collection("orderTracking").document(token)
        trackingRef.get()
            .addOnSuccessListener { tracking ->
                val orderId = tracking.getString("orderId")
                if (!tracking.exists() || tracking.getString("status") != "completed" || orderId.isNullOrBlank()) {
                    onResult(false)
                    return@addOnSuccessListener
                }
                val reviewData = mapOf(
                    "customerRating" to rating,
                    "customerReview" to review.trim(),
                    "reviewSubmittedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                firestore.batch()
                    .update(trackingRef, reviewData)
                    .update(firestore.collection("orders").document(orderId), reviewData)
                    .commit()
                    .addOnSuccessListener { onResult(true) }
                    .addOnFailureListener { onResult(false) }
            }
            .addOnFailureListener { onResult(false) }
    }

    fun dismissOrderReceipt() {
        _confirmedOrder.value = null
    }

    fun reorderPastOrder(order: OrderEntity, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.reorderIntoCart(order)
            onComplete()
        }
    }

    fun buildCartSummaryForAi(): String {
        val items = cartItems.value
        if (items.isEmpty()) return "सध्या ग्राहकाची खरेदी पिशवी रिकामी आहे."
        val lines = items.joinToString("; ") {
            "${it.produce.marathiName} × ${it.quantity} (₹${MarathiFormatters.formatRupees(it.lineTotalRupees)})"
        }
        val total = items.sumOf { it.lineTotalRupees }
        return "$lines | एकूण रक्कम: ₹${MarathiFormatters.formatRupees(total)}"
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = AnnapurnaDatabase.getInstance(context)
                StoreViewModel(StoreRepository(db.storeDao()), context.applicationContext)
            }
        }
    }
}
