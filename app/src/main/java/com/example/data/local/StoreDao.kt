package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StoreDao {
    @Query("SELECT * FROM cart_items ORDER BY addedAtMillis ASC")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE cartLineId = :cartLineId LIMIT 1")
    suspend fun getCartItem(cartLineId: String): CartItemEntity?

    @Query("SELECT * FROM cart_items WHERE produceId = :produceId ORDER BY addedAtMillis DESC LIMIT 1")
    suspend fun getLatestCartItemForProduce(produceId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE cartLineId = :cartLineId")
    suspend fun deleteCartItem(cartLineId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    @Query("SELECT * FROM customer_profile WHERE id = 1 LIMIT 1")
    fun observeCustomerProfile(): Flow<CustomerProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomerProfile(profile: CustomerProfileEntity)

    @Query("SELECT * FROM orders ORDER BY createdAtMillis DESC")
    fun observeAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long
}
