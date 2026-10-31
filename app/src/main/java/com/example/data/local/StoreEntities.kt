package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val produceId: String,
    val quantity: Int,
    val addedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "customer_profile")
data class CustomerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val customerName: String = "",
    val mobileNumber: String = "",
    val deliveryAddress: String = ""
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val orderId: Long = 0L,
    val customerName: String,
    val mobileNumber: String,
    val deliveryAddress: String,
    val itemsSummaryMarathi: String,
    val serializedItems: String, // format: produceId:qty,produceId:qty
    val totalItemsCount: Int,
    val totalAmountRupees: Int,
    val createdAtMillis: Long = System.currentTimeMillis()
)
