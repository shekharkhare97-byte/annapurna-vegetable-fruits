package com.example.ui.viewmodel

import android.content.Context
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
    val cartErrorMarathi: String? = null,
    val isSubmitting: Boolean = false
)

class StoreViewModel(
    private val repository: StoreRepository
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(ProduceCategory.ALL)
    private var productsListener: ListenerRegistration? = null
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
                item.englishKeywords.lowercase().contains(query) ||
                item.descriptionMarathi.lowercase().contains(query)
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

    private val _confirmedOrder = MutableStateFlow<OrderEntity?>(null)
    val confirmedOrder: StateFlow<OrderEntity?> = _confirmedOrder.asStateFlow()

    private var profileHydrated = false

    init {
        productsListener = FirebaseFirestore.getInstance().collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val products = snapshot.documents.mapNotNull { doc ->
                    val imageUrl = doc.getString("imageUrl")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    if (doc.getBoolean("isVisible") != true) return@mapNotNull null
                    val stock = doc.getDouble("stock") ?: (doc.getLong("stock")?.toDouble() ?: 0.0)
                    if (stock <= 0.0) return@mapNotNull null
                    val rate = doc.getDouble("pricePerKg")
                        ?: doc.getLong("pricePerKg")?.toDouble()
                        ?: doc.getDouble("price")
                        ?: doc.getLong("price")?.toDouble()
                        ?: return@mapNotNull null
                    val name = doc.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val category = if (doc.getString("category") == "fruit") ProduceCategory.FRUIT else ProduceCategory.VEGETABLE
                    ProduceItem(
                        id = doc.id,
                        marathiName = name,
                        englishKeywords = name.lowercase(),
                        category = category,
                        priceRupees = rate.toInt(),
                        unitMarathi = "१ किलो",
                        badgeMarathi = "ताजा माल",
                        descriptionMarathi = name,
                        imageRes = if (category == ProduceCategory.FRUIT) R.drawable.img_prod_mango else R.drawable.img_prod_tomato,
                        imageUrl = imageUrl
                    )
                }
                ProduceCatalog.replaceFromAdmin(products)
                _adminProducts.value = products
            }

        viewModelScope.launch {
            repository.customerProfile.collect { saved ->
                if (!profileHydrated && saved != null) {
                    profileHydrated = true
                    _formState.update { current ->
                        current.copy(
                            customerName = current.customerName.ifBlank { saved.customerName },
                            mobileNumber = current.mobileNumber.ifBlank { saved.mobileNumber },
                            deliveryAddress = current.deliveryAddress.ifBlank { saved.deliveryAddress }
                        )
                    }
                }
            }
        }
    }

    override fun onCleared() {\n        productsListener?.remove()\n        super.onCleared()\n    }\n\n    fun selectCategory(category: ProduceCategory) {
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
            it.copy(deliveryAddress = address)
        }
    }

    fun submitOrder() {
        val currentCart = cartItems.value
        val currentForm = _formState.value

        val cartErr = if (currentCart.isEmpty()) {
            "ऑर्डर देण्यापूर्वी कृपया पिशवीत किमान एक भाजी किंवा फळ टाका."
        } else null

        val nameErr = if (currentForm.customerName.trim().length < 2) {
            "कृपया ग्राहकाचे पूर्ण नाव प्रविष्ट करा."
        } else null

        val mobileErr = if (!MarathiFormatters.isValidIndianMobile(currentForm.mobileNumber)) {
            "कृपया वैध १० अंकी मोबाईल नंबर प्रविष्ट करा (उदा. 9876543210)."
        } else null

        if (cartErr != null || nameErr != null || mobileErr != null) {
            _formState.update {
                it.copy(
                    cartErrorMarathi = cartErr,
                    nameErrorMarathi = nameErr,
                    mobileErrorMarathi = mobileErr
                )
            }
            return
        }

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true) }
            val normalizedMobile = MarathiFormatters.normalizePhoneDigits(currentForm.mobileNumber)
            val savedOrder = repository.placeOrder(
                customerName = currentForm.customerName,
                mobileNumber = normalizedMobile,
                deliveryAddress = currentForm.deliveryAddress,
                items = currentCart
            )
            _formState.update {
                it.copy(
                    isSubmitting = false,
                    cartErrorMarathi = null,
                    nameErrorMarathi = null,
                    mobileErrorMarathi = null
                )
            }
            _confirmedOrder.value = savedOrder
        }
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
            "${it.produce.marathiName} × ${it.quantity} (₹${it.lineTotalRupees})"
        }
        val total = items.sumOf { it.lineTotalRupees }
        return "$lines | एकूण रक्कम: ₹$total"
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = AnnapurnaDatabase.getInstance(context)
                StoreViewModel(StoreRepository(db.storeDao()))
            }
        }
    }
}
