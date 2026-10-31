package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnnapurnaDatabase
import com.example.data.model.ProduceCatalog
import com.example.data.repository.StoreRepository
import com.example.util.MarathiFormatters
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: AnnapurnaDatabase
    private lateinit var repository: StoreRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AnnapurnaDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = StoreRepository(database.storeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `apk build config contains zero embedded api keys`() {
        val fieldNames = BuildConfig::class.java.declaredFields.map { it.name.uppercase() }
        val suspiciousFields = fieldNames.filter {
            it.contains("API_KEY") || it.contains("SECRET") || it.contains("TOKEN")
        }
        assertFalse(
            "BuildConfig must not embed any API keys or secrets, found: $suspiciousFields",
            suspiciousFields.isNotEmpty()
        )
    }

    @Test
    fun `app name and catalog are properly configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Annapurna Vegetable and Fruits", context.getString(R.string.app_name))
        assertEquals(12, ProduceCatalog.items.size)
    }

    @Test
    fun `marathi digit conversion and mobile validation support devanagari digits`() {
        assertEquals("४८०", MarathiFormatters.toMarathiNumber(480))
        assertEquals("9876543210", MarathiFormatters.normalizePhoneDigits("९८७६५४३२१०"))
        assertTrue(MarathiFormatters.isValidIndianMobile("९८७६५४३२१०"))
        assertTrue(MarathiFormatters.isValidIndianMobile("9876543210"))
    }

    @Test
    fun `cart and order flow persists order and clears cart`() = runBlocking {
        repository.incrementItem("veg_tomato")
        repository.incrementItem("veg_tomato")
        repository.incrementItem("fruit_banana")

        val cartBefore = repository.cartLineItems.first()
        assertEquals(2, cartBefore.size)
        assertEquals(140, cartBefore.sumOf { it.lineTotalRupees })

        val order = repository.placeOrder(
            customerName = "राजेश कुलकर्णी",
            mobileNumber = "9876543210",
            deliveryAddress = "कोथरूड, पुणे",
            items = cartBefore
        )

        assertTrue(order.orderId > 0)
        assertEquals(140, order.totalAmountRupees)

        val cartAfter = repository.cartLineItems.first()
        assertTrue(cartAfter.isEmpty())

        val orders = repository.recentOrders.first()
        assertEquals(1, orders.size)
        assertEquals("राजेश कुलकर्णी", orders.first().customerName)
    }
}
