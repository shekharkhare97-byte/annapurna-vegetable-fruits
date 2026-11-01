package com.example.data.repository

import com.example.data.local.CartItemEntity
import com.example.data.local.CustomerProfileEntity
import com.example.data.local.OrderEntity
import com.example.data.local.StoreDao
import com.example.data.model.ProduceCatalog
import com.example.data.model.ProduceItem
import com.example.data.model.isWeightBased
import com.example.data.model.formatCartQuantity
import com.example.util.MarathiFormatters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CartLineItem(
    val produce: ProduceItem,
    val quantity: Int
) {
    val lineTotalRupees: Int get() =
        if (produce.isWeightBased()) (produce.priceRupees * quantity + 500) / 1000
        else produce.priceRupees * quantity
}

class StoreRepository(private val storeDao: StoreDao) {

    val cartLineItems: Flow<List<CartLineItem>> = storeDao.observeCartItems().map { entities ->
        entities.mapNotNull { entity ->
            val produce = ProduceCatalog.findById(entity.produceId)
            if (produce != null && entity.quantity > 0) {
                CartLineItem(produce = produce, quantity = entity.quantity)
            } else {
                null
            }
        }
    }

    val customerProfile: Flow<CustomerProfileEntity?> = storeDao.observeCustomerProfile()

    val recentOrders: Flow<List<OrderEntity>> = storeDao.observeAllOrders()

    suspend fun incrementItem(produceId: String) {
        val current = storeDao.getCartItem(produceId)
        val produce = ProduceCatalog.findById(produceId)
        val step = if (produce?.isWeightBased() == true) 250 else 1
        val nextQty = (current?.quantity ?: 0) + step
        storeDao.upsertCartItem(
            CartItemEntity(
                produceId = produceId,
                quantity = nextQty,
                addedAtMillis = current?.addedAtMillis ?: System.currentTimeMillis()
            )
        )
    }

    suspend fun decrementItem(produceId: String) {
        val current = storeDao.getCartItem(produceId) ?: return
        val produce = ProduceCatalog.findById(produceId)
        val step = if (produce?.isWeightBased() == true) 250 else 1
        val nextQty = current.quantity - step
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
        items: List<CartLineItem>
    ): OrderEntity {
        val summaryLines = items.joinToString(separator = "\n") { line ->
            "• ${line.produce.marathiName} (${line.produce.formatCartQuantity(line.quantity)}) = ₹${MarathiFormatters.toMarathiNumber(line.lineTotalRupees)}"
        }
        val serialized = items.joinToString(separator = ",") { "${it.produce.id}:${it.quantity}" }
        val totalQty = items.size
        val grandTotal = items.sumOf { it.lineTotalRupees }
        val now = System.currentTimeMillis()

        val draftOrder = OrderEntity(
            customerName = customerName.trim(),
            mobileNumber = mobileNumber.trim(),
            deliveryAddress = deliveryAddress.trim(),
            itemsSummaryMarathi = summaryLines,
            serializedItems = serialized,
            totalItemsCount = totalQty,
            totalAmountRupees = grandTotal,
            createdAtMillis = now
        )
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
            if (parts.size == 2) {
                val id = parts[0]
                val storedQty = parts[1].toIntOrNull() ?: 0
                val produce = ProduceCatalog.findById(id)
                val qty = if (produce?.isWeightBased() == true && storedQty in 1..99) storedQty * 1000 else storedQty
                if (produce != null && qty > 0) {
                    val existing = storeDao.getCartItem(id)
                    storeDao.upsertCartItem(
                        CartItemEntity(
                            produceId = id,
                            quantity = (existing?.quantity ?: 0) + qty,
                            addedAtMillis = existing?.addedAtMillis ?: System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }
}
