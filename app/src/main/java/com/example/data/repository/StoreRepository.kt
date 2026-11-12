package com.example.data.repository

import com.example.data.local.CartItemEntity
import com.example.data.local.CustomerProfileEntity
import com.example.data.local.OrderEntity
import com.example.data.local.StoreDao
import com.example.data.model.ProduceCatalog
import com.example.data.model.ProduceItem
import com.example.data.model.WeightPricing
import com.example.data.payment.RazorpayPaymentGateway
import com.example.util.MarathiFormatters
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine

data class CartLineItem(
    val produce: ProduceItem,
    val quantity: Int,
    val weightGrams: Int
) {
    val lineTotalRupees: Double get() = if (produce.isPieceBased) produce.priceRupees * quantity else (WeightPricing.priceInPaiseForGrams(produce.priceRupees, weightGrams) * quantity) / 100.0
}

class StoreRepository(private val storeDao: StoreDao) {

    @Volatile var latestTrackingToken: String? = null
        private set

    // Re-map persisted cart rows whenever either Room rows or the Firebase product catalog changes.
    // This prevents a restored cart from appearing empty while products are still loading at app startup.
    val cartLineItems: Flow<List<CartLineItem>> = combine(
        storeDao.observeCartItems(),
        ProduceCatalog.itemsFlow
    ) { entities, products ->
        entities.mapNotNull { entity ->
            val produce = products.firstOrNull { it.id == entity.produceId }
            if (produce != null && entity.quantity > 0) {
                CartLineItem(produce = produce, quantity = entity.quantity, weightGrams = entity.weightGrams)
            } else {
                null
            }
        }
    }

    val customerProfile: Flow<CustomerProfileEntity?> = storeDao.observeCustomerProfile()

    val recentOrders: Flow<List<OrderEntity>> = storeDao.observeAllOrders()

    suspend fun incrementItem(produceId: String, weightGrams: Int = 1000) {
        val current = storeDao.getCartItem(produceId)
        val nextQty = (current?.quantity ?: 0) + 1
        storeDao.upsertCartItem(
            CartItemEntity(
                produceId = produceId,
                quantity = nextQty,
                weightGrams = weightGrams,
                addedAtMillis = current?.addedAtMillis ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun incrementQuantity(produceId: String) {
        val current = storeDao.getCartItem(produceId) ?: return
        storeDao.upsertCartItem(current.copy(quantity = current.quantity + 1))
    }

    suspend fun decrementItem(produceId: String) {
        val current = storeDao.getCartItem(produceId) ?: return
        val nextQty = current.quantity - 1
        if (nextQty <= 0) {
            storeDao.deleteCartItem(produceId)
        } else {
            storeDao.upsertCartItem(current.copy(quantity = nextQty))
        }
    }

    suspend fun removeItem(produceId: String) {
        storeDao.deleteCartItem(produceId)
    }

    suspend fun clearCart() {
        storeDao.clearCart()
    }

    suspend fun saveCustomerProfile(name: String, mobile: String, address: String) {
        storeDao.saveCustomerProfile(
            CustomerProfileEntity(
                id = 1,
                customerName = name,
                mobileNumber = mobile,
                deliveryAddress = address
            )
        )
    }

    suspend fun placeOrder(
        customerName: String,
        mobileNumber: String,
        deliveryAddress: String,
        items: List<CartLineItem>,
        latitude: Double? = null,
        longitude: Double? = null,
        paymentMethod: String = "COD",
        transactionId: String = "",
        paymentOrderId: String = "",
        paymentSignature: String = "",
        deliveryChargeRupees: Int = 0,
        deliveryDistanceKm: Double? = null,
        deliveryTipRupees: Int = 0
    ): OrderEntity {
        val summaryLines = items.joinToString(separator = "\n") { line ->
            val quantityLabel = if (line.produce.isPieceBased) {
                "${MarathiFormatters.toMarathiNumber(line.quantity)} ${line.produce.unitMarathi.removePrefix("१ ")}"
            } else {
                WeightPricing.formatWeightMarathi(line.weightGrams)
            }
            "• ${line.produce.marathiName} ($quantityLabel) = ₹${MarathiFormatters.formatRupees(line.lineTotalRupees)}"
        }
        val serialized = items.joinToString(separator = ",") { "${it.produce.id}:${it.quantity}:${it.weightGrams}" }
        val totalQty = items.sumOf { it.quantity }
        val itemsTotal = items.sumOf { it.lineTotalRupees }
        val grandTotal = itemsTotal + deliveryChargeRupees + deliveryTipRupees
        val tipSummary = if (deliveryTipRupees > 0) "\n• डिलिव्हरी पार्टनर टिप = ₹${MarathiFormatters.toMarathiNumber(deliveryTipRupees)}" else ""
        val completeSummary = summaryLines + "\n• डिलिव्हरी चार्ज = ₹${MarathiFormatters.toMarathiNumber(deliveryChargeRupees)}" + tipSummary
        val now = System.currentTimeMillis()

        val draftOrder = OrderEntity(
            customerName = customerName.trim(),
            mobileNumber = mobileNumber.trim(),
            deliveryAddress = deliveryAddress.trim(),
            itemsSummaryMarathi = completeSummary,
            serializedItems = serialized,
            totalItemsCount = totalQty,
            totalAmountRupees = grandTotal.roundToInt(),
            totalAmountRupeesDecimal = grandTotal,
            createdAtMillis = now
        )
        // A random token exposes status only, never customer details.
        val firestore = FirebaseFirestore.getInstance()
        val remoteOrder = firestore.collection("orders").document()
        val trackingToken = UUID.randomUUID().toString()
        latestTrackingToken = trackingToken
        val remoteItems = items.map { line ->
            mapOf(
                "productId" to line.produce.id,
                "name" to line.produce.displayName(),
                "nameMr" to line.produce.marathiName,
                "nameHi" to line.produce.hindiName,
                "nameEn" to line.produce.englishName,
                "weightGrams" to line.weightGrams,
                "quantity" to line.quantity,
                "pricePerKgRupees" to line.produce.priceRupees,
                "unit" to line.produce.unitMarathi,
                "isPieceBased" to line.produce.isPieceBased,
                "lineTotalRupees" to line.lineTotalRupees
            )
        }
        val remoteOrderData = mutableMapOf<String, Any>(
                "orderId" to remoteOrder.id,
                "trackingToken" to trackingToken,
                "customerName" to customerName.trim(),
                "mobileNumber" to mobileNumber.trim(),
                "deliveryAddress" to deliveryAddress.trim(),
                "items" to remoteItems,
                "itemsSummaryMarathi" to completeSummary,
                "totalItemsCount" to totalQty,
                "itemsTotalRupees" to itemsTotal,
                "deliveryChargeRupees" to deliveryChargeRupees,
                "deliveryTipRupees" to deliveryTipRupees,
                "deliveryDistanceKm" to (deliveryDistanceKm ?: 0.0),
                "totalAmountRupees" to grandTotal,
                "status" to "new",
                "paymentMethod" to paymentMethod,
                "transactionId" to transactionId.trim(),
                "paymentStatus" to if (paymentMethod == "COD") "pending" else "paid",
                "paymentGateway" to if (paymentMethod == "COD") "cash_on_delivery" else "razorpay",
                "createdAt" to FieldValue.serverTimestamp(),
                "createdAtMillis" to now
            )
        if (latitude != null && longitude != null) {
            remoteOrderData["latitude"] = latitude
            remoteOrderData["longitude"] = longitude
            remoteOrderData["locationSource"] = "customer_address_geocoded"
        }
        val trackingData = mapOf(
            "status" to "new",
            "createdAtMillis" to now,
            "orderId" to remoteOrder.id
        )
        if (paymentMethod == "COD") {
            // The trusted Worker reloads product prices and checks current stock before
            // atomically writing the order and tracking record. Never trust client totals.
            remoteOrderData.remove("createdAt")
            val saved = RazorpayPaymentGateway.submitCodOrder(
                order = remoteOrderData,
                tracking = trackingData
            )
            check(saved) { "COD order could not be securely saved." }
        } else {
            require(paymentOrderId.isNotBlank() && paymentSignature.isNotBlank() && transactionId.isNotBlank()) {
                "Verified Razorpay payment details are missing."
            }
            // Paid orders are written only by the trusted Worker after Razorpay capture verification.
            remoteOrderData.remove("createdAt")
            val saved = RazorpayPaymentGateway.submitPaidOrder(
                orderId = paymentOrderId,
                paymentId = transactionId,
                signature = paymentSignature,
                amountPaise = (grandTotal * 100.0).roundToInt().toLong(),
                order = remoteOrderData,
                tracking = trackingData
            )
            check(saved) { "Verified payment order could not be saved." }
        }

        val generatedId = storeDao.insertOrder(draftOrder)
        saveCustomerProfile(customerName.trim(), mobileNumber.trim(), deliveryAddress.trim())
        storeDao.clearCart()
        return draftOrder.copy(orderId = generatedId)
    }

    suspend fun reorderIntoCart(order: OrderEntity) {
        if (order.serializedItems.isBlank()) return
        val pairs = order.serializedItems.split(",")
        for (pair in pairs) {
            val parts = pair.split(":")
            if (parts.size >= 2) {
                val id = parts[0]
                val qty = parts[1].toIntOrNull() ?: 0
                val grams = parts.getOrNull(2)?.toIntOrNull() ?: 1000
                if (ProduceCatalog.findById(id) != null && qty > 0) {
                    val existing = storeDao.getCartItem(id)
                    storeDao.upsertCartItem(
                        CartItemEntity(
                            produceId = id,
                            quantity = (existing?.quantity ?: 0) + qty,
                            weightGrams = existing?.weightGrams ?: grams,
                            addedAtMillis = existing?.addedAtMillis ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
