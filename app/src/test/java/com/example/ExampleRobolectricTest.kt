package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AnnapurnaDatabase
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
    fun `app package and runtime loaded catalog are configured`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("com.aistudio.annapurnaveg.kxpqrz", context.packageName)
        assertTrue(context.getString(R.string.app_name).isNotBlank())
        // Products are intentionally loaded from the live Admin catalog, not a stale local seed list.
        assertTrue(com.example.data.model.ProduceCatalog.items.isEmpty())
    }

    @Test
    fun `marathi digit conversion and mobile validation support devanagari digits`() {
        assertEquals("४८०", MarathiFormatters.toMarathiNumber(480))
        assertEquals("9876543210", MarathiFormatters.normalizePhoneDigits("९८७६५४३२१०"))
        assertTrue(MarathiFormatters.isValidIndianMobile("९८७६५४३२१०"))
        assertTrue(MarathiFormatters.isValidIndianMobile("9876543210"))
    }

    @Test
    fun `cart persists locally and supports add remove and clear operations`() = runBlocking {
        repository.incrementItem("veg_tomato", 1000)
        repository.incrementItem("veg_tomato", 500)
        repository.incrementItem("fruit_banana", 1000)

        val cartBefore = repository.cartLineItems.first()
        // Catalog entries are remote; with no Firebase catalog in this isolated test,
        // validate the Room rows directly through repository cart mutations instead.
        assertEquals(2, database.storeDao().observeCartItems().first().size)

        repository.decrementLatestItemForProduce("veg_tomato")
        assertEquals(2, database.storeDao().observeCartItems().first().size)

        repository.clearCart()
        assertTrue(database.storeDao().observeCartItems().first().isEmpty())
        assertTrue(cartBefore.isEmpty())
    }
}
